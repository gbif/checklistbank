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
package org.gbif.checklistbank.index;

import com.beust.jcommander.Parameter;
import org.gbif.common.search.es.EsClient;

import javax.validation.constraints.Min;

/**
 * Configuration specific to interfacing with elastic search.
 */
public class ElasticsearchConfiguration {

  @Parameter(names = {"--es-hosts"})
  public String hosts;

  @Parameter(names = {"--es-connection-time-out"})
  public int connectionTimeOut = 60000;

  @Parameter(names = {"--es-socket-time-out"})
  public int socketTimeOut = 60000;

  @Parameter(names = {"--es-connection-request-time-out"})
  public int connectionRequestTimeOut = 120000;

  @Parameter(names = {"--es-alias"})
  public String alias;

  @Parameter(names = {"--es-index"})
  public String index;

  @Parameter(names = {"--es-sync-threads"})
  @Min(0)
  public int syncThreads = 2;

  public EsClient buildClient() {
    EsClient.EsClientConfiguration elasticsearchConfiguration = new EsClient.EsClientConfiguration();
    elasticsearchConfiguration.setHosts(hosts);
    elasticsearchConfiguration.setConnectionTimeOut(connectionTimeOut);
    elasticsearchConfiguration.setConnectionRequestTimeOut(connectionRequestTimeOut);
    elasticsearchConfiguration.setSocketTimeOut(socketTimeOut);
    return new EsClient(EsClient.provideEsClient(elasticsearchConfiguration));
  }

}
