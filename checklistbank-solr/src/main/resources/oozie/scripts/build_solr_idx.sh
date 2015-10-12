SOLR_HOME=$1
AVRO_TABLE=$2
ZK_HOST=$3
OUT_HDFS_DIR=$4
SOLR_COLLECTION=$5
SOLR_COLLECTION_OPTS=$6
HADOOP_CLIENT_OPTSP=$7
MAP_RED_OPTS=$8
SOLR_HTTP_URL=$9

source $SOLR_HOME/server/scripts/map-reduce/set-map-reduce-classpath.sh
curl """$SOLR_HTTP_URL"/admin/collections?action=DELETE\&name="$SOLR_COLLECTION"""
$SOLR_HOME/server/scripts/cloud-scripts/zkcli.sh  -zkhost $ZK_HOST -cmd upconfig -confname $SOLR_COLLECTION -confdir solr/collection1/conf/
curl """$SOLR_HTTP_URL"/admin/collections?action=CREATE\&name="$SOLR_COLLECTION"\&"$SOLR_COLLECTION_OPTS"\&collection.configName="$SOLR_COLLECTION"""
export HADOOP_CLIENT_OPTS="$HADOOP_CLIENT_OPTSP $HADOOP_CLIENT_OPTS"
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:$SOLR_HOME/server/solr-webapp/webapp/WEB-INF/lib/jts-1.13.jar
export HADOOP_USER_CLASSPATH_FIRST=true
echo "HADOOP_CLASSPATH" $HADOOP_CLASSPATH
echo "HADOOP_LIBJAR" $HADOOP_LIBJAR
hadoop --config /etc/hadoop/conf/ jar $SOLR_HOME/dist/solr-map-reduce-*.jar $MAP_RED_OPTS -D mapreduce.job.user.classpath.first=true \
-libjars $HADOOP_LIBJAR --morphline-file avro_solr_occurrence_morphline.conf \
--zk-host $ZK_HOST --output-dir $OUT_HDFS_DIR \
--collection $SOLR_COLLECTION --log4j log4j.properties \
--verbose "$AVRO_TABLE" \
--go-live



