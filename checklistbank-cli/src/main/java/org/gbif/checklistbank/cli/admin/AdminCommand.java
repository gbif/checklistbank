package org.gbif.checklistbank.cli.admin;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.gbif.api.model.Constants;
import org.gbif.api.model.crawler.DwcaValidationReport;
import org.gbif.api.model.crawler.GenericValidationReport;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.service.registry.*;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.checklistbank.cli.nubchanged.BackboneDatasetUpdater;
import org.gbif.checklistbank.cli.registry.RegistryService;
import org.gbif.checklistbank.model.DatasetCore;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.NubDb;
import org.gbif.checklistbank.nub.source.ClbSource;
import org.gbif.checklistbank.nub.validation.NubAssertions;
import org.gbif.checklistbank.nub.validation.NubTreeValidation;
import org.gbif.checklistbank.nub.validation.NubValidation;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.NameUsageServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.ParsedNameServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.export.Exporter;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.liquibase.DbSchemaUpdater;
import org.gbif.checklistbank.service.mybatis.mapper.DatasetMapper;
import org.gbif.checklistbank.service.mybatis.tmp.NameUsageReparser;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.*;
import org.kohsuke.MetaInfServices;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

/**
 * Command that issues new normalize or import messages for manual admin purposes.
 */
@MetaInfServices(Command.class)
public class AdminCommand extends BaseCommand {
  private static final Logger LOG = LoggerFactory.getLogger(AdminCommand.class);
  private static final String DWCA_SUFFIX = ".dwca";
  private final AdminConfiguration cfg = new AdminConfiguration();
  private MessagePublisher publisher;
  private ZookeeperUtils zkUtils;
  private DatasetService datasetService;
  private OrganizationService organizationService;
  private InstallationService installationService;
  private NetworkService networkService;
  private NodeService nodeService;
  private Iterable<Dataset> datasets;
  private Exporter exporter;

  public AdminCommand() {
    super("admin");
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

  private void initRegistry() {
    Injector inj = cfg.registry.createRegistryInjector();
    datasetService = inj.getInstance(DatasetService.class);
    organizationService = inj.getInstance(OrganizationService.class);
    installationService = inj.getInstance(InstallationService.class);
    networkService = inj.getInstance(NetworkService.class);
    nodeService = inj.getInstance(NodeService.class);
  }

  private void initCfg() {
    setKnownKey("col", Constants.COL_DATASET_KEY);
    setKnownKey("nub", Constants.NUB_DATASET_KEY);
    setKnownKey("plazi", UUID.fromString("7ce8aef0-9e92-11dc-8738-b8a03c50a862"));
    setKnownKey("iucn", UUID.fromString("19491596-35ae-4a91-9a98-85cf505f1bd3"));
  }

  private void setKnownKey(String name, UUID key) {
    try {
      Field field = cfg.getClass().getDeclaredField(name);
      if (field.getBoolean(cfg)) {
        if (cfg.key != null) {
          LOG.warn("Explicit dataset key given, ignore {} flag", name);
        } else {
          cfg.key = key;
        }
      }
    } catch (ReflectiveOperationException e) {
      e.printStackTrace();
      Throwables.propagate(e);
    }
  }

  private ZookeeperUtils zk() {
    if (zkUtils == null && cfg.zookeeper.isConfigured()) {
      try {
        zkUtils = new ZookeeperUtils(cfg.zookeeper.getCuratorFramework());
      } catch (IOException e) {
        Throwables.propagate(e);
      }
    }
    return zkUtils;
  }

  private void send(Message msg) throws IOException {
    if (publisher == null) {
      publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());
    }
    publisher.send(msg);
  }

  @Override
  protected void doRun() {
    initCfg();
    try {
      if (cfg.operation.global) {
        runGlobalCommands();
      } else {
        initRegistry();
        runDatasetComamnds();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runGlobalCommands() throws Exception {
    switch (cfg.operation) {
      case REPARSE:
        reparseNames();
        break;

      case CLEAN_ORPHANS:
        cleanOrphans();
        break;

      case SYNC_DATASETS:
        initRegistry();
        syncDatasets();
        break;

      case DUMP:
        dumpToNeo();
        break;

      case VALIDATE_NEO:
        verifyNeo();
        break;
  
      case VALIDATE_BACKBONE:
        verifyPg();
        break;
      
      case NUB_CHANGED:
        sendNubChanged();
        break;

      case UPDATE_NUB_DATASET:
        updateNubDataset();
        break;

      case SCHEMA_UPDATE:
        updateSchema();
        break;

      case REMATCH:
        rematchAll();
        break;

      default:
        throw new UnsupportedOperationException();
    }
  }

  private void rematchAll() throws Exception {
    initRegistry();
    LOG.info("Start sending match dataset messages for all checklists but the Backbone and CoL");

    int counter = 0;
      for (Dataset d : Iterables.datasets(DatasetType.CHECKLIST, datasetService)) {
      if (Constants.COL_DATASET_KEY.equals(d.getKey()) || Constants.NUB_DATASET_KEY.equals(d.getKey())) {
        continue;
      }
      send(new MatchDatasetMessage(d.getKey()));
      if (counter++ % 1000 == 0) {
        LOG.info("Sent {} checklist match messages", counter);
      }
    }
    LOG.info("Sent dataset match message for all {} checklists", counter);
  }

  private void updateSchema() {
    try (Connection c = cfg.clb.connect()) {
      DbSchemaUpdater.update(c);
    } catch (SQLException e) {
      LOG.error("Failed to update db schema", e);
    }
  }

  private void updateNubDataset() {
    Injector clbInj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(cfg.clb));
    DatasetMetricsService metricsService = clbInj.getInstance(DatasetMetricsService.class);

    Injector regInj = cfg.registry.createRegistryInjector();
    BackboneDatasetUpdater nubUpdater = new BackboneDatasetUpdater(regInj.getInstance(DatasetService.class),
        regInj.getInstance(OrganizationService.class), regInj.getInstance(NetworkService.class));

    nubUpdater.updateBackboneDataset(metricsService.get(Constants.NUB_DATASET_KEY));
    LOG.info("Backbone dataset metadata updated.");
  }

  private void sendNubChanged() throws IOException {
    Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(cfg.clb));
    DatasetMetricsService metricsService = inj.getInstance(DatasetMetricsService.class);
    send(new BackboneChangedMessage(metricsService.get(Constants.NUB_DATASET_KEY)));
  }

  private void syncDatasets() {
    initRegistry();
    Injector inj = Guice.createInjector(InternalChecklistBankServiceMyBatisModule.create(cfg.clb));
    DatasetMapper mapper = inj.getInstance(DatasetMapper.class);
    LOG.info("Start syncing datasets from registry to CLB.");
    int counter = 0;
    Iterable<Dataset> datasets = Iterables.datasets(DatasetType.CHECKLIST, datasetService);
    mapper.truncate();
    for (Dataset d : datasets) {
      mapper.insert(new DatasetCore(d));
      counter++;
    }
    LOG.info("{} checklist titles copied", counter);
  }

  /**
   * Cleans up orphan records in the postgres db.
   */
  private void cleanOrphans() {
    Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(cfg.clb));
    ParsedNameServiceMyBatis parsedNameService = (ParsedNameServiceMyBatis) inj.getInstance(ParsedNameService.class);
    LOG.info("Start cleaning up orphan names. This will take a while ...");
    int num = parsedNameService.deleteOrphaned();
    LOG.info("{} orphan names deleted", num);
  }

  private void cleanup(Dataset d) throws IOException {
    try {
      if (cfg.zookeeper.isConfigured()) {
        zk().delete(ZookeeperUtils.getCrawlInfoPath(d.getKey(), null));
        LOG.info("Removed crawl {} from ZK running queue", d.getKey());

        //TODO: clear pending & running queues
      }

      // cleanup repo files
      final File dwcaFile = new File(cfg.archiveRepository, d.getKey() + DWCA_SUFFIX);
      FileUtils.deleteQuietly(dwcaFile);
      File dir = cfg.archiveDir(d.getKey());
      if (dir.exists() && dir.isDirectory()) {
        FileUtils.deleteDirectory(dir);
      }
      LOG.info("Removed dwca files from repository {}", dwcaFile);

      RegistryService.deleteStorageFiles(cfg.neo, d.getKey());

    } catch (Exception e) {
      LOG.error("Failed to cleanup dataset {}", d.getKey(), e);
    }
  }

  private void runDatasetComamnds() throws Exception {
    if (cfg.keys != null) {
      datasets = com.google.common.collect.Iterables.transform(cfg.listKeys(), new Function<UUID, Dataset>() {
        @Nullable
        @Override
        public Dataset apply(UUID key) {
          return datasetService.get(key);
        }
      });
    } else {
      datasets = Iterables.datasets(cfg.key, cfg.type, datasetService, organizationService, installationService, networkService, nodeService);
    }

    for (Dataset d : datasets) {
      LOG.info("{} {} dataset {}: {}", cfg.operation, d.getType(), d.getKey(), d.getTitle().replaceAll("\n", " "));
      if (cfg.operation != AdminOperation.CRAWL && cfg.operation != AdminOperation.CLEANUP) {
        // only deal with checklists for most operations
        if (!DatasetType.CHECKLIST.equals(d.getType())) {
          LOG.warn("Cannot {} dataset of type {}: {} {}", cfg.operation, d.getType(), d.getKey(), d.getTitle());
          continue;
        }
      }

      switch (cfg.operation) {
        case CLEANUP:
          cleanup(d);
          break;

        case CRAWL:
          send(new StartCrawlMessage(d.getKey()));
          break;

        case NORMALIZE:
          if (!cfg.archiveDir(d.getKey()).exists()) {
            LOG.info("Missing dwca file. Cannot normalize dataset {}", title(d));
          } else {
            // validation result is a fake valid checklist validation
            send(new DwcaMetasyncFinishedMessage(d.getKey(), d.getType(),
                    URI.create("http://fake.org"), 1, Maps.<String, UUID>newHashMap(),
                    new DwcaValidationReport(d.getKey(),
                        new GenericValidationReport(1, true, Lists.<String>newArrayList(), Lists.<Integer>newArrayList()))
                )
            );
          }
          break;

        case IMPORT:
          if (!cfg.neo.neoDir(d.getKey()).exists()) {
            LOG.info("Missing neo4j directory. Cannot import dataset {}", title(d));
          } else {
            send(new ChecklistNormalizedMessage(d.getKey()));
          }
          break;

        case ANALYZE:
          send(new ChecklistSyncedMessage(d.getKey(), new Date(), 0, 0));
          break;

        case MATCH:
          send(new MatchDatasetMessage(d.getKey()));
          break;

        case EXPORT:
          export(d);
          break;

        default:
          throw new UnsupportedOperationException();
      }
    }
  }

  private void export(Dataset d) {
    if (exporter == null) {
      // lazily init exporter
      exporter = Exporter.create(cfg.exportRepository, cfg.clb, cfg.registry.wsUrl);
    }
    // now export the dataset
    exporter.export(d);
  }

  /**
   * Reparses all names
   */
  private void reparseNames() {
    new NameUsageReparser(cfg.clb).run();
  }

  private void dumpToNeo() throws Exception {
    LOG.info("Start dumping dataset {} from postgres into neo4j", cfg.key);
    ClbSource src = new ClbSource(cfg.clb, cfg.neo, cfg.key, "Checklist " + cfg.key, null);
    src.init(true, cfg.nubRanksOnly);
  }

  private void verifyNeo() throws Exception {
    UsageDao dao = null;
    try {
      dao = UsageDao.open(cfg.neo, cfg.key);
      NubDb db = NubDb.open(dao, AuthorComparator.createWithoutAuthormap());

      validate(dao, new NubTreeValidation(db));
      validate(dao, new NubAssertions(db));

    } finally {
      if (dao != null) {
        dao.close();
      }
    }
  }
  
  private void verifyPg() throws Exception {
    Injector clbInj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(cfg.clb));
    NameUsageService usageService = clbInj.getInstance(NameUsageService.class);
  
    NubAssertions validator = new NubAssertions(usageService);
    if (cfg.file != null) {
      validator.setAssertionFile(cfg.file);
    }
    if (cfg.file2 != null) {
      validator.setHomonymFile(cfg.file2);
    }
    if (validator.validate()) {
      LOG.info("{} passed!", validator.getClass().getSimpleName());
    } else {
      LOG.error("Backbone is not valid! {} failed", validator.getClass().getSimpleName());
    }
  }
  
  private void validate(UsageDao dao, NubValidation validator) {
    try (Transaction tx = dao.beginTx()) {
      boolean valid = validator.validate();
      if (valid) {
        LOG.info("{} passed!", validator.getClass().getSimpleName());
      } else {
        LOG.error("Backbone is not valid! {} failed", validator.getClass().getSimpleName());
      }
    }
  }

  private String title(Dataset d) {
    return d.getKey() + ": " + d.getTitle().replaceAll("\n", " ");
  }
}
