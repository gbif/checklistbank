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
package org.gbif.checklistbank.cli.common;

import org.gbif.checklistbank.config.GangliaConfiguration;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.util.concurrent.AbstractIdleService;

/**
 * A base service that provides convenience methods to interact with rabbit and can set up a ganglia reported metric registry.
 * A hikari db pool is properly closed at the end if needed.
 * @param <T>
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class RabbitBaseService<T extends Message> extends AbstractIdleService implements MessageCallback<T> {

  private static final Logger LOG = LoggerFactory.getLogger(RabbitBaseService.class);

  private final MessagingConfiguration mCfg;
  private final GangliaConfiguration gCfg;
  private final int poolSize;
  private final String queue;
  private final MetricRegistry registry;

  protected MessagePublisher publisher;
  protected MessageListener listener;

  public RabbitBaseService(String queue, int poolSize, MessagingConfiguration mCfg, GangliaConfiguration gCfg) {
    this.mCfg = mCfg;
    this.gCfg = gCfg;
    this.poolSize = poolSize;
    this.queue = queue;
    this.registry = new MetricRegistry();
    initMetrics();
  }

  public MetricRegistry getRegistry() {
    return registry;
  }

  protected String regName(String name) {
    return queue + "." + name;
  }

  /**
   * Binds metrics to an existing metrics registry.
   * override this method to add more service specific metrics
   */
  protected void initMetrics() {
    gCfg.start(registry);
    registry.registerAll(new MemoryUsageGaugeSet());
    // this is broken in Java11 - we need to update metrics and thus Dropwizard!
    //registry.register(Metrics.OPEN_FILES, new FileDescriptorRatioGauge());
  }

  @Override
  protected void startUp() throws Exception {
    publisher = new DefaultMessagePublisher(mCfg.getConnectionParameters());

    // dataset messages are slow, long-running processes. Only prefetch one message
    listener = new MessageListener(mCfg.getConnectionParameters(), 1);
    startUpBeforeListening();
    listener.listen(queue, poolSize, this);
  }

  /**
   * Hook to bind startup code to that gets executed before the listener actually starts listening to messages!
   */
  protected void startUpBeforeListening() throws Exception {
    // don't do anything by default
  }

  @Override
  protected void shutDown() throws Exception {
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
