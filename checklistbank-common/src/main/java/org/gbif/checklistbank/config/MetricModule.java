package org.gbif.checklistbank.config;

import org.gbif.checklistbank.utils.PropertiesUtils;

import java.util.Properties;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class MetricModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(MetricModule.class);

  private final GangliaConfiguration cfg;

  public MetricModule(Properties properties) {
    this.cfg = new GangliaConfiguration();
    cfg.host = properties.getProperty("ganglia.host");
    cfg.port = PropertiesUtils.getIntProp(properties, "ganglia.host", cfg.port);
  }

  public MetricModule(GangliaConfiguration cfg) {
    this.cfg = cfg;
  }

  @Provides
  @Singleton
  public GangliaConfiguration provideCfg() throws Exception {
    return cfg;
  }

  @Provides
  @Singleton
  @Inject
  public MetricRegistry provideMetricRegistry(GangliaConfiguration cfg) throws Exception {
    MetricRegistry reg = new MetricRegistry();
    cfg.start(reg);
    return reg;
  }

  @Override
  protected void configure() {
    // nothing to do
  }
}
