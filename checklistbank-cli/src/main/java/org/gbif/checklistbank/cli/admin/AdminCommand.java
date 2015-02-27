package org.gbif.checklistbank.cli.admin;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.ChecklistValidationReport;
import org.gbif.api.model.crawler.DwcaValidationReport;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;
import org.gbif.common.messaging.api.messages.DwcaMetasyncFinishedMessage;
import org.gbif.common.messaging.api.messages.StartCrawlMessage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public AdminCommand() {
    super("admin");
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

  private DatasetService ds() {
    if (datasetService == null) {
      initRegistry();
    }
    return datasetService;
  }

  private OrganizationService os() {
    if (organizationService == null) {
      initRegistry();
    }
    return organizationService;
  }

  private void initRegistry() {
    Injector inj = cfg.registry.createRegistryInjector();
    datasetService  = inj.getInstance(DatasetService.class);
    organizationService  = inj.getInstance(OrganizationService.class);
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
    try {
      switch (cfg.operation) {
        case CRAWL:
          crawl(cfg.key);

        case NORMALIZE:
          // validation result is a fake valid checklist validation
          send( new DwcaMetasyncFinishedMessage(cfg.key, DatasetType.CHECKLIST,
                  URI.create("http://fake.org"), 1, Maps.<String, UUID>newHashMap(),
                  new DwcaValidationReport(cfg.key,
                    new ChecklistValidationReport(1, true, Lists.<String>newArrayList(), Lists.<Integer>newArrayList()))
                  )
          );
          break;

        case IMPORT:
          send( new ChecklistNormalizedMessage(cfg.key, new NormalizerStats(1,1,0,0,
            Maps.<Origin, Integer>newHashMap(), Maps.<Rank, Integer>newHashMap(), Lists.<String>newArrayList())));
          break;

        case ANALYZE:
          send( new ChecklistSyncedMessage(cfg.key, new Date(), 0, 0) );
          break;

        case CLEANUP:
          cleanup(cfg.key);
          break;

        default:
          throw new UnsupportedOperationException();
      }

    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param key publisher or dataset key
   */
  private void cleanup(final UUID key) throws IOException {
    if (isDataset(key)) {
      cleanupCrawl(key);

    } else if (isOrg(key)) {
      final PagingRequest page = new PagingRequest(0, 25);
      PagingResponse<Dataset> resp = null;
      while (resp == null || !resp.isEndOfRecords()) {
        resp = os().publishedDatasets(key, page);
        for (Dataset d : resp.getResults()) {
          if (cfg.type != null && d.getType() != cfg.type) {
            LOG.info("Ignore {} dataset {}: {}", d.getType(), d.getKey(), d.getTitle().replaceAll("\n", " "));
            continue;
          }
          try {
            cleanupCrawl(d.getKey());
          } catch (IOException e) {
            LOG.warn("Failed to cleanup crawl {}: {}", d.getKey(), e.getMessage());
          }
        }
        page.nextPage();
      }
    } else {
      LOG.warn("Given key is neither a dataset nor a publisher: {}", key);
    }
  }

  /**
   * @param key publisher or dataset key
   */
  private void crawl(final UUID key) throws IOException {
    if (isDataset(key)) {
      crawlDataset(key);

    } else if (isOrg(key)) {
      final PagingRequest page = new PagingRequest(0, 10);
      PagingResponse<Dataset> resp = null;
      int counter = 0;
      while (resp == null || !resp.isEndOfRecords()) {
        resp = os().publishedDatasets(key, page);
        for (Dataset d : resp.getResults()) {
          if (d.getDeleted() != null) {
            LOG.info("Ignore deleted dataset {}: {}", d.getKey(), d.getTitle().replaceAll("\n", " "));
            continue;
          }
          if (cfg.type != null && d.getType() != cfg.type) {
            LOG.info("Ignore {} dataset {}: {}", d.getType(), d.getKey(), d.getTitle().replaceAll("\n", " "));
            continue;
          }
          counter++;
          LOG.info("Crawl {} - {}: {}", counter, d.getKey(), d.getTitle().replaceAll("\n", " "));
          crawlDataset(key);
        }
        page.nextPage();
      }
    } else {
      LOG.warn("Given key is neither a dataset nor a publisher: {}", key);
    }
  }

  private void crawlDataset(UUID key) throws IOException {
    cleanupCrawl(key);
    send( new StartCrawlMessage(key));
  }

  private boolean isOrg(UUID key) {
    return os().get(key) != null;
  }

  private boolean isDataset(UUID key) {
    return ds().get(key) != null;
  }

  private void cleanupCrawl(final UUID datasetKey) throws IOException {
    zk().delete(ZookeeperUtils.getCrawlInfoPath(datasetKey, null));
    LOG.info("Removed crawl {} from zookeeper", datasetKey);
    // cleanup repo files
    final File dwcaFile = new File(cfg.archiveRepository, datasetKey + DWCA_SUFFIX);
    FileUtils.deleteQuietly(dwcaFile);
    File dir = cfg.archiveDir(datasetKey);
    if (dir.exists() && dir.isDirectory()) {
      FileUtils.deleteDirectory(dir);
    }
    LOG.info("Removed dwca files from repository {}", dwcaFile);
  }

}
