package org.gbif.checklistbank.cli.registry;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.logging.LogContext;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.RabbitBaseService;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.index.guice.Solr;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.Mybatis;
import org.gbif.checklistbank.service.mybatis.mapper.DatasetMapper;
import org.gbif.common.messaging.api.messages.RegistryChangeMessage;
import org.gbif.utils.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.yammer.metrics.Timer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that watches registry changed messages and does deletions of checklists and
 * updates to the dataset title table in CLB.
 */
public class RegistryService extends RabbitBaseService<RegistryChangeMessage> {


  private static final Logger LOG = LoggerFactory.getLogger(RegistryService.class);

  private final RegistryConfiguration cfg;
  private final DatasetImportService solrService;
  private final DatasetImportService mybatisService;
  private final DatasetMapper datasetMapper;
  private final Timer timerSolr = registry.timer(regName("solr.time"));
  private final Timer timerSql = registry.timer(regName("sql.time"));

  public RegistryService(RegistryConfiguration cfg) {
    super("clb-registry-change", cfg.poolSize, cfg.messaging, cfg.ganglia);
    this.cfg = cfg;

    // init mybatis layer and solr from cfg instance
    Injector inj = Guice.createInjector(InternalChecklistBankServiceMyBatisModule.create(cfg.clb), new RealTimeModule(cfg.solr));
    initDbPool(inj);
    solrService = inj.getInstance(Key.get(DatasetImportService.class, Solr.class));
    mybatisService = inj.getInstance(Key.get(DatasetImportService.class, Mybatis.class));
    datasetMapper = inj.getInstance(DatasetMapper.class);
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
      LOG.error("Failed to delete dataset from solr", key, e);
    } finally {
      context.stop();
    }

    // postgres usage
    context = timerSql.time();
    try {
      mybatisService.deleteDataset(key);
    } catch (Throwable e) {
      LOG.error("Failed to delete dataset from postgres", key, e);
    } finally {
      context.stop();
    }
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
        switch (msg.getChangeType()) {
          case DELETED:
            delete(d.getKey());
            break;
          case UPDATED:
            datasetMapper.update(d.getKey(), d.getTitle());
            break;
          case CREATED:
            datasetMapper.insert(d.getKey(), d.getTitle());
            break;
        }
      }
    }
  }

  @Override
  public Class<RegistryChangeMessage> getMessageClass() {
    return RegistryChangeMessage.class;
  }
}
