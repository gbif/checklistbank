package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.checklistbank.index.model.NameUsageAvro;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.VernacularNameServiceMyBatis;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.base.Throwables;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executable job that creates a list of {@link org.apache.solr.common.SolrInputDocument} using a list of {@link org.gbif.api.model.checklistbank.NameUsage} objects.
 */
public class NameUsageAvroExportJob implements Callable<Integer> {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Minimum usage key, inclusive, to process.
   */
  private final int startKey;

  /**
   * Maximum usage key, inclusive, to process.
   */
  private final int endKey;

  private String nameNode;

  private String targetHdfsDir;

  /**
   * Service layer.
   */
  private final UsageService nameUsageService;
  private final VernacularNameServiceMyBatis vernacularNameService;
  private final DescriptionServiceMyBatis descriptionService;
  private final DistributionServiceMyBatis distributionService;
  private final SpeciesProfileServiceMyBatis speciesProfileService;

  private StopWatch stopWatch = new StopWatch();

  /**
   * {@link org.gbif.api.model.checklistbank.NameUsage}/{@link org.apache.solr.common.SolrInputDocument} converter.
   */
  private final NameUsageAvroConverter nameUsageAvroConverter;

  /**
   * Default constructor.
   */
  public NameUsageAvroExportJob(
    final UsageService nameUsageService,
    final int startKey,
    final int endKey,
    final VernacularNameServiceMyBatis vernacularNameService,
    final DescriptionServiceMyBatis descriptionService,
    final DistributionServiceMyBatis distributionService,
    final SpeciesProfileServiceMyBatis speciesProfileService,
    String nameNode,
    String targetHdfsDir
  ) {
    this.nameUsageService = nameUsageService;
    this.vernacularNameService = vernacularNameService;
    this.descriptionService = descriptionService;
    this.distributionService = distributionService;
    this.speciesProfileService = speciesProfileService;
    this.startKey = startKey;
    this.endKey = endKey;
    nameUsageAvroConverter = new NameUsageAvroConverter();
    this.targetHdfsDir = targetHdfsDir;
    this.nameNode= nameNode;

  }

  /**
   * Iterates over the assigned {@link org.gbif.api.model.checklistbank.NameUsage} objects to insert the corresponding {@link org.apache.solr.common.SolrInputDocument}
   * objects.
   *
   * @return the total number of documents added by this Thread.
   */
  @Override
  public Integer call() throws Exception {
    // Timing information initialization
    stopWatch.start();
    log.info("Adding usages from id {} to {}", startKey, endKey);
    int docCount = 0;
    // Get all usages
    List<NameUsage> usages = nameUsageService.listRange(startKey, endKey);
    // get all component maps into memory first
    Map<Integer, List<VernacularName>> vernacularNameMap = vernacularNameService.listRange(startKey, endKey);

    Map<Integer, List<Description>> descriptionMap = descriptionService.listRange(startKey, endKey);

    Map<Integer, List<Distribution>> distributionMap = distributionService.listRange(startKey, endKey);

    Map<Integer, List<SpeciesProfile>> speciesProfileMap = speciesProfileService.listRange(startKey, endKey);
    File file = new File(startKey+ "-" + endKey + ".avro");
    file.createNewFile();
    log.info("Creating file " + file.getAbsolutePath());
    ClassLoader classLoader = AvroTest.class.getClassLoader();
    Schema schema = new Schema.Parser().parse(classLoader.getResource("solr.avrsc").openStream());
    DatumWriter<NameUsageAvro> datumWriter = new SpecificDatumWriter<>(NameUsageAvro.class);
    try(DataFileWriter<NameUsageAvro> dataFileWriter = new DataFileWriter<NameUsageAvro>(datumWriter)) {
      dataFileWriter.create(schema, file);

      // now we're ready to build the solr indices quicky!
      for (NameUsage usage : usages) {
        if (usage == null) {
          log.warn("Unexpected numm usage found in range {}-{}, docCount={}", startKey, endKey, docCount);
          continue;
        }
        try {
          UsageExtensions ext = new UsageExtensions();
          ext.speciesProfiles = speciesProfileMap.get(usage.getKey());
          ext.vernacularNames = vernacularNameMap.get(usage.getKey());
          ext.descriptions = descriptionMap.get(usage.getKey());
          ext.distributions = distributionMap.get(usage.getKey());

          List<Integer> parents = nameUsageService.listParents(usage.getKey());
          dataFileWriter.append(nameUsageAvroConverter.toObject(usage, parents, ext));

        } catch (Exception e) {
          log.error("Error indexing document for usage {}", usage.getKey(), e);
        }
        docCount++;
        NameUsageIndexer.counter.incrementAndGet();
      }
    }

    moveToHdfs(file,nameNode);
    log.info(file.getName() + " moved to hdfs");
    // job finished notice
    stopWatch.stop();
    log.info("Finished indexing of usages in range {}-{}. Total time: {}",
      new Object[] {startKey, endKey, stopWatch.toString()});

    return docCount;
  }


  private boolean moveToHdfs(File file, String nameNode) throws IOException {
    try {
      Configuration configuration = new Configuration();
      configuration.set(FileSystem.FS_DEFAULT_NAME_KEY, nameNode);
      Path targetPath = new Path(targetHdfsDir, file.getName());
      log.info("Moving file {} to HDFS path ", file, targetPath);
      return FileUtil.copy(file, FileSystem.get(configuration), targetPath, true, configuration);
    } catch (IOException ioe) {
      log.error("Error moving file to HDFS",ioe);
      throw Throwables.propagate(ioe);
    }
  }

}
