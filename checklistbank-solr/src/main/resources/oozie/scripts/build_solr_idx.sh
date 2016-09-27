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
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:$SOLR_HOMEserver/solr-webapp/webapp/WEB-INF/lib/checklistbank-solr-plugins-2.44-SNAPSHOT.jar
export HADOOP_LIBJAR=$HADOOP_LIBJAR,file://$SOLR_HOMEserver/solr-webapp/webapp/WEB-INF/lib/checklistbank-solr-plugins-2.44-SNAPSHOT.jar
export HADOOP_USER_CLASSPATH_FIRST=true
echo "HADOOP_CLASSPATH" $HADOOP_CLASSPATH
echo "HADOOP_LIBJAR" $HADOOP_LIBJAR

if [ $IS_SINGLE_SHARD = true ] ; then

hadoop --config /etc/hadoop/conf/ jar $SOLR_HOME/dist/solr-map-reduce-*.jar $MAP_RED_OPTS -D mapreduce.job.user.classpath.first=true \
-libjars $HADOOP_LIBJAR --morphline-file avro_solr_morphline.conf --output-dir $OUT_HDFS_DIR \
--log4j log4j.properties --verbose --shards 1 --solr-home-dir solr/checklistbank/ "$AVRO_TABLE" \

else

SOLR_COLLECTION_TODAY=$SOLR_COLLECTION"_"$(date +"%Y_%m_%d")

echo "Delete todays collection $SOLR_COLLECTION_TODAY if existing"
curl -s """$SOLR_HTTP_URL"/admin/collections?action=DELETE\&name="$SOLR_COLLECTION_TODAY"""

echo "Copy configs for $SOLR_COLLECTION_TODAY to ZK"
$SOLR_HOME/server/scripts/cloud-scripts/zkcli.sh  -zkhost $ZK_HOST -cmd upconfig -confname $SOLR_COLLECTION_TODAY -confdir solr/checklistbank/conf/

echo "Create new collection $SOLR_COLLECTION_TODAY"
curl -s """$SOLR_HTTP_URL"/admin/collections?action=CREATE\&name="$SOLR_COLLECTION_TODAY"\&"$SOLR_COLLECTION_OPTS"\&collection.configName="$SOLR_COLLECTION_TODAY"""

echo "Build $SOLR_COLLECTION_TODAY"
hadoop --config /etc/hadoop/conf/ jar $SOLR_HOME/dist/solr-map-reduce-*.jar $MAP_RED_OPTS -D mapreduce.job.user.classpath.first=true \
-libjars $HADOOP_LIBJAR --morphline-file avro_solr_morphline.conf \
--zk-host $ZK_HOST --output-dir $OUT_HDFS_DIR \
--collection $SOLR_COLLECTION_TODAY --log4j log4j.properties \
--verbose "$AVRO_TABLE" \
--go-live

echo "Create alias $SOLR_COLLECTION for $SOLR_COLLECTION_TODAY"
curl -s """$SOLR_HTTP_URL"/admin/collections?action=CREATEALIAS\&name="$SOLR_COLLECTION"\&collections="$SOLR_COLLECTION_TODAY"""

fi
