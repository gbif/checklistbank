package org.gbif.checklistbank.cli.common;

import org.gbif.checklistbank.config.GangliaConfiguration;
import org.gbif.checklistbank.config.MetricModule;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.IOException;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base service that provides convenience methods to interact with rabbit and can set up a ganglia reported metric registry.
 * A hikari db pool is properly closed at the end if needed.
 * @param <T>
 */
public abstract class RabbitBaseService<T extends Message> extends AbstractIdleService implements MessageCallback<T> {
  private static final Logger LOG = LoggerFactory.getLogger(RabbitBaseService.class);

  private final MessagingConfiguration mCfg;
  private final int poolSize;
  private final String queue;
  private final Injector injector;
  private MetricRegistry registry;
  protected HikariDataSource hds;
  protected MessagePublisher publisher;
  protected MessageListener listener;


  public RabbitBaseService(String queue, int poolSize, MessagingConfiguration mCfg, GangliaConfiguration gCfg, List<Module> modules) {
    this.mCfg = mCfg;
    this.poolSize = poolSize;
    this.queue = queue;
    injector = Guice.createInjector(ImmutableList.<Module>builder()
        .add(new MetricModule(gCfg))
        .addAll(modules)
        .build());
    this.registry = injector.getInstance(MetricRegistry.class);
    initMetrics(this.registry);
    // keep a reference to the hikari pool so we can close it properly on shutdown
    for (Module m : modules) {
      if (m instanceof ChecklistBankServiceMyBatisModule || m instanceof InternalChecklistBankServiceMyBatisModule) {
        hds = (HikariDataSource) getInstance(InternalChecklistBankServiceMyBatisModule.DATASOURCE_KEY);
        break;
      }
    }
  }

  public RabbitBaseService(String queue, int poolSize, MessagingConfiguration mCfg, GangliaConfiguration gCfg, Module ... modules) {
    this(queue, poolSize, mCfg, gCfg, Lists.newArrayList(modules));
  }

  public MetricRegistry getRegistry() {
    return registry;
  }

  public Injector injector() {
    return injector;
  }

  public <T> T getInstance(Class<T> clazz) {
    return injector.getInstance(clazz);
  }

  public <T> T getInstance(Key<T> key) {
    return injector.getInstance(key);
  }

  protected String regName(String name) {
    return queue + "." + name;
  }

  /**
   * Binds metrics to an existing metrics registry.
   * override this method to add more service specific metrics
   */
  protected void initMetrics(MetricRegistry registry) {
    registry.registerAll(new MemoryUsageGaugeSet());
    registry.register(Metrics.OPEN_FILES, new FileDescriptorRatioGauge());
  }

  @Override
  protected void startUp() throws Exception {
    publisher = new DefaultMessagePublisher(mCfg.getConnectionParameters());

    // dataset messages are slow, long running processes. Only prefetch one message
    listener = new MessageListener(mCfg.getConnectionParameters(), 1);
    startUpBeforeListening();
    listener.listen(queue, poolSize, this);
  }

  /**
   * Hook to bind startup code to that gets executed before the listener actually starts listening to messages!
   */
  protected void startUpBeforeListening() throws Exception {
    // dont do anything by default
  }

  @Override
  protected void shutDown() throws Exception {
    if (hds != null) {
      hds.close();
    }
    if (listener != null) {
      listener.close();
    }
    if (publisher != null) {
      publisher.close();
    }
  }

  protected void send(Message msg) throws IOException {
    try {
      LOG.info("Sending {}", msg.getClass().getSimpleName());
      publisher.send(msg);
    } catch (IOException e) {
      LOG.error("Could not send {}", msg.getClass().getSimpleName(), e);
      throw e;
    }
  }

}
