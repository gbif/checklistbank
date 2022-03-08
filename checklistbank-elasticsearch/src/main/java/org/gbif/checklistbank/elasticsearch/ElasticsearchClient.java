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
package org.gbif.checklistbank.elasticsearch;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.SneakyThrows;

/**
 * Class to wrap Elasticsearch search operations for index creation and indexing.
 * It uses the configuration classes in this package.
 */
public class ElasticsearchClient {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchClient.class);

  /**
   * Default indexing settings.
   */
  private static final Settings INDEXING_SETTINGS = Settings.builder()
                                                      .put("index.refresh_interval", "-1")
                                                      .put("index.number_of_replicas", "0")
                                                      .put("index.translog.durability", "async")
                                                      .build();
  /**
   * Default search live settings.
   */
  private static final Settings SEARCH_SETTINGS = Settings.builder()
                                                    .put("index.refresh_interval", "1s")
                                                    .put("index.number_of_replicas", "1")
                                                    .build();

  //Wrapped Elasticsearch client
  private final RestHighLevelClient esClient;

  //Configuration instance used to access and establish connections to Elasticsearch
  private final ElasticsearchConfiguration configuration;

  public ElasticsearchClient(ElasticsearchConfiguration configuration) {
    this.esClient = buildEsClient(configuration);
    this.configuration = configuration;
    //Generate index name if it is not provided
    if (configuration.getIndex() == null) {
       configuration.setIndex(getEsIndexingIdxName(configuration.getAlias()));
    }
  }

  /**
   * Creates the indexing settings using the desired number of shards.
   */
  private static Settings indexingSettings(int numberOfShards) {
    return Settings.builder()
            .put(INDEXING_SETTINGS)
            .put("index.number_of_shards", numberOfShards)
            .build();
  }

  /**
   * Creates a search settings instance based on default values.
   */
  private static Settings searchSettings() {
    return Settings.builder().put(SEARCH_SETTINGS).build();
  }

  /**
   * Creates an Elasticsearch index using indexing settings and the configuration class.
   */
  @SneakyThrows
  public void createIndex() {
    createIndex(configuration.getIndex(),
                readFileToString(configuration.getMappingsFile()),
                mergeSettings(readSettings(configuration.getSettingsFile()),
                              indexingSettings(configuration.getNumberOfShards())));
  }

  /**
   * Creates an Elasticsearch index using the idxName, mapping source and settings provided.
   */
  @SneakyThrows
  private void createIndex(String idxName, String source, Settings settings) {
    LOG.info("Creating Elasticsearch Index {} ", idxName);
    //create ES idx if it doesn't exist
    if (esClient.indices().exists(new GetIndexRequest(idxName), RequestOptions.DEFAULT)) {
      esClient.indices().delete(new DeleteIndexRequest(idxName), RequestOptions.DEFAULT);
    }
    CreateIndexRequest createIndexRequest =
      new CreateIndexRequest(idxName).mapping(source, XContentType.JSON).settings(settings);
    CreateIndexResponse response = esClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    LOG.info("Elasticsearch Index creation result {} ", response);
  }

  /**
   * Extracts the host configuration into an array of HttpHost.
   */
  private static HttpHost[] getHosts(ElasticsearchConfiguration configuration) {
    String[] hostsUrl = configuration.getHost().split(",");
    HttpHost[] hosts = new HttpHost[hostsUrl.length];
    int i = 0;
    for (String host : hostsUrl) {
      try {
        URL url = new URL(host);
        hosts[i] = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        i++;
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      }
    }
    return hosts;
  }

  /**
   * Creates ElasticSearch client using default connection settings.
   */
  public RestHighLevelClient buildEsClient(ElasticsearchConfiguration configuration) {
    HttpHost[] hosts = getHosts(configuration);
    return new RestHighLevelClient(RestClient.builder(hosts)
                                     .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(
                                         configuration.getConnectionTimeOut())
                                       .setSocketTimeout(configuration.getSocketTimeOut())
                                       .setConnectionRequestTimeout(configuration.getConnectionRequestTimeOut()))
                                     .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS));
  }

  /**
   * Creates ElasticSearch client using default connection settings.
   */
  public static RestClient buildRestEsClient(ElasticsearchConfiguration configuration) {
    HttpHost[] hosts = getHosts(configuration);
    return RestClient.builder(hosts).setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(
                                         configuration.getConnectionTimeOut())
                                       .setSocketTimeout(configuration.getSocketTimeOut())
                                       .setConnectionRequestTimeout(configuration.getConnectionRequestTimeOut()))
                                     .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS).build();
  }

  /**
   * Updates index to use the search settings.
   */
  public void updateIndexSearchSettings() {
    updateIndexSettings(configuration.getIndex(), ElasticsearchClient.searchSettings());
  }

  /**
   * Updates an index settings using the provided settings argument.
   */
  @SneakyThrows
  public void updateIndexSettings(String index, Settings settings) {
    //Update setting to search production
    esClient.indices()
      .putSettings(new UpdateSettingsRequest().indices(index).settings(settings), RequestOptions.DEFAULT);
  }

  /**
   * Updates the Elasticsearch setting to use search settings and swap the alias to used the recently created index.
   */
  public void goLive() {
    LOG.info("Swapping index and setting search settings.");
    updateIndexSearchSettings();
    swapIndexToAlias();
    LOG.info("Index and alias swapped and live now.");
  }

  /**
   * Swap the alias provide in the configuration instance to use the index created or provided in configuration class.
   */
  private void swapIndexToAlias() {
    swapIndexToAlias(configuration.getAlias(), configuration.getIndex());
  }

  /**
   * This method delete all the indexes associated to the alias and associates the alias to toIdx.
   */
  @SneakyThrows
  public void swapIndexToAlias(String alias, String toIdx) {
    //Sets the idx alias
    GetAliasesResponse aliasesGetResponse =
      esClient.indices().getAlias(new GetAliasesRequest().aliases(alias), RequestOptions.DEFAULT);

    IndicesAliasesRequest swapAliasesRequest = new IndicesAliasesRequest();
    swapAliasesRequest.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD).index(
      toIdx).writeIndex(true).aliases(alias));

    //add the removal all existing indexes of that alias
    aliasesGetResponse.getAliases()
      .keySet()
      .forEach(idx -> swapAliasesRequest.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE_INDEX).index(
        idx)));

    //Execute all the alias operations in a single/atomic call
    esClient.indices().updateAliases(swapAliasesRequest, RequestOptions.DEFAULT);
  }

  /**
   * Generate an index name using configuration.alias + "_" + current time in milliseconds.
   */
  private static String getEsIndexingIdxName(String alias) {
    return alias + "_" + new Date().getTime();
  }

  /**
   * Reads the content of a file into a String variable.
   */
  private static String readFileToString(String filePath) throws IOException {
    return IOUtils.toString(EsBackfill.class.getResourceAsStream(filePath), StandardCharsets.UTF_8.toString());
  }

  /**
   * Read the content of input file and loads it into Settings object.
   */
  private static Settings readSettings(String filePath) throws IOException {
    return Settings.builder().loadFromSource(readFileToString(filePath), XContentType.JSON).build();
  }

  /**
   * Merge two instances of settings objects.
   */
  private static Settings mergeSettings(Settings settings1, Settings settings2) {
    return Settings.builder().put(settings1).put(settings2).build();
  }
}
