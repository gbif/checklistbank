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
import org.gbif.checklistbank.logging.LogContext;
import org.gbif.common.messaging.api.messages.DatasetBasedMessage;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;

public abstract class RabbitDatasetService<T extends DatasetBasedMessage> extends RabbitBaseService<T> {

  private static final Logger LOG = LoggerFactory.getLogger(RabbitDatasetService.class);
  private static final Marker DOI_SMTP = MarkerFactory.getMarker("SMTP");
  private Timer timer;
  private Counter succeeded;
  private Counter failed;
  protected Set<UUID> runningJobs = new HashSet<>();
  private final String action;

  public RabbitDatasetService(String queue, int poolSize, MessagingConfiguration mCfg, GangliaConfiguration gCfg, String action) {
    super(queue, poolSize, mCfg, gCfg);
    this.action = action;
  }

  @Override
  protected void initMetrics() {
    super.initMetrics();
    timer = getRegistry().timer(regName("time"));
    succeeded = getRegistry().counter(regName("succeeded"));
    failed = getRegistry().counter(regName("failed"));
  }

  @Override
  public void handleMessage(T msg) {
    final Timer.Context context = timer.time();
    try {
      LogContext.startDataset(msg.getDatasetUuid());
      if (!ignore(msg)) {
        if (runningJobs.contains(msg.getDatasetUuid())) {
          LOG.warn("Dataset {} {} job already running. Ignore message", action, msg.getDatasetUuid());
        } else {
          runningJobs.add(msg.getDatasetUuid());
          process(msg);
          succeeded.inc();
          runningJobs.remove(msg.getDatasetUuid());
        }
      }
    } catch (Throwable e) {
      runningJobs.remove(msg.getDatasetUuid());
      LOG.error(DOI_SMTP, "Failed to {} dataset {}", action, msg.getDatasetUuid(), e);
      failed(msg.getDatasetUuid());
      failed.inc();
    } finally {
      context.stop();
      LogContext.endDataset();
    }
  }

  /**
   * Implement this filter method to ignore messages.
   * The default implementation accepts all incoming messages.
   *
   * @param msg the message to check
   *
   * @return true if the message should be ignored
   */
  protected boolean ignore(T msg) {
    return false;
  }

  /**
   * Implement this to do the real work.
   * The method is allowed to throw any exceptions which will be handled by this class.
   * Basic ganglia timer and succeeded/failed counter are also implemented already.
   */
  protected abstract void process(T msg) throws Exception;

  /**
   * Optional hook to implement when the message processing threw an exception.
   *
   * @param datasetKey the dataset being processed
   */
  protected void failed(UUID datasetKey) {
  }

  protected void send(DatasetBasedMessage msg) throws IOException {
    try {
      LOG.info("Sending {} for dataset {}", msg.getClass().getSimpleName(), msg.getDatasetUuid());
      publisher.send(msg);
    } catch (IOException e) {
      LOG.error("Could not send {} for dataset [{}]", msg.getClass().getSimpleName(), msg.getDatasetUuid(), e);
      throw e;
    }
  }

}
