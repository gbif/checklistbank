# Rebuild Backbone & Deploy it to Production

## Build new backbone on backbonebuild-vh
We use `backbonebuild-vh` with its local postgres database to build a new backbone and also run the matching and species API from there so we don't need to copy the database around to a different environment. The configs for `backbonebuild-vh` are located in the `nub` environment cli folder.
This setup skips UAT and reviews the backbone with the help of the review tools only.


### Copy ChecklistBank database from prod
Stop CLB CLIs on prod, dump the prod database and recreate it under the name `clb` on `backbonebuild-vh`.
All the following work is done as crap user on `backbonebuild-vh`, mostly in the bin directory:

### Build Neo4J backbone
 - Review configs at https://github.com/gbif/gbif-configuration/blob/master/cli/nub/config/
 - Run Neo4J NUB build via `./clb-buildnub.sh`
 - `./start-clb-importer.sh` to automatically insert the neo4j nub into postgres once the build is done
 - `./start-clb-analysis.sh` 
 - `./stop-clb.sh` once completed
 - archive nub.log

### Rebuild NUB lookup index
 - `stop-nub-ws.sh`
 - `rm -Rf ~/nub_idx`
 - `start-nub-ws.sh`
 - wait until the logs indicate the index build was finished (~1h).

### Rematch IUCN
The IUCN checklist is queried to assign a redlist status to occurrences.
It needs to be matched to the latest backbone before the occurrence processing can run.
 - `./start-clb-matcher.sh`
 - `./start-clb-analysis.sh` 
 - `./clb-admin MATCH --iucn` 


## Reprocess occurrences on UAT
Processing uses checklistbank-nub-ws, via the KVS cache.
 - Ensure release version of pipelines CLIs, checklistbank-ws & checklistbank-nub-ws are deployed in UAT
 - Stop all pipelines CLIs
 - Either `truncate_preserve 'name_usage_kv'`, or create a new `name_usage_YYYYMMDD_kv` table in HBase:
   ```
   create 'name_usage_20210225_kv', {NAME => 'v', BLOOMFILTER => 'ROW', DATA_BLOCK_ENCODING => 'FAST_DIFF', COMPRESSION => 'SNAPPY'},{SPLITS => ['1','2','3','4','5','6','7','8']}
   ```
 - See [pipelines documentation](https://github.com/gbif/pipelines/tree/dev/gbif/pipelines/interpretation-docs), but on UAT:
   ```
   curl -Ss 'http://api.gbif-uat.org/v1/occurrence/search?limit=0&facet=datasetKey&facetLimit=50000' | \
   jq '{ datasetsToInclude: [ .facets[].counts[].name ] }' | \
   curl -u user:password -H 'Content-Type: application/json' -X POST -d@- \
       'https://api.gbif-uat.org/v1/pipelines/history/run?steps=VERBATIM_TO_INTERPRETED&useLastSuccessful=true&reason=New+backbone'
   ````
   (this doesn't reuses the existing ES index, see the pipelines documentation for alternatives).

## Rematch checklists
Also do this on `backbonebuild-vh` as crap user.
If not already running start the matcher cli:
 - `start-clb-matcher.sh`
 - `start-clb-analysis.sh`
 - rematch CoL first: `./clb-admin.sh MATCH --col` so subsequent dataset analysis contains the right CoL percentage coverage
 - then rematch all the rest: `./clb-admin.sh REMATCH` this takes 1-2 days to complete!!!

## Export backbone CSV
See https://hosted-datasets.gbif.org/datasets/backbone/readme.html
- export CSV from postgres: ` \copy (select * from v_backbone) to 'simple.txt'`
- gzip and move to move to https://hosted-datasets.gbif.org/datasets/backbone/yyyy-mm-dd

## Backfill Occurrence maps & cubes
See https://github.com/gbif/metrics/tree/master/cube

### Map builds
 - Update configurations if table names have changed.
 - Either way, either let the scheduler run its course, or start the jobs manually.

## Final prod deployment

### Prepare CLB
 - backbonebuild-vh: `sudo -u postgres pg_dump -U postgres -Fc -Z1 clb > /var/lib/pgsql/11/backups/clb-2021-03-03.dump`
 - import dump into prod:
   - scp file to pg1.gbif.org
   - `sudo -u postgres pg_restore --clean --dbname prod_checklistbank2 --jobs 8 /var/lib/pgsql/11/backups/clb-2021-03-08.dump`
   - `sudo -u postgres psql -U clb prod_checklistbank -c 'VACUUM ANALYZE'
 - copy NUB index from `backbonebuild-vh:/home/crap/nubidx` to `ws.gbif.org:/usr/local/gbif/services/checklistbank-nub-ws/nubidxNEW`
 - update webservice configs
   - https://github.com/gbif/gbif-configuration/blob/master/checklistbank-ws/prod/application.properties
   - https://github.com/gbif/gbif-configuration/blob/master/checklistbank-nub-ws/prod/application.properties

### Deploy CLB WS
 - prod deploy of checklistbank-nub-ws
 - swap NUB index within checklistbank-nub-ws:
   ````
   systemctl stop checklistbank-nub-ws
   rm -Rf /usr/local/gbif/services/checklistbank-nub-ws/nubidx
   mv /usr/local/gbif/services/checklistbank-nub-ws/nubidxNEW /usr/local/gbif/services/checklistbank-nub-ws/nubidx
   systemctl start checklistbank-nub-ws
   ````
 - prod deploy of checklistbank-ws
 - alias to new solr collection
   ````
   systemctl stop checklistbank-ws
   curl -s "http://c5n1.gbif.org:8983/solr/admin/collections?action=CREATEALIAS&name=prod_checklistbank&collections=prod_checklistbank_2017_02_22"
   systemctl start checklistbank-ws
   ````

### Deploy other WS
 - Deploy metrics-ws, vectortile-server, occurrence-ws, registry-ws (as required)

### Deploy Dataset index coordinator job
The dataset index contains information about taxa used in occurrence and checklist datasets which is updated nightly by an Oozie coordinator job. That job needs to be redeployed with updated configs to use the latest clb and occ settings:
 - update https://github.com/gbif/gbif-configuration/blob/master/registry-index-builder/prod.properties
 - execute `c4gateway-vh:.../registry/registry-index-builder/install-coordinator.sh prod TOKEN`


## Update prod backbone metadata
This updates the prod registry! Only do this when we went live.
This needs to be done before the DWCA export though, as that should include the updated metadata
- `./admin.sh UPDATE_NUB_DATASET`

## Export backbone DwC-A
- import from backbonebuild-vh clb DB dump
- export NUB to DWCA: `./clb-admin.sh EXPORT --nub`
- move to https://hosted-datasets.gbif.org/datasets/backbone/yyyy-mm-dd
