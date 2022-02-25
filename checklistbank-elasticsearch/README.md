# Elasticsearch Checklistbank

This project aims to be library that can be used to access and build Elasticsearch indexes.


## To build the project

It is configured to build a shaded, executable jar which will build a full new Elasticsearch index from the avro files. 
The following Maven command will produce a shaded jar in your target dir named checklistbank-elasticsearch.jar.

```
mvn install package
```


## Building the Elasticsearch Index

This project provides the Checklistbank Elasticsearch index.

To run the indexer using existing Avro files run the command spark-submit command following this example:

```
spark2-submit --class org.gbif.checklistbank.elasticsearch.EsBackfill --conf spark.dynamicAllocation.enabled=false --master yarn --executor-memory 6G --executor-cores 2 --num-executors 3 checklistbank-elasticsearch.jar application.yml
```

### Configuration
The Spark job receives a configuration as argument which should include the following elements:

```
indexingPartitions: 80
sourceDirectory: "hdfs://data/name_usage"
elasticsearch:
  host: "http://es1:9200/,http://es2:9200/"
  connectionTimeOut: 60000
  socketTimeOut: 60000
  connectionRequestTimeOut: 120000
  alias: "species"
  numberOfShards: 12
  mappingsFile: "/elasticsearch/species-schema-mapping.json"
  settingsFile: "/elasticsearch/species-schema-settings.json"
```

### Cluster settings
Depending on the size of the cluster used, memory breakers might need to be disabled, the following setting has to be applied to each node followed by a restart.

```
indices.breaker.total.use_real_memory: false
```

Once the index is created, this setting should be reverted to its default value and nodes need to be restarted.

