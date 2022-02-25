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

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * Elasticsearch Checklistbank indexer.
 * Creates a new Index using as input Avro files puts that index as the only to back the provided alias name.
 */
public class EsBackfill {

  private static final Logger LOG = LoggerFactory.getLogger(EsBackfill.class);

  /** Entry point for execution. Commandline arguments are: 0: required path to property file */
  public static void main(String[] args) throws Exception {

    EsBackfillConfiguration configuration = readConfiguration(args);

    ElasticsearchClient elasticsearchClient = new ElasticsearchClient(configuration.getElasticsearch());

    //Create Index
    elasticsearchClient.createIndex();

    //Reads the Elasticsearch settings used by the Spark Elasticsearch library
    SparkConf conf = new SparkConf().setAppName("Checklistbank Elasticsearch Indexer")
      .set("es.nodes", configuration.getElasticsearch().getHost())
      .set("es.resource", configuration.getElasticsearch().getIndex());

    //Loads the Avro name usages
    JavaSparkContext sc = new JavaSparkContext(conf);
    SQLContext sqlContext = new SQLContext(sc);
    JavaRDD<String> usages = sqlContext.read()
      .format("com.databricks.spark.avro")
      .load(configuration.getSourceDirectory())
      .toJSON()
      .toJavaRDD()
      .repartition(configuration.getIndexingPartitions());  //partitions the input data

    //Loads JSON usages into Elasticsearch
    JavaEsSpark.saveJsonToEs(usages,
                             configuration.getElasticsearch().getIndex(),
                             ImmutableMap.of("es.mapping.id", "key"));
    // This statement is used because the Guice container is not stopped inside the threadpool.
    LOG.info("Indexing done. Time to exit.");

    //Stop Spark context
    sc.stop();

    //Make index live
    elasticsearchClient.goLive();

    System.exit(0);
  }

  /**
   * Reads the YAML configuration file into a BackfillConfiguration instance.
   */
  private static EsBackfillConfiguration readConfiguration(String[] args) {
      if (args.length == 0) {
        throw new IllegalArgumentException("Configuration file must be provided as argument");
      }
      return EsBackfillConfiguration.loadFromFile(args[0]);
  }

}
