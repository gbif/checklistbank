# CLB Indexing with Neo4j
Evaluation of checklist bank indexing using Neo4j to read dwc archive, normalize the data and establish taxon relationships.


## Java VM
run with: -server -Xmx4g -XX:+UseConcMarkSweepGC



# Note on supported dwc archive formats

## scientific names
scientific names are accepted in 3 different formats:

 1) all in scientificName: Abies alba var. alpina Mill.

 2) canonical & author
scientificName= Abies alba var. alpina
scientificNameAuthorship = Mill.

 3) atomized:
genus(Name) = Abies
specificEpithet= alba
infraspecificEpithet= alpina
scientificNameAuthorship = Mill.
verbatimTaxonRank = var.
taxonRank=variety