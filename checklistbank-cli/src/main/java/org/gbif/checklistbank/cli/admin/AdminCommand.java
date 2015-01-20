package org.gbif.checklistbank.cli.admin;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.ChecklistValidationReport;
import org.gbif.api.model.crawler.DwcaValidationReport;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.common.messaging.DefaultMessagePublisher;
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
import com.google.common.collect.Maps;
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

  public AdminCommand() {
    super("admin");
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

  @Override
  protected void doRun() {
    try {
      publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());
      switch (cfg.operation) {
        case DOWNLOAD:
          publisher.send( new StartCrawlMessage(cfg.datasetKey));

        case NORMALIZE:
          // validation result is a fake valid checklist validation
          publisher.send( new DwcaMetasyncFinishedMessage(cfg.datasetKey, DatasetType.CHECKLIST,
                  URI.create("http://fake.org"), 1, Maps.<String, UUID>newHashMap(),
                  new DwcaValidationReport(cfg.datasetKey,
                    new ChecklistValidationReport(1, true, Lists.<String>newArrayList(), Lists.<Integer>newArrayList()))
                  )
          );
          break;

        case IMPORT:
          publisher.send( new ChecklistNormalizedMessage(cfg.datasetKey, new NormalizerStats(1,1,0,0,
            Maps.<Origin, Integer>newHashMap(), Maps.<Rank, Integer>newHashMap(), Lists.<String>newArrayList())) );
          break;

        case ANALYZE:
          publisher.send( new ChecklistSyncedMessage(cfg.datasetKey, new Date(), 0, 0) );
          break;

        case CRAWL_PUBLISHER:
          crawlPublisher(cfg.publisherKey);
          break;

        case CLEANUP:
          cleanupCrawl(cfg.datasetKey);
          break;

        default:
          throw new UnsupportedOperationException();
      }

    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private void cleanupCrawl(final UUID datasetKey) throws IOException {
    ZookeeperUtils zkUtils = new ZookeeperUtils(cfg.zookeeper.getCuratorFramework());
    zkUtils.delete(ZookeeperUtils.getCrawlInfoPath(datasetKey, null));
    // cleanup repo files
    final File dwcaFile = new File(cfg.archiveRepository, datasetKey + DWCA_SUFFIX);
    FileUtils.deleteQuietly(dwcaFile);
    FileUtils.deleteDirectory(cfg.archiveDir(datasetKey));
  }

  private void crawlPublisher(final UUID orgKey) throws IOException, InterruptedException {
    final OrganizationService orgService = cfg.registry.createRegistryInjector().getInstance(OrganizationService.class);
    final PagingRequest page = new PagingRequest(0, 10);
    PagingResponse<Dataset> resp = null;
    int counter = 0;
    while (resp == null || !resp.isEndOfRecords()) {
      resp = orgService.publishedDatasets(orgKey, page);
      for (Dataset d : resp.getResults()) {
        counter++;
        LOG.info("Crawl {} - {}: {}", counter, d.getKey(), d.getTitle());
        publisher.send( new StartCrawlMessage(d.getKey()));
      }
      Thread.sleep(10000);
      page.nextPage();
    }
  }

}
