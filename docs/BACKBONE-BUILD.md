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

### rebuild nub lookup index on ws.gbif-uat.org
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
 - copy hbase tables from prod:

```
ssh mblissett@c5gateway-vh.gbif.org
sudo -u hbase hbase shell
> snapshot 'prod_d_occurrence', 'prod_d_occurrence_2018-02-02'
> snapshot 'prod_d_occurrence_counter', 'prod_d_occurrence_counter_2018-02-02'
> snapshot 'prod_d_occurrence_lookup', 'prod_d_occurrence_lookup_2018-02-02'
sudo -u hdfs hbase org.apache.hadoop.hbase.snapshot.ExportSnapshot -snapshot prod_d_occurrence_2018-02-02 -copy-to hdfs://c4master1-vh:8020/hbase -mappers 36
sudo -u hdfs hbase org.apache.hadoop.hbase.snapshot.ExportSnapshot -snapshot prod_d_occurrence_counter_2018-02-02 -copy-to hdfs://c4master1-vh:8020/hbase -mappers 9
sudo -u hdfs hbase org.apache.hadoop.hbase.snapshot.ExportSnapshot -snapshot prod_d_occurrence_lookup_2018-02-02 -copy-to hdfs://c4master1-vh:8020/hbase -mappers 9

ssh mblissett@c4gateway-vh.gbif.org
sudo -u hdfs hdfs dfs -chown -R hbase /hbase/.hbase-snapshot
sudo -u hdfs hdfs dfs -chown -R hbase '/hbase/archive/data/default/prod_d_*'
sudo -u hbase hbase shell
> disable 'uat_occurrence'
> disable 'uat_occurrence_counter'
> disable 'uat_occurrence_lookup'
> drop 'uat_occurrence'
> drop 'uat_occurrence_counter'
> drop 'uat_occurrence_lookup'
> clone_snapshot 'prod_d_occurrence_2018-02-02', 'uat_occurrence'
> clone_snapshot 'prod_d_occurrence_counter_2018-02-02', 'uat_occurrence_counter'
> clone_snapshot 'prod_d_occurrence_lookup_2018-02-02', 'uat_occurrence_lookup'
```

 - Deploy release version of occurrence-ws, checklistbank-ws & checklistbank-nub-ws to UAT
 - Change UAT processor-interpreted config to use new HBase table if necessary (`uat_occurrence` is used above)
 - Update occurrence and clb related clis
 - Delete rabbit MQ queues (on UAT)
 - Change UAT [processor-interpreted config](https://github.com/gbif/gbif-configuration/blob/master/cli/uat/config/processor-interpreted.yaml) to use new HBase table
 - Synchronize configuration and CLIs on C4 nodes if they are also to be used for processing (e.g. rsync uatcrawler1-vh:/home/crap to c4n{1..9}:/home/crap)
 - Start 3-4 processors on each node with: `start-processor-interpreted.sh`

### Get all gbifids
 - export all ID from HBase to a file `ids`, store in `/mnt/auto/misc/<something>/`

```
hive
> CREATE TABLE matt.occurrence_ids_2018-02-02
> ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' AS
> SELECT gbifid FROM uat.occurrence_hbase;

hdfs dfs -getmerge /user/hive/warehouse/matt.db/occurrence_ids_2018-02-02 occurrence_ids_2018-02-02.tsv
```

### Issue interpretation messages
 - As "crap" user on uatcrawler1-vh:
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
 - rematch CoL first: `./clb-admin.sh MATCH --col` so subsequent dataset analysis contains the right col perc. coverage
 - then rematch all the rest: `./clb-admin.sh REMATCH` this takes 1-2 days to complete!!!
 - when complete (no more rabbit messages in clb-matcher & clb-analysis):
 - boma: `pg_dump -U postgres nub_build | gzip > nub.sql.gz`
 - import into uat:
   - scp file to camelot
   - `gunzip -c nub.sql.gz | psql -U postgres uat_checklistbank`


## Export backbone DwC-A
 - import from nub_build dump
 - export NUB to dwca: `./clb-admin.sh EXPORT --nub`
 - move to rs.gbif.org/datasets/backbone/2017-mm-dd

## Export backbone CSV
See http://rs.gbif.org/datasets/backbone/readme.html
 - export csv from postgres: `\copy v_backbone to 'simple.txt'`
 - gzip and move to move to rs.gbif.org/datasets/backbone/2019-mm-dd
  
## Backfill Occurrence maps & cubes
See https://github.com/gbif/metrics/tree/master/cube

## Rebuild Occurrence HDFS and Solr
### Occurrence HDFS table
Warning: Do NOT use prod, it needs to keep running.
 - Update the configurations for UAT using the new HBase table: https://github.com/gbif/gbif-configuration/blob/master/occurrence-download/profiles.xml
 - Install the workflow for UAT on the gateway https://github.com/gbif/occurrence/tree/master/occurrence-download

### Occurrence Solr
Warning: For Solr, we use the prod config BUT the UAT hive database to have it ready with the right number of shards.
**Build the occurrence HDFS table first**
 - Update the configurations to use hive.db=uat https://github.com/gbif/gbif-configuration/blob/master/occurrence-index-builder/prod.properties
 - Install workflow for PROD on the gateway

### Map builds
 - Update configurations if table names have changed.
 - Either way, either let the scheduler run its course, or start the jobs manually.

## Final prod deployment
### Prepare CLB
 - import uat dump into prod:
   - `gunzip -c nub.sql.gz | psql -U postgres prod_checklistbank`
   - psql -U clb prod_checklistbank -c 'VACUUM ANALYZE'
 - copy nub index from `ws.gbif.uat.org/usr/local/gbif/services/checklistbank-nub-ws/nubidx` to `ws.gbif.org/usr/local/gbif/services/checklistbank-nub-ws/nubidxNEW`
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
    - `curl -s "http://c4n1.gbif.org:8983/solr/admin/collections?action=CREATEALIAS&name=prod_checklistbank&collections=prod_checklistbank_2017_02_22"`
    - ./start.sh

### Deploy Occurrences WS
 - Deploy metric-ws, tile-server-ws and occurrence-ws
 - Disable previous HBase tables to ensure no configuration are still using them

### Deploy Occurrences CLI

### Deploy Dataset index coordinator job
The dataset index contains information about taxa used in occurrence and checklist datasets which is updated nightly by an Oozie coordinator job. That job needs to be redeployed with updated configs to use the latest clb and occ settings:
 - update https://github.com/gbif/gbif-configuration/blob/master/registry-index-builder/prod.properties
 - execute `c4gateway-vh:.../registry/registry-index-builder/install-coordinator.sh prod TOKEN`
