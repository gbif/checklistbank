#!/usr/bin/env bash

echo "copy checklistbank-solr-plugins.jar to DEV cluster..."

#TODO: make the environment configurable
for h in c1n1 c1n2 c1n3 c1n6 c2n1 c2n2 c2n3 bantha devgateway-vh; do
 echo "copy to $h.gbif.org"
 scp -p ../target/checklistbank-solr-plugins.jar root@$h.gbif.org:/opt/cloudera/parcels/SOLR5/server/solr-webapp/webapp/WEB-INF/lib
done
