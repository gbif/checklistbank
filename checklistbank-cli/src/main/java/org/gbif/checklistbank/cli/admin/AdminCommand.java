package org.gbif.checklistbank.cli.admin;

import org.gbif.api.model.Constants;
import org.gbif.api.model.crawler.DwcaValidationReport;
import org.gbif.api.model.crawler.GenericValidationReport;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.cli.common.SpringContextBuilder;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.checklistbank.cli.nubbuild.BackboneDatasetUpdater;
import org.gbif.checklistbank.cli.registry.RegistryService;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.DatasetCore;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.NubDb;
import org.gbif.checklistbank.nub.source.ClbSource;
import org.gbif.checklistbank.nub.validation.NubAssertions;
import org.gbif.checklistbank.nub.validation.NubTreeValidation;
import org.gbif.checklistbank.nub.validation.NubValidation;
import org.gbif.checklistbank.service.mybatis.export.Exporter;
import org.gbif.checklistbank.service.mybatis.persistence.liquibase.DbSchemaUpdater;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.DatasetMapper;
import org.gbif.checklistbank.service.mybatis.service.NameUsageServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.ParsedNameServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.tmp.NameUsageReparser;
import org.gbif.checklistbank.ws.client.DatasetMetricsClient;
import org.gbif.checklistbank.ws.client.NubResourceClient;
import org.gbif.cli.BaseCommand;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;
import org.gbif.common.messaging.api.messages.DwcaMetasyncFinishedMessage;
import org.gbif.common.messaging.api.messages.MatchDatasetMessage;
import org.gbif.common.messaging.api.messages.Platform;
import org.gbif.common.messaging.api.messages.StartCrawlMessage;
import org.gbif.registry.ws.client.DatasetClient;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.registry.ws.client.NetworkClient;
import org.gbif.registry.ws.client.NodeClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.ws.client.ClientBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * Command that issues new normalize or import messages for manual admin purposes.
 */
public class AdminCommand extends BaseCommand {

  private static final Logger LOG = LoggerFactory.getLogger(AdminCommand.class);

  private static final String DWCA_SUFFIX = ".dwca";

  private ApplicationContext ctx;

  private final AdminConfiguration cfg = new AdminConfiguration();

  // TODO read or read-write?
  private DatasetService datasetService;
  private OrganizationService organizationService;
  private InstallationService installationService;
  private NetworkService networkService;
  private NodeService nodeService;

  private MessagePublisher publisher;
  private ZookeeperUtils zkUtils;
  private Exporter exporter;

  public AdminCommand() {
    super("admin");
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

  private void initRegistry() {
    // TODO: 03/01/2022 do we need user/password here?
    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withUrl(cfg.registry.wsUrl);
    datasetService = clientBuilder.build(DatasetClient.class);
    organizationService = clientBuilder.build(OrganizationClient.class);
    installationService = clientBuilder.build(InstallationClient.class);
    networkService = clientBuilder.build(NetworkClient.class);
    nodeService = clientBuilder.build(NodeClient.class);
  }

  private void initCfg() {
    setKnownKey("col", Constants.COL_DATASET_KEY);
    setKnownKey("nub", Constants.NUB_DATASET_KEY);
    setKnownKey("plazi", UUID.fromString("7ce8aef0-9e92-11dc-8738-b8a03c50a862"));
    setKnownKey("iucn", UUID.fromString("19491596-35ae-4a91-9a98-85cf505f1bd3"));
  }

  private void initContext(ClbConfiguration clbConfiguration) {
    ctx = SpringContextBuilder.create().withClbConfiguration(clbConfiguration).build();
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
    initContext(cfg.clb);
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

      case NUB_CHECK:
        nubCheck();
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

  private void nubCheck() throws Exception {
    Preconditions.checkNotNull(cfg.file, "file config required for test data");
    Preconditions.checkNotNull(cfg.nubws, "nubws config required");

    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withUrl(cfg.nubws);
    NameUsageMatchingService src = clientBuilder.build(NubResourceClient.class);
    new NubCheck(src).testFile(cfg.file);
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
    ClientBuilder clientBuilder = new ClientBuilder();
    // TODO: 04/01/2022 what url?
    clientBuilder.withUrl("");
    DatasetMetricsService metricsService = clientBuilder.build(DatasetMetricsClient.class);

    BackboneDatasetUpdater nubUpdater = new BackboneDatasetUpdater(datasetService, organizationService, networkService);

    nubUpdater.updateBackboneDataset(metricsService.get(Constants.NUB_DATASET_KEY));
    LOG.info("Backbone dataset metadata updated.");
  }

  private void sendNubChanged() throws IOException {
    ClientBuilder clientBuilder = new ClientBuilder();
    // TODO: 04/01/2022 what url?
    clientBuilder.withUrl("");
    DatasetMetricsService metricsService = clientBuilder.build(DatasetMetricsClient.class);
    send(new BackboneChangedMessage(metricsService.get(Constants.NUB_DATASET_KEY)));
  }

  private void syncDatasets() {
    initRegistry();
    DatasetMapper mapper = ctx.getBean(DatasetMapper.class);
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
    ParsedNameServiceMyBatis parsedNameService = ctx.getBean(ParsedNameServiceMyBatis.class);
    LOG.info("Start cleaning up orphan names. This will take a while ...");
    int num = parsedNameService.deleteOrphaned();
    LOG.info("{} orphan names deleted", num);
  }

  private void cleanup(Dataset d) {
    Objects.requireNonNull(d.getKey(), "datasetUuid can't be null");
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
    Iterable<Dataset> datasets;
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
          Objects.requireNonNull(d.getKey(), "datasetUuid can't be null");
          send(new StartCrawlMessage(d.getKey()));
          break;

        case NORMALIZE:
          Objects.requireNonNull(d.getKey(), "datasetUuid can't be null");
          if (!cfg.archiveDir(d.getKey()).exists()) {
            LOG.info("Missing dwca file. Cannot normalize dataset {}", title(d));
          } else {
            // validation result is a fake valid checklist validation
            send(
                new DwcaMetasyncFinishedMessage(
                    d.getKey(),
                    d.getType(),
                    URI.create("http://fake.org"),
                    1,
                    new HashMap<>(),
                    new DwcaValidationReport(
                        d.getKey(),
                        new GenericValidationReport(1, true, new ArrayList<>(), new ArrayList<>())),
                    Platform.ALL
                )
            );
          }
          break;

        case IMPORT:
          Objects.requireNonNull(d.getKey(), "datasetUuid can't be null");
          if (!cfg.neo.neoDir(d.getKey()).exists()) {
            LOG.info("Missing neo4j directory. Cannot import dataset {}", title(d));
          } else {
            send(new ChecklistNormalizedMessage(d.getKey()));
          }
          break;

        case ANALYZE:
          Objects.requireNonNull(d.getKey(), "datasetUuid can't be null");
          send(new ChecklistSyncedMessage(d.getKey(), new Date(), 0, 0));
          break;

        case MATCH:
          Objects.requireNonNull(d.getKey(), "datasetUuid can't be null");
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
      // TODO: 04/01/2022 put property checklistbank.api.url
      exporter = Exporter.create(cfg.exportRepository, ctx);
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

  private void verifyNeo() {
    try (UsageDao dao = UsageDao.open(cfg.neo, cfg.key)) {
      NubDb db = NubDb.open(dao, AuthorComparator.createWithoutAuthormap());

      validate(dao, new NubTreeValidation(db));
      validate(dao, new NubAssertions(db));

    }
  }

  private void verifyPg() {
    NameUsageService usageService = ctx.getBean(NameUsageServiceMyBatis.class);

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
