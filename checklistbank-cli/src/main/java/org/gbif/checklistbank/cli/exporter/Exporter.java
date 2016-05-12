package org.gbif.checklistbank.cli.exporter;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.checklistbank.cli.common.RegistryServiceConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.mapper.DistributionMapper;
import org.gbif.checklistbank.service.mybatis.mapper.MultimediaMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageComponentMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.ReferenceMapper;
import org.gbif.checklistbank.service.mybatis.mapper.VernacularNameMapper;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.IucnTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.DwcaWriter;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exporter {

  private static final Logger LOG = LoggerFactory.getLogger(Exporter.class);
  private static final Joiner CONCAT = Joiner.on(";").skipNulls();
  private static final PagingRequest EXT_PAGE = new PagingRequest(0, 500);
  private final File repository;
  private final NameUsageMapper usageMapper;
  private final VernacularNameMapper vernacularMapper;
  private final DistributionMapper distributionMapper;
  private final MultimediaMapper mediaMapper;
  private final ReferenceMapper referenceMapper;
  private final DatasetService datasetService;

  private Exporter(File repository, ClbConfiguration cfg, DatasetService datasetService) {
    this.repository = repository;
    // init postgres mappers
    Injector inj = Guice.createInjector(InternalChecklistBankServiceMyBatisModule.create(cfg));
    usageMapper = inj.getInstance(NameUsageMapper.class);
    vernacularMapper = inj.getInstance(VernacularNameMapper.class);
    distributionMapper = inj.getInstance(DistributionMapper.class);
    mediaMapper = inj.getInstance(MultimediaMapper.class);
    referenceMapper = inj.getInstance(ReferenceMapper.class);

    this.datasetService = datasetService;
  }

  /**
   * @param registryWs base URL of the registry API, e.g. http://api.gbif.org/v1
   */
  public static Exporter create(File repository, ClbConfiguration cfg, String registryWs) {
    RegistryServiceConfiguration regCfg = new RegistryServiceConfiguration();
    regCfg.wsUrl = registryWs;
    Injector inj = regCfg.createRegistryInjector();
    return new Exporter(repository, cfg, inj.getInstance(DatasetService.class));
  }

  /**
   * Synchroneously generates a new dwca export file for a given dataset
   * @return the newly generated dwca file
   */
  public File export(Dataset dataset) {
    DwcaExport exporter = new DwcaExport(dataset);
    exporter.run();
    return exporter.dwca;
  }

  public File export(UUID datasetKey) {
    return export(datasetService.get(datasetKey));
  }

  private class DwcaExport implements Runnable, ResultHandler<NameUsage> {
    private final Dataset dataset;
    private final File dwca;
    private DwcaWriter writer;
    private Set<UUID> constituents = Sets.newHashSet();
    private int counter;
    private int extCounter;

    public DwcaExport(Dataset dataset) {
      this.dataset = dataset;
      this.dwca = new File(repository, dataset.getKey().toString() + ".zip");
    }

    public void run() {
      LOG.info("Start exporting checklist {} into DwC-A at {}", dataset.getKey(), dwca.getAbsolutePath());
      File tmp = Files.createTempDir();
      try {
        FileUtils.forceMkdir(dwca.getParentFile());
        writer = new DwcaWriter(DwcTerm.Taxon, tmp);
        usageMapper.processDataset(dataset.getKey(), this);

        // add EML
        writer.setEml(dataset);

        // add constituents
        for (UUID dkey : constituents) {
          Dataset constituent = datasetService.get(dkey);
          if (constituent != null) {
            writer.addConstituent(constituent);
          }
        }
        try {
          // finish dwca
          writer.close();
          // zip it up to final location
          CompressionUtil.zipDir(tmp, dwca, true);

        } catch (IOException e) {
          LOG.error("Failed to bundle dwca at {}", tmp.getAbsolutePath(), e);
          throw e;
        }

      } catch (IOException e) {
        LOG.error("Failed to create dwca for dataset {} at {}", dataset.getKey(), tmp.getAbsolutePath(), e);

      } finally {
        try {
          FileUtils.deleteDirectory(tmp);
        } catch (IOException e) {
          LOG.error("Failed to remove tmp dwca dir {}", tmp.getAbsolutePath(), e);
        }
      }
      LOG.info("Done exporting checklist {} with {} usages and {} extensions into DwC-A at {}", dataset.getKey(), counter, extCounter, dwca.getAbsolutePath());
    }


    @Override
    public void handleResult(ResultContext<? extends NameUsage> obj) {
      final NameUsage u = obj.getResultObject();
      try {
        writer.newRecord(u.getKey().toString());
        writer.addCoreColumn(DwcTerm.taxonID, u.getKey());
        writer.addCoreColumn(DwcTerm.datasetID, u.getConstituentKey());
        if (u.getConstituentKey() != null && !u.getConstituentKey().equals(dataset.getKey())) {
          constituents.add(u.getConstituentKey());
        }
        writer.addCoreColumn(DwcTerm.parentNameUsageID, u.getParentKey());
        writer.addCoreColumn(DwcTerm.acceptedNameUsageID, u.getAcceptedKey());
        writer.addCoreColumn(DwcTerm.originalNameUsageID, u.getBasionymKey());
        writer.addCoreColumn(DwcTerm.scientificName, u.getScientificName());
        writer.addCoreColumn(DwcTerm.taxonRank, u.getRank());
        writer.addCoreColumn(DwcTerm.nameAccordingTo, u.getAccordingTo());
        writer.addCoreColumn(DwcTerm.namePublishedIn, u.getPublishedIn());
        writer.addCoreColumn(DwcTerm.taxonomicStatus, u.getTaxonomicStatus());
        writer.addCoreColumn(DwcTerm.nomenclaturalStatus, enum2Str(u.getNomenclaturalStatus()));
        writer.addCoreColumn(DwcTerm.kingdom, u.getKingdom());
        writer.addCoreColumn(DwcTerm.phylum, u.getPhylum());
        writer.addCoreColumn(DwcTerm.class_, u.getClazz());
        writer.addCoreColumn(DwcTerm.order, u.getOrder());
        writer.addCoreColumn(DwcTerm.family, u.getFamily());
        writer.addCoreColumn(DwcTerm.genus, u.getGenus());
        writer.addCoreColumn(DwcTerm.taxonRemarks, u.getRemarks());
        addExtensionData(u);
        if (counter++ % 10000 == 0) {
          LOG.info("{} usages with {} extension added to dwca", counter, extCounter);
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    private void addExtensionData(NameUsage u) throws IOException {
      // distributions
      for (Distribution v : listExtensions(distributionMapper, u.getKey())) {
        writer.addExtensionRecord(GbifTerm.Distribution, map(v));
        extCounter++;
      }
      // media
      for (NameUsageMediaObject v : listExtensions(mediaMapper, u.getKey())) {
        writer.addExtensionRecord(GbifTerm.Multimedia, map(v));
        extCounter++;
      }
      // references
      for (Reference v : listExtensions(referenceMapper, u.getKey())) {
        writer.addExtensionRecord(GbifTerm.Reference, map(v));
        extCounter++;
      }
      // vernacular names
      for (VernacularName v : listExtensions(vernacularMapper, u.getKey())) {
        writer.addExtensionRecord(GbifTerm.VernacularName, map(v));
        extCounter++;
      }
      // TODO: types
    }

    private Map<Term, String> map(Distribution d) {
      Map<Term, String> data = Maps.newHashMap();
      data.put(DwcTerm.locationID, d.getLocationId());
      data.put(DwcTerm.locality, d.getLocality());
      add(data, d.getCountry());
      data.put(DwcTerm.locationRemarks, d.getRemarks());
      data.put(DwcTerm.establishmentMeans, enum2Str(d.getEstablishmentMeans()));
      data.put(DwcTerm.lifeStage, enum2Str(d.getLifeStage()));
      data.put(DwcTerm.occurrenceStatus, enum2Str(d.getStatus()));
      data.put(IucnTerm.threatStatus, enum2Str(d.getThreatStatus()));
      data.put(DcTerm.source, d.getSource());
      return data;
    }

    private Map<Term, String> map(NameUsageMediaObject m) {
      Map<Term, String> data = Maps.newHashMap();
      data.put(DcTerm.identifier, uri2Str(m.getIdentifier()));
      data.put(DcTerm.references, uri2Str(m.getReferences()));
      data.put(DcTerm.title, m.getTitle());
      data.put(DcTerm.description, m.getDescription());
      data.put(DcTerm.license, m.getLicense());
      data.put(DcTerm.creator, m.getCreator());
      data.put(DcTerm.created, date2Str(m.getCreated()));
      data.put(DcTerm.contributor, m.getContributor());
      data.put(DcTerm.publisher, m.getPublisher());
      data.put(DcTerm.rightsHolder, m.getRightsHolder());
      data.put(DcTerm.source, m.getSource());
      return data;
    }

    private Map<Term, String> map(Reference r) {
      Map<Term, String> data = Maps.newHashMap();
      data.put(DcTerm.bibliographicCitation, r.getCitation());
      data.put(DcTerm.identifier, r.getDoi());
      data.put(DcTerm.references, r.getLink());
      data.put(DcTerm.source, r.getSource());
      return data;
    }

    private Map<Term, String> map(VernacularName v) {
      Map<Term, String> data = Maps.newHashMap();
      data.put(DwcTerm.vernacularName, v.getVernacularName());
      data.put(DcTerm.language, enum2Str(v.getLanguage()));
      add(data, v.getCountry());
      data.put(DwcTerm.sex, enum2Str(v.getSex()));
      data.put(DwcTerm.lifeStage, enum2Str(v.getLifeStage()));
      data.put(DcTerm.source, v.getSource());
      return data;
    }

    private <T> List<T> listExtensions(NameUsageComponentMapper<T> mapper, int usageKey) {
      if (Constants.NUB_DATASET_KEY.equals(dataset.getKey())) {
        return mapper.listByNubUsage(usageKey, EXT_PAGE);
      } else {
        return mapper.listByChecklistUsage(usageKey, EXT_PAGE);
      }
    }

    private String enum2Str(Collection<? extends Enum> es) {
      if (es == null) return "";
      return CONCAT.join(es).toLowerCase().replaceAll("_", " ");
    }

    private String enum2Str(Language l) {
      if (l == null) return null;
      return l.getIso2LetterCode();
    }

    private String enum2Str(Enum e) {
      if (e == null) return null;
      return e.name().toLowerCase().replaceAll("_", " ");
    }

    private String uri2Str(URI uri) {
      if (uri == null) return null;
      return uri.toString();
    }

    private String date2Str(Date date) {
      if (date == null) return null;
      return DateFormatUtils.ISO_DATE_FORMAT.format(date);
    }

    private void add(Map<Term, String> data, Country val) {
      if (val != null) {
        data.put(DwcTerm.countryCode, val.getIso2LetterCode());
        data.put(DwcTerm.country, val.getTitle());
      }
    }
  }

}
