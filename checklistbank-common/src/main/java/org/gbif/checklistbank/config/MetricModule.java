package org.gbif.checklistbank.config;

import org.gbif.checklistbank.utils.PropertiesUtils;

import java.util.Properties;

import com.codahale.metrics.MetricRegistry;
import org.springframework.context.annotation.Bean;

public class MetricModule {
  private final GangliaConfiguration cfg;

  public MetricModule(Properties properties) {
    this.cfg = new GangliaConfiguration();
    cfg.host = properties.getProperty("ganglia.host");
    cfg.port = PropertiesUtils.getIntProp(properties, "ganglia.host", cfg.port);
  }

  public MetricModule(GangliaConfiguration cfg) {
    this.cfg = cfg;
  }

  @Bean
  public GangliaConfiguration provideCfg() throws Exception {
    return cfg;
  }

  @Bean
  public MetricRegistry provideMetricRegistry(GangliaConfiguration cfg) throws Exception {
    MetricRegistry reg = new MetricRegistry();
    cfg.start(reg);
    return reg;
  }
}
