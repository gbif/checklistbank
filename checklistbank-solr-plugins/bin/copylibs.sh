#!/usr/bin/env bash


# DEV
#echo "copy checklistbank-solr-plugins.jar to DEV cluster..."
# for h in c1n1 c1n2 c1n3 c1n6 c2n1 c2n2 c2n3 bantha devgateway-vh; do

# PROD/UAT
echo "copy checklistbank-solr-plugins.jar to PROD cluster..."
for h in uatsolr-vh c4n1 c4n10 c4n11 c4n12 c4n2 c4n3 c4n4 c4n5 c4n6 c4n7 c4n8 c4n9 prodgateway-vh prodmaster1-vh prodmaster2-vh prodmaster3-vh prodsolr01-vh prodsolr02-vh prodsolr03-vh prodsolr05-vh prodsolr06-vh prodsolr07-vh prodsolr08-vh prodsolr09-vh prodsolr10-vh; do
 echo "copy to $h.gbif.org"
 scp -p ../target/checklistbank-solr-plugins.jar root@$h.gbif.org:/opt/cloudera/parcels/SOLR5/server/solr-webapp/webapp/WEB-INF/lib
done
