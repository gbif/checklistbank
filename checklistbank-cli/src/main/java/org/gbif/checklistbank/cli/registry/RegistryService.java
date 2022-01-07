package org.gbif.checklistbank.cli.registry;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.RabbitBaseService;
import org.gbif.checklistbank.cli.common.SpringContextBuilder;
import org.gbif.checklistbank.index.NameUsageIndexServiceSolr;
import org.gbif.checklistbank.logging.LogContext;
import org.gbif.checklistbank.model.DatasetCore;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.DatasetMapper;
import org.gbif.checklistbank.service.mybatis.service.DatasetImportServiceMyBatis;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.DefaultMessageRegistry;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.messages.RegistryChangeMessage;
import org.gbif.utils.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A service that watches registry changed messages and does deletions of checklists and
 * updates to the dataset title table in CLB.
 */
public class RegistryService extends RabbitBaseService<RegistryChangeMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(RegistryService.class);

  private ApplicationContext ctx;

  private final RegistryConfiguration cfg;
  private DatasetImportService solrService;
  private DatasetImportService mybatisService;
  private DatasetMapper datasetMapper;
  private Timer timerSolr;
  private Timer timerSql;

  public RegistryService(RegistryConfiguration cfg) {
    // TODO: 05/01/2022 make sure these two modules configured
//    super(null, "clb-registry-change", cfg.poolSize, cfg.messaging, cfg.ganglia, null/*InternalChecklistBankServiceMyBatisModule.create(cfg.clb)*/, new RealTimeModule(cfg.solr));
    super("clb-registry-change", cfg.poolSize, cfg.messaging, cfg.ganglia);
    this.cfg = cfg;

    // TODO: 05/01/2022 add initContext method?
    initContext();
  }

  private void initContext() {
    // TODO: 05/01/2022 configure
    ctx = SpringContextBuilder.create()
        .build();
  }

  @Override
  protected void initMetrics() {
    super.initMetrics();
    timerSolr = getRegistry().timer(regName("solr.time"));
    timerSql = getRegistry().timer(regName("sql.time"));
  }

  /**
   * Deletes all neo and kvp files created during indexing.
   *
   * @param cfg        a neo configuration needed to point to the right setup
   * @param datasetKey the dataset to delete files for
   */
  public static void deleteStorageFiles(NeoConfiguration cfg, UUID datasetKey) {
    // delete neo & kvp storage files
    File kvp = cfg.kvp(datasetKey);
    if (kvp.exists() && !kvp.delete()) {
      LOG.warn("Failed to delete kvo data dir {}", kvp.getAbsoluteFile());
    }

    // delete neo storage files
    File neoDir = cfg.neoDir(datasetKey);
    if (neoDir.exists()) {
      try {
        FileUtils.deleteDirectory(neoDir);
      } catch (IOException e) {
        LOG.warn("Failed to delete neo data dir {}", neoDir.getAbsoluteFile());
      }
    }
    LOG.info("Deleted dataset storage files");
  }

  private void delete(UUID key) throws RuntimeException {
    LogContext.startDataset(key);
    LOG.info("Deleting data for checklist {}", key);
    // solr
    Timer.Context context = timerSolr.time();
    try {
      solrService.deleteDataset(key);
    } catch (Throwable e) {
      LOG.error("Failed to delete dataset with key [{}] from solr", key, e);
    } finally {
      context.stop();
    }

    // postgres usage
    context = timerSql.time();
    try {
      mybatisService.deleteDataset(key);
    } catch (Throwable e) {
      LOG.error("Failed to delete dataset with key [{}] from postgres", key, e);
    } finally {
      context.stop();
    }

    // archives
    deleteStorageFiles(cfg.neo, key);

    // delete dataset table entry
    datasetMapper.delete(key);
    LogContext.endDataset();
  }

  @Override
  public void handleMessage(RegistryChangeMessage msg) {
    if (Dataset.class.equals(msg.getObjectClass())) {
      Dataset d = (Dataset) ObjectUtils.coalesce(msg.getNewObject(), msg.getOldObject());
      if (d != null && DatasetType.CHECKLIST == d.getType()) {
        DatasetCore dc = new DatasetCore(d);
        switch (msg.getChangeType()) {
          case DELETED:
            delete(d.getKey());
            break;
          case UPDATED:
            datasetMapper.update(dc);
            break;
          case CREATED:
            datasetMapper.insert(dc);
            break;
        }
      }
    }
  }

  @Override
  public Class<RegistryChangeMessage> getMessageClass() {
    return RegistryChangeMessage.class;
  }

  @Override
  protected void startUp() throws Exception {
    initContext();

    // TODO: 05/01/2022 make sure these are present in the context
    solrService = ctx.getBean(NameUsageIndexServiceSolr.class);
    mybatisService = ctx.getBean(DatasetImportServiceMyBatis.class);
    datasetMapper = ctx.getBean(DatasetMapper.class);

    publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());
    ObjectMapper objectMapper = ctx.getBean(ObjectMapper.class);

    // dataset messages are slow, long-running processes. Only prefetch one message
    listener = new MessageListener(cfg.messaging.getConnectionParameters(), new DefaultMessageRegistry(), objectMapper, 1);
    startUpBeforeListening();
    listener.listen("clb-registry-change", cfg.poolSize, this);
  }
}
