/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.config;

import org.gbif.checklistbank.utils.PropertiesUtils;

import java.util.Properties;

import org.springframework.context.annotation.Bean;

import com.codahale.metrics.MetricRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricModule {
  private GangliaConfiguration cfg;

  public MetricModule() {
    //NOTHING
  }

  public MetricModule(Properties properties) {
    this.cfg = new GangliaConfiguration();
    cfg.host = properties.getProperty("ganglia.host");
    cfg.port = PropertiesUtils.getIntProp(properties, "ganglia.port", cfg.port);
  }

  public MetricModule(GangliaConfiguration cfg) {
    this.cfg = cfg;
  }

  @Bean
  public MetricRegistry provideMetricRegistry(GangliaConfiguration cfg) throws Exception {
    MetricRegistry reg = new MetricRegistry();
    cfg.start(reg);
    return reg;
  }
}
