# Checklist Darwin Core Archives
tbd.

## Name Usage

## Primary Keys: taxonID, scientificNameID, taxonConceptID
 - taxonAccordingTo for concept sec reference
 
## Verbatim vs ID terms
 - parentNameUsage & parentNameUsageID

## Classification
 - prefer to use parentNameUsageID

## Synonyms
 - synonyms in the core with an acceptedNameUsageID
 - no chaining, acceptedNameUsageID should point to an accepted taxon not another synonym
 - taxonomicStatus can indicate kind of synonym, e.g. homotypic
 - acceptedNameUsage(ID) takes precedence over parentNameUsageID

## originalNameUsage (basionym)
 - prefer originalNameUsageID
 
## Controlled vocabularies
 - rank
 - taxonomic status

## namePublishedIn

