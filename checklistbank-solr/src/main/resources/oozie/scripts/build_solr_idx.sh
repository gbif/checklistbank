SOLR_HOME=$1
AVRO_TABLE=$2
ZK_HOST=$3
OUT_HDFS_DIR=$4
SOLR_COLLECTION=$5
SOLR_COLLECTION_OPTS=$6
HADOOP_CLIENT_OPTSP=$7
MAP_RED_OPTS=$8
SOLR_HTTP_URL=$9
IS_SINGLE_SHARD=${10}

#sets up HADOOP_CLASSPATH and HADOOP_LIBJAR
source $SOLR_HOME/server/scripts/map-reduce/set-map-reduce-classpath.sh
export HADOOP_CLIENT_OPTS="$HADOOP_CLIENT_OPTSP $HADOOP_CLIENT_OPTS"
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:$SOLR_HOME/server/solr-webapp/webapp/WEB-INF/lib/checklistbank-solr-plugins-2.44-SNAPSHOT.jar
export HADOOP_USER_CLASSPATH_FIRST=true
echo "HADOOP_CLASSPATH" $HADOOP_CLASSPATH
echo "HADOOP_LIBJAR" $HADOOP_LIBJAR

if [ $IS_SINGLE_SHARD = true ] ; then

hadoop --config /etc/hadoop/conf/ jar $SOLR_HOME/dist/solr-map-reduce-*.jar $MAP_RED_OPTS -D mapreduce.job.user.classpath.first=true \
-libjars $HADOOP_LIBJAR --morphline-file avro_solr_morphline.conf --output-dir $OUT_HDFS_DIR \
--log4j log4j.properties --verbose --shards 1 --solr-home-dir solr/checklistbank/ "$AVRO_TABLE" \

else

PREV_COLLECTION="$SOLR_COLLECTION-prev"
NEW_COLLECTION="$SOLR_COLLECTION-new"

echo "Delete existing new collection $NEW_COLLECTION"
curl """$SOLR_HTTP_URL"/admin/collections?action=DELETE\&name="$NEW_COLLECTION"""

echo "Copy configs for $NEW_COLLECTION to ZK"
$SOLR_HOME/server/scripts/cloud-scripts/zkcli.sh  -zkhost $ZK_HOST -cmd upconfig -confname $NEW_COLLECTION -confdir solr/checklistbank/conf/

echo "Create new collection $NEW_COLLECTION"
curl """$SOLR_HTTP_URL"/admin/collections?action=CREATE\&name="$NEW_COLLECTION"\&"$SOLR_COLLECTION_OPTS"\&collection.configName="$NEW_COLLECTION"""

echo "Build $NEW_COLLECTION"
hadoop --config /etc/hadoop/conf/ jar $SOLR_HOME/dist/solr-map-reduce-*.jar $MAP_RED_OPTS -D mapreduce.job.user.classpath.first=true \
-libjars $HADOOP_LIBJAR --morphline-file avro_solr_morphline.conf \
--zk-host $ZK_HOST --output-dir $OUT_HDFS_DIR \
--collection $NEW_COLLECTION --log4j log4j.properties \
--verbose "$AVRO_TABLE" \
--go-live

echo "Delete previous collection $PREV_COLLECTION"
curl """$SOLR_HTTP_URL"/admin/collections?action=DELETE\&name="$PREV_COLLECTION"""

echo "Rename current collection $SOLR_COLLECTION to $PREV_COLLECTION"
curl """$SOLR_HTTP_URL"/admin/collections?action=RENAME\&core="$SOLR_COLLECTION"\&other="$PREV_COLLECTION"""

echo "Rename new collection to $SOLR_COLLECTION"
curl """$SOLR_HTTP_URL"/admin/collections?action=RENAME\&core="$NEW_COLLECTION"\&other="$SOLR_COLLECTION"""

fi
