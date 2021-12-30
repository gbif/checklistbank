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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ganglia.GangliaReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;

import info.ganglia.gmetric4j.gmetric.GMetric;

/**
 * A configuration class which holds the host and port to connect yammer metrics to a ganglia server.
 */
@SuppressWarnings("PublicField")
@Component
@ConditionalOnProperty(name = "checklistbank.ganglia.host")
public class GangliaConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(GangliaConfiguration.class);

  @Parameter(names = "--ganglia-host")
  @Value("${checklistbank.ganglia.host}")
  public String host;

  @Parameter(names = "--ganglia-port")
  @Value("${checklistbank.ganglia.port}")
  public int port = 8649;

  /**
   * Starts the GangliaReporter, pointing to the configured host and port.
   */
  @JsonIgnore
  public void start(MetricRegistry registry) {
    if (host != null && port > 0) {
      try {
        final GMetric ganglia = new GMetric(host, port, GMetric.UDPAddressingMode.MULTICAST, 1);
        final GangliaReporter reporter = GangliaReporter.forRegistry(registry)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build(ganglia);
        reporter.start(1, TimeUnit.MINUTES);
        LOG.info("Reporting to ganglia at {}:{}", host, port);
      } catch (IOException e) {
        LOG.warn("Failed to setup ganglia reporting at {}:{}", host, port, e);
      }
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("host", host)
      .add("port", port)
      .toString();
  }
}
