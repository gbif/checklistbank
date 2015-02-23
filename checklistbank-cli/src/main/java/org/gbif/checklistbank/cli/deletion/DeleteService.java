package org.gbif.checklistbank.cli.deletion;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.messages.RegistryChangeMessage;

import java.util.UUID;
import javax.sql.DataSource;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteService extends AbstractIdleService implements MessageCallback<RegistryChangeMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(DeleteService.class);

  public static final String QUEUE = "clb-deletion";

  public static final String DELETION_METER = "checklist.deleted";

  private final DeleteConfiguration cfg;
  private MessageListener listener;
  private NameUsageIndexService solrService;
  private DatasetImportService mybatisService;
  private final MetricRegistry registry = new MetricRegistry("checklist.deletion");
  private final Timer timer = registry.timer("checklist.deletion.time");
  // we need to properly close the connection pool, so we keep a reference
  private HikariDataSource hds;

  public DeleteService(DeleteConfiguration configuration) {
    this.cfg = configuration;
    registry.meter(DELETION_METER);
  }

  @Override
  protected void startUp() throws Exception {
    cfg.ganglia.start(registry);

    // init mybatis layer and solr from cfg instance
    Key<DataSource> dsKey = Key.get(DataSource.class, Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME));
    Injector inj = Guice.createInjector(cfg.clb.createServiceModule(), new RealTimeModule(cfg.solr));
    hds = (HikariDataSource) inj.getInstance(dsKey);
    solrService = inj.getInstance(NameUsageIndexService.class);
    mybatisService = inj.getInstance(DatasetImportService.class);

    listener = new MessageListener(cfg.messaging.getConnectionParameters());
    listener.listen(QUEUE, cfg.messaging.poolSize, this);
  }

  @Override
  protected void shutDown() throws Exception {
    if (listener != null) {
      listener.close();
    }
    hds.close();
  }

  @Override
  public void handleMessage(RegistryChangeMessage msg) {
    if (RegistryChangeMessage.ChangeType.DELETED == msg.getChangeType() && Dataset.class.equals(msg.getObjectClass())) {
      final Timer.Context context = timer.time();
      Dataset d = (Dataset) msg.getOldObject();
      try {
        if (DatasetType.CHECKLIST == d.getType()) {
          deleteChecklist(d.getKey());
        }
      } catch (Throwable e) {
        LOG.error("Failed to analyze dataset [{}]", d.getKey(), e);

      } finally {
        context.stop();
      }
    }
  }

  private void deleteChecklist(final UUID key) {
    LOG.info("Deleting data for checklist {}", key);
    // solr
    solrService.delete(key);
    // postgres usage
    mybatisService.deleteDataset(key);
    LOG.info("Deleted data for checklist {}", key);
  }

  @Override
  public Class<RegistryChangeMessage> getMessageClass() {
    return RegistryChangeMessage.class;
  }

}
