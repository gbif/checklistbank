package org.gbif.checklistbank.cli.admin;

import org.gbif.api.model.Constants;
import org.gbif.api.model.crawler.DwcaValidationReport;
import org.gbif.api.model.crawler.GenericValidationReport;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.checklistbank.cli.matcher.MatchDatasetMessage;
import org.gbif.checklistbank.cli.registry.RegistryService;
import org.gbif.checklistbank.kryo.migrate.VerbatimUsageMigrator;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.NubAssertions;
import org.gbif.checklistbank.nub.NubDb;
import org.gbif.checklistbank.nub.NubTreeValidation;
import org.gbif.checklistbank.nub.TreeValidation;
import org.gbif.checklistbank.nub.source.ClbSource;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.ParsedNameServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.mapper.DatasetMapper;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;
import org.gbif.common.messaging.api.messages.DwcaMetasyncFinishedMessage;
import org.gbif.common.messaging.api.messages.StartCrawlMessage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.kohsuke.MetaInfServices;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command that issues new normalize or import messages for manual admin purposes.
 */
@MetaInfServices(Command.class)
public class AdminCommand extends BaseCommand {
  private static final Logger LOG = LoggerFactory.getLogger(AdminCommand.class);
  private static final String DWCA_SUFFIX = ".dwca";
  private static final Set<AdminOperation> SINE_COMMANDS = Sets.newHashSet(
      AdminOperation.REPARSE,
      AdminOperation.CLEAN_ORPHANS,
      AdminOperation.SYNC_DATASETS,
      AdminOperation.UPDATE_VERBATIM,
      AdminOperation.DUMP,
      AdminOperation.VALIDATE_NEO,
      AdminOperation.NUB_CHANGED
  );
  private final AdminConfiguration cfg = new AdminConfiguration();
  private MessagePublisher publisher;
  private ZookeeperUtils zkUtils;
  private DatasetService datasetService;
  private OrganizationService organizationService;
  private InstallationService installationService;
  private NetworkService networkService;
  private NodeService nodeService;
  private Iterable<Dataset> datasets;

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
    if (cfg.col) {
      if (cfg.key != null) {
        LOG.warn("Explicit dataset key given, ignore col flag");
      } else {
        cfg.key = Constants.COL_DATASET_KEY;
      }
    }
    if (cfg.nub) {
      if (cfg.key != null) {
        LOG.warn("Explicit dataset key given, ignore nub flag");
      } else {
        cfg.key = Constants.NUB_DATASET_KEY;
      }
    }
  }

  private ZookeeperUtils zk() {
    if (zkUtils == null) {
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
      if (SINE_COMMANDS.contains(cfg.operation)) {
        runSineDatasets();
      } else {
        initRegistry();
        runDatasetComamnds();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runSineDatasets() throws Exception {
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

      case UPDATE_VERBATIM:
        updateVerbatim();
        break;

      case DUMP:
        dumpToNeo();
        break;

      case VALIDATE_NEO:
        verifyNeo();
        break;

      case NUB_CHANGED:
        sendNubChanged();
        break;

      default:
        throw new UnsupportedOperationException();
    }
  }

  private void sendNubChanged() throws IOException {
    Injector inj = Guice.createInjector(cfg.clb.createServiceModule());
    DatasetMetricsService metricsService = inj.getInstance(DatasetMetricsService.class);
    send(new BackboneChangedMessage(metricsService.get(Constants.NUB_DATASET_KEY)));
  }

  private void updateVerbatim() throws Exception {
    VerbatimUsageMigrator migrator = new VerbatimUsageMigrator(cfg.clb);
    migrator.updateAll();
  }

  private void syncDatasets() {
    initRegistry();
    Injector inj = Guice.createInjector(cfg.clb.createMapperModule());
    DatasetMapper mapper = inj.getInstance(DatasetMapper.class);
    LOG.info("Start syncing datasets from registry to CLB.");
    int counter = 0;
    Iterable<Dataset> datasets = Iterables.datasets(DatasetType.CHECKLIST, datasetService);
    mapper.truncate();
    for (Dataset d : datasets) {
      mapper.insert(d.getKey(), d.getTitle());
      counter++;
    }
    LOG.info("{} checklist titles copied", counter);
  }

  /**
   * Cleans up orphan records in the postgres db.
   */
  private void cleanOrphans() {
    Injector inj = Guice.createInjector(cfg.clb.createServiceModule());
    ParsedNameServiceMyBatis parsedNameService = (ParsedNameServiceMyBatis) inj.getInstance(ParsedNameService.class);
    LOG.info("Start cleaning up orphan names. This will take a while ...");
    int num = parsedNameService.deleteOrphaned();
    LOG.info("{} orphan names deleted", num);
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
      switch (cfg.operation) {
        case CLEANUP:
          zk().delete(ZookeeperUtils.getCrawlInfoPath(d.getKey(), null));
          LOG.info("Removed crawl {} from zookeeper", d.getKey());

          // cleanup repo files
          final File dwcaFile = new File(cfg.archiveRepository, d.getKey() + DWCA_SUFFIX);
          FileUtils.deleteQuietly(dwcaFile);
          File dir = cfg.archiveDir(d.getKey());
          if (dir.exists() && dir.isDirectory()) {
            FileUtils.deleteDirectory(dir);
          }
          LOG.info("Removed dwca files from repository {}", dwcaFile);

          RegistryService.deleteStorageFiles(cfg.neo, d.getKey());
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

        case MATCH_DATASET:
          if (DatasetType.CHECKLIST.equals(d.getType())) {
            send(new MatchDatasetMessage(d.getKey()));
          } else {
            LOG.warn("Cannot match dataset of type {}: {} {}", d.getType(), d.getKey(), d.getTitle());
          }
          break;

        default:
          throw new UnsupportedOperationException();
      }
    }
  }

  /**
   * Reparses all names
   */
  private void reparseNames() {
    Injector inj = Guice.createInjector(cfg.clb.createServiceModule());
    ParsedNameService parsedNameService = inj.getInstance(ParsedNameService.class);
    LOG.info("Start reparsing all names. This will take a while ...");
    int num = parsedNameService.reparseAll();
    LOG.info("{} names reparsed", num);
  }

  private void dumpToNeo() throws Exception {
    LOG.info("Start dumping dataset {} from postgres into neo4j", cfg.key);
    ClbSource src = new ClbSource(cfg.clb, cfg.key, "Checklist " + cfg.key);
    src.setNeoRepository(cfg.neo.neoRepository);
    src.init(true, cfg.nubRanksOnly);
  }

  private void verifyNeo() throws Exception {
    UsageDao dao = null;
    try {
      dao = UsageDao.open(cfg.neo, cfg.key);
      NubDb db = NubDb.open(dao, AuthorComparator.createWithoutAuthormap());

      validate(dao, new NubTreeValidation(db));
      LOG.info("Tree validation passed!");

      validate(dao, new NubAssertions(db));
      LOG.info("Nub assertions passed!");

    } finally {
      if (dao != null) {
        dao.close();
      }
    }
  }

  private void validate(UsageDao dao, TreeValidation validator) throws AssertionError {
    try (Transaction tx = dao.beginTx()) {
      boolean valid = validator.validate();
      if (!valid) {
        LOG.error("Backbone is not valid!");
        throw new AssertionError("Backbone is not valid!");
      }
    }
  }

  private String title(Dataset d) {
    return d.getKey() + ": " + d.getTitle().replaceAll("\n", " ");
  }
}
