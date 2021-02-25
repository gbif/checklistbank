# Rebuild Backbone & put it into production

## Build new backbone on backbonebuild-vh

### Build Neo4J backbone
 - Configure NUB builder to use prod DB & registry services for reading source data https://github.com/gbif/gbif-configuration/blob/master/cli/nub/config/clb-nub.yaml
 - Run Neo4J NUB build via `./clb-buildnub.sh`

### Import into Postgres
 - If this is an RC backbone build, stop production CLB CLIs.
 - Copy the production database to backbonebuild-vh (temporary VM with plenty of fast storage and RAM)
 - Dump prod CLB DB and import into PostgreSQL on backbonebuild-vh as DB clb
 - Configure CLB importer to use this DB https://github.com/gbif/gbif-configuration/blob/master/cli/nub/config/clb-importer.yaml
 - `./start-clb-importer.sh`
 - Issue message to import Neo4J backbone into postgres: `./clb-admin.sh IMPORT --nub`
 - Once imported, dump backbonebuild-vh clb DB and copy to pg1.gbif-uat.org uat_checklistbank
 - `./stop-clb.sh`

(TODO: Reinterpreting the IUCN checklist.)

### Rebuild NUB lookup index on ws.gbif-uat.org
 - `cd /usr/local/gbif/services/checklistbank-nub-ws/2.xx/1234567`
 - `./stop.sh`
 - `rm -Rf /usr/local/gbif/services/checklistbank-nub-ws/nub_idx`
 - `./start.sh`
 - wait until the logs indicate the index build was finished (~1h).

## Reprocess occurrences on UAT
Processing uses checklistbank-nub-ws, via the KVS cache.
 - Ensure release version of pipelines CLIs, checklistbank-ws & checklistbank-nub-ws are deployed in UAT
 - Stop all pipelines CLIs
 - Create a new `name_usage_YYYYMMDD_kv` table in HBase:
   ```
   create 'name_usage_20210225_kv', {NAME => 'v', BLOOMFILTER => 'ROW', DATA_BLOCK_ENCODING => 'FAST_DIFF', COMPRESSION => 'SNAPPY'},{SPLITS => ['1','2','3','4','5','6','7','8']}
   ```
 - See [pipelines documentation](https://github.com/gbif/pipelines/tree/dev/gbif/pipelines/interpretation-docs), but on UAT: `curl -Ss 'http://api.gbif-uat.org/v1/occurrence/search?limit=0&facet=datasetKey&facetLimit=50000' | jq '{ datasetsToInclude: [ .facets[].counts[].name ] }' | curl -iu username:password -X POST 'https://api.gbif-uat.org/v1/pipelines/history/run?steps=VERBATIM_TO_INTERPRETED&reason=New+backbone` (not bothering with a new index etc; it's OK if we have mixed data visible for a while on UAT).

## Rematch checklists
 - Change CLB matcher & analysis configs to use backbonebuild-vh's clb DB:
   - https://github.com/gbif/gbif-configuration/blob/master/cli/nub/config/clb-matcher.yaml
   - https://github.com/gbif/gbif-configuration/blob/master/cli/nub/config/clb-analysis.yaml
 - `start-clb-matcher.sh`
 - `start-clb-analysis.sh`
 - rematch CoL first: `./clb-admin.sh MATCH --col` so subsequent dataset analysis contains the right CoL percentage coverage
 - then rematch all the rest: `./clb-admin.sh REMATCH` this takes 1-2 days to complete!!!
 - when complete (no more rabbit messages in clb-matcher & clb-analysis):
 - backbonebuild-vh: `pg_dump -U postgres nub_build | gzip > nub.sql.gz`
 - import into UAT:
   - scp file to pg1.gbif-uat.org
   - `gunzip -c nub.sql.gz | psql -U postgres uat_checklistbank`

## Export backbone DwC-A
 - import from backbonebuild-vh clb DB dump
 - export NUB to DWCA: `./clb-admin.sh EXPORT --nub`
 - move to rs.gbif.org/datasets/backbone/yyyy-mm-dd

## Export backbone CSV
See http://rs.gbif.org/datasets/backbone/readme.html
 - export CSV from postgres: ` \copy (select * from v_backbone) to 'simple.txt'`
 - gzip and move to move to rs.gbif.org/datasets/backbone/yyyy-mm-dd
  
## Backfill Occurrence maps & cubes
See https://github.com/gbif/metrics/tree/master/cube

### Map builds
 - Update configurations if table names have changed.
 - Either way, either let the scheduler run its course, or start the jobs manually.

## Final prod deployment
### Prepare CLB
 - import UAT dump into prod:
   - `gunzip -c nub.sql.gz | psql -U postgres prod_checklistbank`
   - psql -U clb prod_checklistbank -c 'VACUUM ANALYZE'
 - copy nub index from `ws.gbif.uat.org/usr/local/gbif/services/checklistbank-nub-ws/nubidx` to `ws.gbif.org/usr/local/gbif/services/checklistbank-nub-ws/nubidxNEW`
 - update webservice configs
   - https://github.com/gbif/gbif-configuration/blob/master/checklistbank-ws/prod/application.properties
   - https://github.com/gbif/gbif-configuration/blob/master/checklistbank-nub-ws/prod/application.properties
 - reinterpret production occurences into a new index: https://github.com/gbif/pipelines/blob/dev/gbif/pipelines/interpretation-docs/full-reinterpretation-with-new-occurrence-index.md

### Deploy CLB
 - prod deploy of checklistbank-nub-ws
 - swap NUB index within checklistbank-nub-ws:
    - ./stop.sh
    - rm -Rf /usr/local/gbif/services/checklistbank-nub-ws/nubidx
    - mv /usr/local/gbif/services/checklistbank-nub-ws/nubidxNEW /usr/local/gbif/services/checklistbank-nub-ws/nubidx
    - ./start.sh
 - prod deploy of checklistbank-ws
 - alias to new solr collection
    - ./stop.sh
    - `curl -s "http://c5n1.gbif.org:8983/solr/admin/collections?action=CREATEALIAS&name=prod_checklistbank&collections=prod_checklistbank_2017_02_22"`
    - ./start.sh

### Deploy Occurrences WS
 - Deploy metric-ws, tile-server-ws and occurrence-ws
 - Disable previous HBase tables to ensure no configuration are still using them

### Deploy Occurrences CLI

### Deploy Dataset index coordinator job
The dataset index contains information about taxa used in occurrence and checklist datasets which is updated nightly by an Oozie coordinator job. That job needs to be redeployed with updated configs to use the latest clb and occ settings:
 - update https://github.com/gbif/gbif-configuration/blob/master/registry-index-builder/prod.properties
 - execute `c4gateway-vh:.../registry/registry-index-builder/install-coordinator.sh prod TOKEN`
