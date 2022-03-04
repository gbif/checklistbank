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
package org.gbif.checklistbank.cli.config;


import org.gbif.common.search.es.EsClient;

import org.elasticsearch.client.RestHighLevelClient;

import com.beust.jcommander.Parameter;

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
  public int syncThreads = 60000;

  public RestHighLevelClient buildClient() {
    EsClient.EsClientConfiguration esClientConfiguration = new EsClient.EsClientConfiguration();
    esClientConfiguration.setHosts(hosts);
    esClientConfiguration.setConnectionRequestTimeOut(connectionRequestTimeOut);
    esClientConfiguration.setConnectionTimeOut(connectionTimeOut);
    esClientConfiguration.setSocketTimeOut(socketTimeOut);
    return EsClient.provideEsClient(esClientConfiguration);
  }

}
