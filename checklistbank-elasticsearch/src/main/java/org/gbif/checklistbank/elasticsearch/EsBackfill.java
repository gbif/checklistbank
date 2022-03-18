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

import org.gbif.common.search.es.EsClient;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettingBlocks;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import co.elastic.clients.elasticsearch.indices.Translog;
import com.google.common.collect.ImmutableMap;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Elasticsearch Checklistbank indexer. Creates a new Index using as input Avro files puts that
 * index as the only to back the provided alias name.
 */
public class EsBackfill {

  private static final Logger LOG = LoggerFactory.getLogger(EsBackfill.class);

  /** Entry point for execution. Commandline arguments are: 0: required path to property file */
  public static void main(String[] args) throws Exception {

    EsBackfillConfiguration configuration = readConfiguration(args);

    EsClient.EsClientConfiguration esClientConfiguration = new EsClient.EsClientConfiguration();
    esClientConfiguration.setHosts(configuration.getElasticsearch().getHost());
    esClientConfiguration.setConnectionTimeOut(
        configuration.getElasticsearch().getConnectionTimeOut());
    esClientConfiguration.setSocketTimeOut(configuration.getElasticsearch().getSocketTimeOut());
    esClientConfiguration.setConnectionRequestTimeOut(
        configuration.getElasticsearch().getConnectionRequestTimeOut());

    co.elastic.clients.elasticsearch.ElasticsearchClient elasticsearchClient =
        EsClient.provideEsClient(esClientConfiguration);

    EsClient esClient = new EsClient(elasticsearchClient);

    // Create Index
    IndexSettings indexingSettings =
        new IndexSettings.Builder()
            .analysis(
                EsClient.deserializeFromFile(
                    configuration.getElasticsearch().getSettingsFile(),
                    IndexSettingsAnalysis._DESERIALIZER))
            .refreshInterval(new Time.Builder().time("-1").build())
            .numberOfReplicas("0")
            .translog(new Translog.Builder().durability("async").build())
            .blocks(new IndexSettingBlocks.Builder().readOnlyAllowDelete(null).build())
            .numberOfShards(String.valueOf(configuration.getElasticsearch().getNumberOfShards()))
            .build();

    TypeMapping typeMapping =
        EsClient.deserializeFromFile(
          configuration.getElasticsearch().getMappingsFile(), TypeMapping._DESERIALIZER);

    esClient.createIndex(
        configuration.getElasticsearch().getIndex(),
        typeMapping,
        indexingSettings);

    // Reads the Elasticsearch settings used by the Spark Elasticsearch library
    SparkConf conf =
        new SparkConf()
            .setAppName("Checklistbank Elasticsearch Indexer")
            .set("es.nodes", configuration.getElasticsearch().getHost())
            .set("es.resource", configuration.getElasticsearch().getIndex())
            .set("es.nodes.wan.only", "true");

    // Loads the Avro name usages
    JavaSparkContext sc = new JavaSparkContext(conf);
    SQLContext sqlContext = new SQLContext(sc);
    JavaRDD<String> usages =
        sqlContext
            .read()
            .format("com.databricks.spark.avro")
            .load(configuration.getSourceDirectory())
            .toJSON()
            .toJavaRDD()
            .repartition(configuration.getIndexingPartitions()); // partitions the input data

    // Loads JSON usages into Elasticsearch
    JavaEsSpark.saveJsonToEs(
        usages,
        configuration.getElasticsearch().getIndex(),
        ImmutableMap.of("es.mapping.id", "key"));
    // This statement is used because the Guice container is not stopped inside the threadpool.
    LOG.info("Indexing done. Time to exit.");

    // Stop Spark context
    sc.stop();

    // Make index live
    IndexSettings searchSettings =
      new IndexSettings.Builder()
        .refreshInterval(new Time.Builder().time("1s").build())
        .numberOfReplicas("1")
        .build();
    esClient.updateSettings(configuration.getElasticsearch().getIndex(), searchSettings);

    esClient.swapAlias(
        configuration.getElasticsearch().getAlias(), configuration.getElasticsearch().getIndex());

    System.exit(0);
  }

  /** Reads the YAML configuration file into a BackfillConfiguration instance. */
  private static EsBackfillConfiguration readConfiguration(String[] args) {
    if (args.length == 0) {
      throw new IllegalArgumentException("Configuration file must be provided as argument");
    }
    return EsBackfillConfiguration.loadFromFile(args[0]);
  }
}
