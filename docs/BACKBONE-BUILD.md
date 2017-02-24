# Rebuild Backbone & put it into production

## Build new backbone on UAT

### build neo4j backbone
 - stop all UAT clis as the backbone build needs 40g memory, growing with more sources
 - configure nub builder to use prod db & registry services for reading source data https://github.com/gbif/gbif-configuration/blob/master/cli/uat/config/clb-nub.yaml
 - run neo4j nub build via `./clb-buildnub.sh`

### import into postgres 
 - dump prod clb db and import into boma (for faster writes) as db nub_build
 - configure clb importer to use boma nub_build db https://github.com/gbif/gbif-configuration/blob/master/cli/uat/config/clb-importer.yaml
 - `./start-clb-importer.sh`
 - issue message to import neo4j backbone into postgres: `./clb-admin.sh IMPORT --nub`
 - once imported, dump boma nub_build and copy to uat_checklistbank
 - `./stop-clb.sh`

### rebuild nub lookup index on apps2.gbif-uat.org
 - `cd /usr/local/gbif/services/checklistbank-nub-ws/2.xx/1234567`
 - `./stop.sh`
 - `rm -Rf /usr/local/gbif/services/checklistbank-nub-ws/nub_idx`
 - `./start.sh`
 - wait until the logs indicate the index build was finished (~1h).
 
## Reprocess occurrences
Processing uses the geocode-ws and checklistbank-nub-ws.
Registry is not used as the dataset/organisation derived values are stored already when generating the verbatim view: http://api.gbif-uat.org/v1/occurrence/996799163/verbatim

### Stop PROD
 - Stop crawling & interpreting on PROD
 - Stop Oozie HDFS occurrence table coordinator

### Prepare UAT
 - copy hbase tables from prod
 - Deploy release version of occurrence-ws, checklistbank-ws & checklistbank-nub-ws to UAT
 - Change UAT processor-interpreted config to use new HBase table
 - Update occurrence and clb related clis
 - Delete rabbit MQ queues (on UAT)
 - Change UAT [processor-interpreted config](https://github.com/gbif/gbif-configuration/blob/master/cli/uat/config/processor-interpreted.yaml) to use new HBase table and 12(?) threads
 - start 3 processors with: `start-processor-interpreted.sh`

### Get all gbifids
 - export all ID from HBase to a file `ids`, store in `/mnt/auto/misc/<something>/`

### Issue interpretation messages
 - As "crap" user:
 - `cd /mnt/auto/misc/<something>/`
 - `split -l 1000000 ids ids-`
 - start a screen session: `screen -S interpret`
 - `cd ~/util; ./interpret-occurrences -e uat /mnt/auto/misc/<something>/ids-*`
 
## Rematch checklists
 - Change CLB matcher & analysis configs to use bomas nub_build:
   - https://github.com/gbif/gbif-configuration/blob/master/cli/uat/config/clb-matcher.yaml
   - https://github.com/gbif/gbif-configuration/blob/master/cli/uat/config/clb-analysis.yaml
 - `start-clb-matcher.sh`
 - `start-clb-analysis.sh`
 - rematch all: `./clb-admin.sh REMATCH`
 - when complete (no more rabbit messages in clb-matcher & clb-analysis):
 - boma: `pg_dump -U postgres nub_build | gzip > nub.sql.gz`
 - import into uat: 
   - scp file to camelot
   - `gunzip -c nub.sql.gz | psql -U postgres uat_checklistbank`


## Export backbone DwC-A
 - import from nub_build dump
 - export NUB to dwca: `./clb-admin.sh EXPORT --nub`
 - move to rs.gbif.org/datasets/backbone/2017-mm-dd

## Backfill Occurrence maps & cubes
See https://github.com/gbif/metrics/tree/master/cube

## Rebuild Occurrence HDFS and Solr
### Occurrence HDFS table 
Warning: Do NOT use prod, it needs to keep running.
 - Update the configurations for UAT using the new HBase table: https://github.com/gbif/gbif-configuration/blob/master/occurrence-download/profiles.xml
 - Install the workflow for UAT on the gateway https://github.com/gbif/occurrence/tree/master/occurrence-download 
 
### Occurrence Solr
Warning: For Solr, we use the prod config BUT the UAT hive database to have it ready with the right number of shards.
 - Update the configurations to use hive.db=uat https://github.com/gbif/gbif-configuration/blob/master/occurrence-index-builder/prod.properties
 - Install workflow for PROD on the gateway

## Final prod deployment
### Prepare CLB
 - import uat dump into prod: 
   - `gunzip -c nub.sql.gz | psql -U postgres prod_checklistbank`
   - psql -U clb prod_checklistbank -c 'VACUUM ANALYZE'
 - copy nub index from `apps2.gbif.uat.org/usr/local/gbif/services/checklistbank-nub-ws/nubidx` to `apps2.gbif.org/usr/local/gbif/services/checklistbank-nub-ws/nubidxNEW`
 - update webservice configs
   - https://github.com/gbif/gbif-configuration/blob/master/checklistbank-ws/prod/application.properties
   - https://github.com/gbif/gbif-configuration/blob/master/checklistbank-nub-ws/prod/application.properties
 - build new prod solr index without aliasing to prod_checklistbank
   
### Deploy CLB
 - prod deploy of checklistbank-nub-ws
 - swap nub index within checklistbank-nub-ws:
    - ./stop.sh
    - rm -Rf /usr/local/gbif/services/checklistbank-nub-ws/nubidx
    - mv /usr/local/gbif/services/checklistbank-nub-ws/nubidxNEW /usr/local/gbif/services/checklistbank-nub-ws/nubidx
    - ./start.sh
 - prod deploy of checklistbank-ws
 - alias to new solr collection
    - ./stop.sh
    - `curl -s "http://prodsolr01-vh.gbif.org:8983/solr/admin/collections?action=CREATEALIAS&name=prod_checklistbank&collections=prod_checklistbank_2017_02_22"`
    - ./start.sh

### Occ

tbd
