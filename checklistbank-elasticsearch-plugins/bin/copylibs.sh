#!/usr/bin/env bash

ENV=${1-'dev'}
VERSION=${2-'2.47-SNAPSHOT'}
JAR=checklistbank-solr-plugins-$VERSION-shaded.jar

if [ $ENV = 'prod' ] || [ $ENV = 'uat' ] ; then
  NODES="c4n1 c4n2 c4n3 c4n4 c4n5 c4n6 c4n7 c4n8 c4n9 c4n10 c4n11 c4n12 prodgateway-vh prodmaster1-vh prodmaster2-vh prodmaster3-vh uatsolr-vh uatsolr02-vh prodsolr01-vh prodsolr02-vh prodsolr03-vh prodsolr05-vh prodsolr06-vh prodsolr07-vh prodsolr08-vh prodsolr09-vh prodsolr10-vh"
elif [ $ENV = 'dev' ] ; then
  NODES="c1n1 c1n2 c1n3 c1n6 c2n1 c2n2 c2n3 bantha devgateway-vh"
else
  echo "Invalid environment $ENV. Please use on of dev/uat/prod"
  exit
fi

echo "copy $JAR to $ENV cluster ..."

echo $NODES;

for h in $NODES; do
 echo "copy to $h.gbif.org"
 ssh root@$h.gbif.org "rm -f /opt/cloudera/parcels/SOLR5/server/solr-webapp/webapp/WEB-INF/lib/checklistbank-solr-plugins*"
 scp -p ../target/$JAR root@$h.gbif.org:/opt/cloudera/parcels/SOLR5/server/solr-webapp/webapp/WEB-INF/lib
done



