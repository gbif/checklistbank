package org.gbif.checklistbank.service.mybatis.export;

import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.config.RegistryServiceConfiguration;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.mapper.*;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwca.io.DwcaStreamWriter;
import org.gbif.utils.file.CompressionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Exporter {

  private static final Logger LOG = LoggerFactory.getLogger(Exporter.class);
  private final File repository;
  private final NameUsageMapper usageMapper;
  private final VernacularNameMapper vernacularMapper;
  private final DescriptionMapper descriptionMapper;
  private final DistributionMapper distributionMapper;
  private final MultimediaMapper mediaMapper;
  private final ReferenceMapper referenceMapper;
  private final TypeSpecimenMapper typeSpecimenMapper;
  private final DatasetService datasetService;

  private Exporter(File repository, ClbConfiguration cfg, DatasetService datasetService) {
    this.repository = repository;
    // init postgres mappers
    Injector inj = Guice.createInjector(InternalChecklistBankServiceMyBatisModule.create(cfg));
    usageMapper = inj.getInstance(NameUsageMapper.class);
    descriptionMapper = inj.getInstance(DescriptionMapper.class);
    distributionMapper = inj.getInstance(DistributionMapper.class);
    mediaMapper = inj.getInstance(MultimediaMapper.class);
    vernacularMapper = inj.getInstance(VernacularNameMapper.class);
    referenceMapper = inj.getInstance(ReferenceMapper.class);
    typeSpecimenMapper = inj.getInstance(TypeSpecimenMapper.class);

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
        try (RowHandler.TaxonHandler coreHandler = new RowHandler.TaxonHandler(writer, dataset.getKey())) {
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

        // descriptions
        try (RowHandler.DescriptionHandler handler = new RowHandler.DescriptionHandler(writer)) {
          descriptionMapper.processDataset(dataset.getKey(), handler);
          LOG.info("Written {} description records", handler.getCounter());
          extCounter = handler.getCounter();
        }

        // distributions
        try (RowHandler.DistributionHandler handler = new RowHandler.DistributionHandler(writer)) {
          distributionMapper.processDataset(dataset.getKey(), handler);
          LOG.info("Written {} distribution records", handler.getCounter());
          extCounter = +handler.getCounter();
        }

        // media
        try (RowHandler.NameUsageMediaObjectHandler handler = new RowHandler.NameUsageMediaObjectHandler(writer)) {
          mediaMapper.processDataset(dataset.getKey(), handler);
          LOG.info("Written {} media records", handler.getCounter());
          extCounter = +handler.getCounter();
        }

        // references
        try (RowHandler.ReferenceHandler handler = new RowHandler.ReferenceHandler(writer)) {
          referenceMapper.processDataset(dataset.getKey(), handler);
          LOG.info("Written {} reference records", handler.getCounter());
          extCounter = +handler.getCounter();
        }

        // types
        try (RowHandler.TypeSpecimenHandler handler = new RowHandler.TypeSpecimenHandler(writer)) {
          typeSpecimenMapper.processDataset(dataset.getKey(), handler);
          LOG.info("Written {} typification records", handler.getCounter());
          extCounter = +handler.getCounter();
        }

        // vernacular names
        try (RowHandler.VernacularNameHandler handler = new RowHandler.VernacularNameHandler(writer)) {
          vernacularMapper.processDataset(dataset.getKey(), handler);
          LOG.info("Written {} vernacular name records", handler.getCounter());
          extCounter = +handler.getCounter();
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

}
