/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.service.mybatis.export;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.*;
import org.gbif.dwc.DwcaStreamWriter;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.registry.metadata.EMLWriter;
import org.gbif.utils.file.CompressionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

@Transactional
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

  @Autowired
  public Exporter(@Qualifier("exportRepository") File repository,
                  DatasetService datasetService,
                  NameUsageMapper usageMapper, VernacularNameMapper vernacularMapper, DescriptionMapper descriptionMapper,
                  DistributionMapper distributionMapper, MultimediaMapper mediaMapper, ReferenceMapper referenceMapper,
                  TypeSpecimenMapper typeSpecimenMapper) {
    this.repository = repository;
    this.usageMapper = usageMapper;
    this.vernacularMapper = vernacularMapper;
    this.descriptionMapper = descriptionMapper;
    this.distributionMapper = distributionMapper;
    this.mediaMapper = mediaMapper;
    this.referenceMapper = referenceMapper;
    this.typeSpecimenMapper = typeSpecimenMapper;
    this.datasetService = datasetService;
  }

  public Exporter(File repository, ApplicationContext ctx, DatasetService datasetService) {
    this.repository = repository;
    usageMapper = ctx.getBean(NameUsageMapper.class);
    descriptionMapper = ctx.getBean(DescriptionMapper.class);
    distributionMapper = ctx.getBean(DistributionMapper.class);
    mediaMapper = ctx.getBean(MultimediaMapper.class);
    vernacularMapper = ctx.getBean(VernacularNameMapper.class);
    referenceMapper = ctx.getBean(ReferenceMapper.class);
    typeSpecimenMapper = ctx.getBean(TypeSpecimenMapper.class);
    this.datasetService = datasetService;
  }

  /**
   * Synchronously generates a new DwcA export file for a given dataset
   *
   * @return the newly generated DwcA file
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
    private final EMLWriter emlWriter = EMLWriter.newInstance();

    private final Dataset dataset;
    private final File dwca;
    private DwcaStreamWriter writer;
    private int counter;
    private int extCounter;

    public DwcaExport(Dataset dataset) {
      this.dataset = dataset;
      this.dwca = new File(repository, dataset.getKey().toString() + ".zip");
    }

    @Override
    public void run() {
      LOG.info("Start exporting checklist {} into DwC-A at {}", dataset.getKey(), dwca.getAbsolutePath());
      File tmp = Files.createTempDir();
      try {
        exportDataFiles(tmp);
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

    @Transactional
    public void exportDataFiles(File tmp) throws Exception {
        writer = new DwcaStreamWriter(tmp, DwcTerm.Taxon, DwcTerm.taxonID, true);

        // add EML
        StringWriter emlString = new StringWriter();
        emlWriter.writeTo(dataset, emlString);
        writer.setMetadata(emlString.toString(), "eml.xml");

        // write core taxa
        try (RowHandler.TaxonHandler coreHandler = new RowHandler.TaxonHandler(writer, dataset.getKey())) {
          usageMapper.processDataset(dataset.getKey()).forEach(coreHandler);
          counter = coreHandler.getCounter();
          LOG.info("Written {} core taxa", counter);

          // add constituents
          LOG.info("Adding {} constituents metadata", coreHandler.getConstituents().size());
          for (UUID dkey : coreHandler.getConstituents()) {
            Dataset constituent = datasetService.get(dkey);
            if (constituent != null) {
              StringWriter constituentEmlString = new StringWriter();
              emlWriter.writeTo(constituent, constituentEmlString);
              writer.addConstituent(constituent.getKey().toString(), constituentEmlString.toString());
            }
          }
        }

        // descriptions
        try (RowHandler.DescriptionHandler handler = new RowHandler.DescriptionHandler(writer)) {
          descriptionMapper.processDataset(dataset.getKey()).forEach(handler);
          LOG.info("Written {} description records", handler.getCounter());
          extCounter = handler.getCounter();
        }

        // distributions
        try (RowHandler.DistributionHandler handler = new RowHandler.DistributionHandler(writer)) {
          distributionMapper.processDataset(dataset.getKey()).forEach(handler);
          LOG.info("Written {} distribution records", handler.getCounter());
          extCounter = +handler.getCounter();
        }

        // media
        try (RowHandler.NameUsageMediaObjectHandler handler = new RowHandler.NameUsageMediaObjectHandler(writer)) {
          mediaMapper.processDataset(dataset.getKey()).forEach(handler);
          LOG.info("Written {} media records", handler.getCounter());
          extCounter = +handler.getCounter();
        }

        // references
        try (RowHandler.ReferenceHandler handler = new RowHandler.ReferenceHandler(writer)) {
          referenceMapper.processDataset(dataset.getKey()).forEach(handler);
          LOG.info("Written {} reference records", handler.getCounter());
          extCounter = +handler.getCounter();
        }

        // types
        try (RowHandler.TypeSpecimenHandler handler = new RowHandler.TypeSpecimenHandler(writer)) {
          typeSpecimenMapper.processDataset(dataset.getKey()).forEach(handler);
          LOG.info("Written {} typification records", handler.getCounter());
          extCounter = +handler.getCounter();
        }

        // vernacular names
        try (RowHandler.VernacularNameHandler handler = new RowHandler.VernacularNameHandler(writer)) {
          vernacularMapper.processDataset(dataset.getKey()).forEach(handler);
          LOG.info("Written {} vernacular name records", handler.getCounter());
          extCounter = +handler.getCounter();
        }


        // finish dwca
        writer.close();
    }
  }

}
