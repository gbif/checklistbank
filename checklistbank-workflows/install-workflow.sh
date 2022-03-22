#!/usr/bin/env bash
#exit on any failure
set -e

P=$1
TOKEN=$2

echo "Getting latest checklistbank-index-builder workflow properties file from github"
curl -s -H "Authorization: token $TOKEN" -H 'Accept: application/vnd.github.v3.raw' -O -L https://api.github.com/repos/gbif/gbif-configuration/contents/checklistbank-index-builder/profiles.xml
curl -s -H "Authorization: token $TOKEN" -H 'Accept: application/vnd.github.v3.raw' -O -L https://api.github.com/repos/gbif/gbif-configuration/contents/checklistbank-index-builder/$P.properties

#extract the oozie.url value from the properties file
oozie_url=`cat $P.properties| grep "oozie.url" | cut -d'=' -f2-`

echo "Assembling jar for $ENV"

mvn --settings profiles.xml -Poozie,$P clean package -DskipTests assembly:single
cp target/checklistbank-workflows.jar target/oozie-workflow/lib

if hdfs dfs -test -d /checklistbank-index-builder-$P/; then
   echo "Removing content of current Oozie workflow directory"
   sudo -u hdfs hdfs dfs -rm -f -r /checklistbank-index-builder-$P/*
else
   echo "Creating workflow directory"
   sudo -u hdfs hdfs dfs -mkdir /checklistbank-index-builder-$P/
fi
echo "Copying new Oozie workflow to HDFS"
sudo -u hdfs hdfs dfs -copyFromLocal target/oozie-workflow/* /checklistbank-index-builder-$P/
sudo -u hdfs hdfs dfs -copyFromLocal $P.properties /checklistbank-index-builder-$P/lib/

echo "Executing Oozie workflow"
sudo -E -u hdfs oozie job --oozie ${oozie_url} -config $P.properties -run

