package org.gbif.checklistbank.elasticsearch;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.elasticsearch.indices.update_aliases.AddAction;
import co.elastic.clients.json.JsonpDeserializer;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.json.stream.JsonParser;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

;
/** Generic ElasticSearch wrapper client to encapsulate indexing and admin operations. */
@Component
public class EsClient implements Closeable {
  private static final JacksonJsonpMapper MAPPER = new JacksonJsonpMapper();
  @Data
  public static class EsClientConfiguration {
    private String hosts;
    private int connectionTimeOut;
    private int socketTimeOut;
    private int connectionRequestTimeOut;
  }
  private final ElasticsearchClient elasticsearchClient;
  @Autowired
  public EsClient(ElasticsearchClient elasticsearchClient) {
    this.elasticsearchClient = elasticsearchClient;
  }
  /**
   * Points the indexName to the alias, and deletes all the indices that were pointing to the alias.
   */
  public void swapAlias(String alias, String indexName) {
    try {
      GetAliasResponse getAliasesResponse =
          elasticsearchClient
              .indices()
              .getAlias(new GetAliasRequest.Builder().name(alias).index(indexName).allowNoIndices(true).build());
      Set<String> idxsToDelete = getAliasesResponse.result().keySet();
      elasticsearchClient.indices()
        .updateAliases(new UpdateAliasesRequest.Builder()
                         .actions(new Action.Builder().add(new AddAction.Builder()
                                                             .alias(alias)
                                                             .index(indexName)
                                                             .build())
                                    .build())
                         .build());
      if (!idxsToDelete.isEmpty()) {
        elasticsearchClient
          .indices()
          .delete(new DeleteIndexRequest.Builder()
                    .index(new ArrayList<>(idxsToDelete))
                    .build());
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
  public static <T> T deserializeFromFile(String settingsFile, JsonpDeserializer<T> deserializer) {
    try (final JsonParser jsonParser = MAPPER.jsonProvider().createParser(
      new InputStreamReader(
        new BufferedInputStream(
          EsClient.class.getClassLoader().getResourceAsStream(settingsFile))))) {
      return deserializer.deserialize(jsonParser, MAPPER);
    }
  }
  /** Creates a new index using the indexName, recordType and settings provided. */
  @SneakyThrows
  public void createIndex(
    String indexName,
    TypeMapping mappings,
    IndexSettings settings) {
    CreateIndexRequest.Builder createIndexRequest = new CreateIndexRequest.Builder();
    createIndexRequest
      .index(indexName)
      .settings(new IndexSettings.Builder().index(settings).build())
      .mappings(mappings);
    elasticsearchClient.indices().create(c -> c.index(indexName).settings(settings).mappings(mappings));
  }
  /** Updates the settings of an existing index. */
  public void updateSettings(String indexName, IndexSettings settings) {
    try {
      elasticsearchClient.indices().putSettings(s -> s.index(indexName).settings(settings));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
  /** Performs a ElasticSearch {@link BulkRequest}. */
  public BulkResponse bulk(BulkRequest bulkRequest) throws IOException {
    return elasticsearchClient.bulk(bulkRequest);
  }
  /** Creates ElasticSearch client using default connection settings. */
  public static ElasticsearchClient provideEsClient(EsClientConfiguration esClientConfiguration) {
    String[] hostsUrl = esClientConfiguration.hosts.split(",");
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
    return new ElasticsearchClient( new RestClientTransport(
      RestClient.builder(hosts)
        .setRequestConfigCallback(
          requestConfigBuilder ->
            requestConfigBuilder
              .setConnectTimeout(esClientConfiguration.getConnectionTimeOut())
              .setSocketTimeout(esClientConfiguration.getSocketTimeOut())
              .setConnectionRequestTimeout(
                esClientConfiguration.getConnectionRequestTimeOut()))
        .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS)
        .build(), new JacksonJsonpMapper()));
  }
  @Override
  public void close() {
    // shuts down the ES client
    if (Objects.nonNull(elasticsearchClient)) {
      try {
        elasticsearchClient._transport().close();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}