#exit on any failure
set -e

#!/bin/bash
P=$1
TOKEN=$2
#get the third parameter, if absent use a default of false
IS_SINGLE_SHARD=${3-'false'}

echo "Getting latest checklistbank-index-builder workflow properties file from github"
curl -s -H "Authorization: token $TOKEN" -H 'Accept: application/vnd.github.v3.raw' -O -L https://api.github.com/repos/gbif/gbif-configuration/contents/checklistbank-index-builder/$P.properties

if [ $IS_SINGLE_SHARD = true ] ; then
  echo -e "\nsolr.is_single_shard=true\n" >> $P.properties
else
  echo -e "\nsolr.is_single_shard=false\n" >> $P.properties
fi

#extract the oozie.url value from the properties file
oozie_url=`cat $P.properties| grep "oozie.url" | cut -d'=' -f2`

echo "Assembling jar for $ENV"

mvn -Poozie,$P clean package assembly:single
mvn -Psolr,$P package assembly:single

if hdfs dfs -test -d /checklistbank-index-builder-$P/; then
   echo "Removing content of current Oozie workflow directory"
   hdfs dfs -rm -f -r /checklistbank-index-builder-$P/*
else
   echo "Creating workflow directory"
   hdfs dfs -mkdir /checklistbank-index-builder-$P/
fi
echo "Copying new Oozie workflow to HDFS"
hdfs dfs -copyFromLocal target/oozie-workflow/* /checklistbank-index-builder-$P/
hdfs dfs -copyFromLocal $P.properties /checklistbank-index-builder-$P/lib/

echo "Executing Oozie workflow"
oozie job --oozie ${oozie_url} -config $P.properties -run

