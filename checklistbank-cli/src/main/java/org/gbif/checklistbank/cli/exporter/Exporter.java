package org.gbif.checklistbank.cli.exporter;

import com.google.common.collect.ImmutableList;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.*;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.checklistbank.cli.common.RegistryServiceConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.ParsedNameUsage;
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
import org.gbif.dwca.io.DwcaStreamWriter;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

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
import org.neo4j.unsafe.impl.batchimport.cache.NumberArrayFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exporter {

  private static final Logger LOG = LoggerFactory.getLogger(Exporter.class);
  private static final Joiner CONCAT = Joiner.on(";").skipNulls();
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
   *
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

  private class DwcaExport implements Runnable {
    private final Dataset dataset;
    private final File dwca;
    private DwcaStreamWriter writer;
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
        writer = new DwcaStreamWriter(tmp, DwcTerm.Taxon, DwcTerm.taxonID, true);

        // add EML
        writer.setMetadata(dataset);

        // write core taxa
        try (TaxonHandler coreHandler = new TaxonHandler(writer, dataset.getKey())) {
          usageMapper.processDataset(dataset.getKey(), coreHandler);
          counter = coreHandler.getCounter();
          LOG.info("Written {} core taxa", counter);

          // add constituents
          LOG.info("Adding {} constituents metadata", coreHandler.getConstituents().size());
          for (UUID dkey : coreHandler.getConstituents()) {
            Dataset constituent = datasetService.get(dkey);
            if (constituent != null) {
              writer.addConstituent(constituent);
            }
          }
        }

        // distributions
        try (DistributionHandler dHandler = new DistributionHandler(writer)) {
          distributionMapper.processDataset(dataset.getKey(), dHandler);
          LOG.info("Written {} distribution records", dHandler.getCounter());
          extCounter = dHandler.getCounter();
        }

        // media
        try (NameUsageMediaObjectHandler mHandler = new NameUsageMediaObjectHandler(writer)) {
          mediaMapper.processDataset(dataset.getKey(), mHandler);
          LOG.info("Written {} media records", mHandler.getCounter());
          extCounter = mHandler.getCounter();
        }

        // references
        try (ReferenceHandler rHandler = new ReferenceHandler(writer)) {
          referenceMapper.processDataset(dataset.getKey(), rHandler);
          LOG.info("Written {} reference records", rHandler.getCounter());
          extCounter = rHandler.getCounter();
        }

        // vernacular names
        try (VernacularNameHandler vHandler = new VernacularNameHandler(writer)) {
          vernacularMapper.processDataset(dataset.getKey(), vHandler);
          LOG.info("Written {} vernacular name records", vHandler.getCounter());
          extCounter = vHandler.getCounter();
        }

        // finish dwca
        writer.close();
        // zip it up to final location
        FileUtils.forceMkdir(dwca.getParentFile());
        CompressionUtil.zipDir(tmp, dwca, true);

        LOG.info("Done exporting checklist {} with {} usages and {} extensions into DwC-A at {}", dataset.getKey(), counter, extCounter, dwca.getAbsolutePath());

      } catch (Exception e) {
        LOG.error("Failed to create dwca for dataset {} at {}", dataset.getKey(), tmp.getAbsolutePath(), e);

      } finally {
        try {
          FileUtils.deleteDirectory(tmp);
        } catch (IOException e) {
          LOG.error("Failed to remove tmp dwca dir {}", tmp.getAbsolutePath(), e);
        }
      }
    }

  }

  private static abstract class RowHandler<T> implements ResultHandler<T>, AutoCloseable {
    private final DwcaStreamWriter.RowWriteHandler writer;
    private int counter;
    private final Term rowType;

    private RowHandler(DwcaStreamWriter writer, Term rowType, List<Term> columns) {
      int idx = 1;
      Map<Term, Integer> mapping = Maps.newHashMap();
      for (Term term : columns) {
        mapping.put(term, idx++);
      }
      this.writer = writer.writeHandler(rowType, 0, mapping);
      this.rowType = rowType;
    }

    abstract String[] toRow(T obj);

    @Override
    public void handleResult(ResultContext<? extends T> ctx) {
      writer.write(toRow(ctx.getResultObject()));
      if (counter++ % 10000 == 0) {
        LOG.info("{} {} records added to dwca", counter, rowType.simpleName());
      }
    }

    public int getCounter() {
      return counter;
    }

    @Override
    public void close() throws Exception {
      writer.close();
    }
  }

  private static class TaxonHandler extends RowHandler<ParsedNameUsage> {

    static final List<Term> columns = ImmutableList.of(
            DwcTerm.datasetID,
            DwcTerm.parentNameUsageID,
            DwcTerm.acceptedNameUsageID,
            DwcTerm.originalNameUsageID,
            DwcTerm.scientificName,
            DwcTerm.scientificNameAuthorship,
            GbifTerm.canonicalName,
            GbifTerm.genericName,
            DwcTerm.specificEpithet,
            DwcTerm.infraspecificEpithet,
            DwcTerm.taxonRank,
            DwcTerm.nameAccordingTo,
            DwcTerm.namePublishedIn,
            DwcTerm.taxonomicStatus,
            DwcTerm.nomenclaturalStatus,
            DwcTerm.taxonRemarks,
            DwcTerm.kingdom,
            DwcTerm.phylum,
            DwcTerm.class_,
            DwcTerm.order,
            DwcTerm.family,
            DwcTerm.genus
    );
    private final Set<UUID> constituents = Sets.newHashSet();
    private final UUID datasetKey;

    private TaxonHandler(DwcaStreamWriter writer, UUID datasetKey) {
      super(writer, DwcTerm.Taxon, columns);
      this.datasetKey = datasetKey;
    }

    public Set<UUID> getConstituents() {
      return constituents;
    }

    @Override
    String[] toRow(ParsedNameUsage u) {
      String[] row = new String[columns.size()+1];

      final ParsedName pn = u.getParsedName();

      int idx = 0;
      row[idx++] = toStr(u.getKey());
      row[idx++] = toStr(u.getConstituentKey());
      if (u.getConstituentKey() != null && !u.getConstituentKey().equals(datasetKey)) {
        constituents.add(u.getConstituentKey());
      }
      row[idx++] = toStr(u.getParentKey());
      row[idx++] = toStr(u.getAcceptedKey());
      row[idx++] = toStr(u.getBasionymKey());
      // name
      row[idx++] = u.getScientificName();
      row[idx++] = u.getAuthorship();
      row[idx++] = u.getCanonicalName();
      row[idx++] = pn.getGenusOrAbove();
      row[idx++] = pn.getSpecificEpithet();
      row[idx++] = pn.getInfraSpecificEpithet();
      // taxon
      row[idx++] = toStr(u.getRank());
      row[idx++] = u.getAccordingTo();
      row[idx++] = u.getPublishedIn();
      row[idx++] = toStr(u.getTaxonomicStatus());
      row[idx++] = toStr(u.getNomenclaturalStatus());
      row[idx++] = u.getRemarks();
      // classification
      row[idx++] = u.getKingdom();
      row[idx++] = u.getPhylum();
      row[idx++] = u.getClazz();
      row[idx++] = u.getOrder();
      row[idx++] = u.getFamily();
      row[idx] = u.getGenus();

      return row;
    }
  }

  private static class DistributionHandler extends RowHandler<Distribution> {

    static final List<Term> columns = ImmutableList.of(
            DwcTerm.locationID,
            DwcTerm.locality,
            DwcTerm.country,
            DwcTerm.countryCode,
            DwcTerm.locationRemarks,
            DwcTerm.establishmentMeans,
            DwcTerm.lifeStage,
            DwcTerm.occurrenceStatus,
            IucnTerm.threatStatus,
            DcTerm.source
    );

    private DistributionHandler(DwcaStreamWriter writer) {
      super(writer, GbifTerm.Distribution, columns);
    }

    @Override
    String[] toRow(Distribution d) {
      int idx = 0;
      String[] row = new String[columns.size()+1];

      row[idx++] = toStr(d.getTaxonKey());
      row[idx++] = d.getLocationId();
      row[idx++] = d.getLocality();
      addCountryColumns(row, idx, d.getCountry());
      idx = idx + 2;
      row[idx++] = d.getRemarks();
      row[idx++] = toStr(d.getEstablishmentMeans());
      row[idx++] = toStr(d.getLifeStage());
      row[idx++] = toStr(d.getStatus());
      row[idx++] = toStr(d.getThreatStatus());
      row[idx] = d.getSource();

      return row;
    }
  }

  private static class NameUsageMediaObjectHandler extends RowHandler<NameUsageMediaObject> {

    static final List<Term> columns = ImmutableList.of(
            DcTerm.identifier,
            DcTerm.references,
            DcTerm.title,
            DcTerm.description,
            DcTerm.license,
            DcTerm.creator,
            DcTerm.created,
            DcTerm.contributor,
            DcTerm.publisher,
            DcTerm.rightsHolder,
            DcTerm.source
    );

    private NameUsageMediaObjectHandler(DwcaStreamWriter writer) {
      super(writer, GbifTerm.Multimedia, columns);
    }

    @Override
    String[] toRow(NameUsageMediaObject m) {
      int idx = 0;
      String[] row = new String[columns.size()+1];

      row[idx++] = toStr(m.getTaxonKey());
      row[idx++] = toStr(m.getIdentifier());
      row[idx++] = toStr(m.getReferences());
      row[idx++] = m.getTitle();
      row[idx++] = m.getDescription();
      row[idx++] = m.getLicense();
      row[idx++] = m.getCreator();
      row[idx++] = toStr(m.getCreated());
      row[idx++] = m.getContributor();
      row[idx++] = m.getPublisher();
      row[idx++] = m.getRightsHolder();
      row[idx] = m.getSource();

      return row;
    }
  }

  private static class ReferenceHandler extends RowHandler<Reference> {

    static final List<Term> columns = ImmutableList.of(
            DcTerm.bibliographicCitation,
            DcTerm.identifier,
            DcTerm.references,
            DcTerm.source
    );

    private ReferenceHandler(DwcaStreamWriter writer) {
      super(writer, GbifTerm.Reference, columns);
    }

    @Override
    String[] toRow(Reference r) {
      int idx = 0;
      String[] row = new String[columns.size()+1];

      row[idx++] = toStr(r.getTaxonKey());
      row[idx++] = r.getCitation();
      row[idx++] = r.getDoi();
      row[idx++] = r.getLink();
      row[idx] = r.getSource();

      return row;
    }
  }

  private static class VernacularNameHandler extends RowHandler<VernacularName> {

    static final List<Term> columns = ImmutableList.of(
            DwcTerm.vernacularName,
            DcTerm.language,
            DwcTerm.country,
            DwcTerm.countryCode,
            DwcTerm.sex,
            DwcTerm.lifeStage,
            DcTerm.source
    );

    private VernacularNameHandler(DwcaStreamWriter writer) {
      super(writer, GbifTerm.VernacularName, columns);
    }

    @Override
    String[] toRow(VernacularName v) {
      int idx = 0;
      String[] row = new String[columns.size()+1];

      row[idx++] = toStr(v.getTaxonKey());
      row[idx++] = v.getVernacularName();
      row[idx++] = toStr(v.getLanguage());
      addCountryColumns(row, idx, v.getCountry());
      idx=idx+2;
      row[idx++] = toStr(v.getSex());
      row[idx++] = toStr(v.getLifeStage());
      row[idx] = v.getSource();

      return row;
    }
  }

  private static String toStr(Collection<? extends Enum> es) {
    if (es == null) return "";
    return CONCAT.join(es).toLowerCase().replaceAll("_", " ");
  }

  private static String toStr(Language l) {
    if (l == null) return null;
    return l.getIso2LetterCode();
  }

  private static String toStr(Enum e) {
    if (e == null) return null;
    return e.name().toLowerCase().replaceAll("_", " ");
  }

  private static String toStr(Date date) {
    if (date == null) return null;
    return DateFormatUtils.ISO_DATE_FORMAT.format(date);
  }

  private static String toStr(Object obj) {
    return obj == null ? null : obj.toString();
  }

  private static void addCountryColumns(String[] row, int idx, Country val) {
    if (val != null) {
      row[idx++] = val.getTitle();
      row[idx] = val.getIso2LetterCode();
    } else {
      row[idx++] = null;
      row[idx] = null;
    }
  }
}
