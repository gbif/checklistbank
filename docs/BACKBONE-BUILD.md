# Rebuild Backbone & put it into production

## Build new backbone on UAT
 - stop all UAT clis as the backbone build needs 40g memory, growing with more sources
 - configure nub builder to use prod db & registry services for reading source data https://github.com/gbif/gbif-configuration/blob/master/cli/uat/config/clb-nub.yaml
 - run neo4j nub build via '''./clb-buildnub.sh'''
 - dump prod clb db and import into boma (for faster writes) as db nub_build
 - start-clb-importer.sh to import neo4j backbone into postgres
 - start-clb-analysis.sh

... to be continued

## Reprocess occurrences

### Prepare UAT
 - Copy registry and directory databases from prod to UAT
 - Deploy release version registry-ws and checklist-bank-ws to UAT
 - Change UAT processor-interpreted config to use new HBase table
 - Update occurrence and clb related clis
 - Delete rabbit MQ queues (on UAT)
 - Change UAT [processor-interpreted config](https://github.com/gbif/gbif-configuration/blob/master/cli/uat/config/processor-interpreted.yaml) to use new HBase table and 12(?) threads
 - start-processor-interpreted.sh

### Get all gbifids
 - export all ID from HBase to a file '''ids''', store in `/mnt/auto/misc/<something>/`

### Issue interpretation messages
 - As "crap" user:
 - `cd /mnt/auto/misc/<something>/`
 - `split -l 1000000 ids ids-`
 - start a screen session: `screen -S interpret`
 - `cd ~/util; ./interpret-occurrences -e uat /mnt/auto/misc/<something>/ids-*`
 
## Rematch checklists
 - Change CLB related configurations:
https://github.com/gbif/gbif-configuration/blob/master/cli/uat/config/clb-matcher.yaml

## Rebuild solr, maps & cubes
tbd

## Final prod deployment
tbd

