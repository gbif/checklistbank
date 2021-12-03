# Build a new GBIF Backbone Taxonomy and deploy it to production

This procedure builds a new backbone on backbonebuild-vh and exposes the required webservices from there directly to run occurrence processing.
We skip reviewing on UAT but instead rely on the [taxonomy review tool](http://labs.gbif.org/taxonomy-review-v4).

## Build new backbone on backbonebuild-vh
We use `backbonebuild-vh` with its local postgres database to build a new backbone and also run the matching and species API from there so we don't need to copy the database around to a different environment. The configs for `backbonebuild-vh` are located in the `nub` environment cli folder.

This setup skips UAT and reviews the backbone with the help of the review tools only.


### Copy ChecklistBank database from prod
 - If this is a release (or release candidate) backbone build, stop production CLB CLIs.
 - Dump prod CLB DB and import into PostgreSQL on backbonebuild-vh as DB clb

All the following work is done as crap user on `backbonebuild-vh`, mostly in the bin directory:

### Build Neo4J backbone
 - Review configs at https://github.com/gbif/gbif-configuration/blob/master/cli/nub/config/
 - Run Neo4J NUB build via `./clb-buildnub.sh`
 - `./start-clb-importer` to automatically insert the neo4j nub into postgres once the build is done
 - `./start-clb-analysis`
 - `./stop-clb` once completed
 - archive `nub.log`

### Rebuild NUB lookup index & mapdb
 - `./stop-nub-ws`
 - `rm -Rf ~/nubidx`
 - `rm ~/nublookupDB`
 - `./start-nub-ws.sh`
 - wait until the logs indicate the index build was finished (~1h).

This exposes the nub matching service on port 9000:
http://backbonebuild-vh.gbif.org:9000/species/match?verbose=true&name=Abies

### Rematch checklists
With a new backbone all checklists must be rematched. The COL dataset must come first as the metrics make use of it for each other dataset.
The IUCN checklist is queried to assign a redlist status to occurrences, so occurrence processing must not start before it is rematched.

If not already running start the matcher cli:
- `./start-clb-matcher.sh`
- `./start-clb-analysis.sh`
- `./clb-admin MATCH --col` rematch CoL first so subsequent dataset analysis contains the right CoL percentage coverage
- `./clb-admin MATCH --iucn`
- then rematch all the rest: `./clb-admin REMATCH` this takes 10-20h to complete.


## Prod deployment

### Prepare CLB
 - backbonebuild-vh: `sudo -u postgres pg_dump -U postgres -Fc -Z1 clb > /var/lib/pgsql/11/backups/clb-2021-03-03.dump`
 - import dump into prod:
   - scp file to pg1.gbif.org
   - `sudo -u postgres pg_restore --clean --dbname prod_checklistbank2 --jobs 8 /var/lib/pgsql/11/backups/clb-2021-03-08.dump`
   - `sudo -u postgres psql -U clb prod_checklistbank -c 'VACUUM ANALYZE'
 - copy NUB index from `backbonebuild-vh:/home/crap/nubidx` to `ws.gbif.org:/usr/local/gbif/services/checklistbank-nub-ws/nubidxNEW`
 - copy nublookupDB index from `backbonebuild-vh:/home/crap/nublookupDB` to `ws.gbif.org:/usr/local/gbif/services/checklistbank-nub-ws/nublookupDBNEW`
 - update webservice configs
   - https://github.com/gbif/gbif-configuration/blob/master/checklistbank-ws/prod/application.properties
   - https://github.com/gbif/gbif-configuration/blob/master/checklistbank-nub-ws/prod/application.properties

### Rebuild solr index
We build a new solr index for prod using oozie and the backbonebuild database, but do not yet change the production alias which will be done when we deploy the services.

 - ssh c5gateway.gbif.org
  ````
cd /home/mdoering/checklistbank/checklistbank-solr
git pull
./install-workflow.sh prod token
  ````
 - 

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

### Update prod backbone metadata
This updates the prod registry! Only do this when we went live.
This needs to be done before the DWCA export though, as that should include the updated metadata
- `./admin.sh UPDATE_NUB_DATASET`

### Export backbone DwC-A
- import from backbonebuild-vh clb DB dump
- export NUB to DWCA: `./clb-admin.sh EXPORT --nub`
- move to https://hosted-datasets.gbif.org/datasets/backbone/yyyy-mm-dd

### Export backbone CSV
See https://hosted-datasets.gbif.org/datasets/backbone/readme.html
- export CSV from postgres: ` \copy (select * from v_backbone) to 'simple.txt'`
- gzip and move to move to https://hosted-datasets.gbif.org/datasets/backbone/yyyy-mm-dd
- explain the changes in a document at [backbone-builds](https://github.com/gbif/checklistbank/tree/master/docs/backbone-builds) for use in the [release notes](https://www.gbif.org/release-notes).

### Copy logs
See https://hosted-datasets.gbif.org/datasets/backbone/readme.html



# OCCURRENCES

## Reprocess occurrences
Processing uses checklistbank-nub-ws, via the KVS cache.
- Ensure release version of checklistbank-ws and checklistbank-nub-ws are deployed and running on backbonebuild-vh.
- Activate the new-backbone Varnish configuration, which directs requests from machines used to do reinterpretation to the backbonebuild-vh webservices.
- Ensure any occurrence ingestion has completed then stop crawler CLIs.
- `truncate_preserve 'name_usage_kv'`, or (if preferred) create a new `name_usage_YYYYMMDD_kv` table in HBase and update configurations to use this:
  ```
  create 'name_usage_20210225_kv', {NAME => 'v', BLOOMFILTER => 'ROW', DATA_BLOCK_ENCODING => 'FAST_DIFF', COMPRESSION => 'SNAPPY'},{SPLITS => ['1','2','3','4','5','6','7','8']}
  ```
- Use the [Pipelines reinterpretation pipeline](https://github.com/gbif/pipelines-jenkins-reinterpretation/) to run a reinterpretation, on the appropriate environment, choosing a new index if required.
  - In UAT, typically we would reinterpret in-place,
  - In production, we increment the index letter and build a new index, then swap over once it is completed.


## Backfill Occurrence maps
- Update configurations if table names have changed.
- Let the scheduler run its course, or (if preferred) start the jobs manually.

## Deploy other WS
- Deploy metrics-ws, vectortile-server, occurrence-ws, registry-ws (as required)
