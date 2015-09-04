# Checklist datasets
Checklist datasets are published as Darwin Core archives with a core rowType of [dwc:Taxon](http://rs.gbif.org/core/dwc_taxon.xml). Every core record has a unique taxonID and represents a "name usage", i.e. a taxon in the wider sense regardless whether it is accepted or a synonym. The scientific name of a taxon may not be unique and taxon concepts can be indicated by using the dwc:taxonAccordingTo term.

In contrast to occurrence records checklist are highly relational in nature. Nearly every record is related to other records in some way. This makes imports very vulnerable and a single bad record can break checklists considerably.

GBIF has published various documents that guide publishers in crafting archives:

 - [Publishing Species Checklists, Best Practices](http://www.gbif.org/resources/2548)
 - [GBIF GNA Profile Reference Guide for Darwin Core Archive, Core Terms and Extensions](http://www.gbif.org/resources/2562)
 - [Publishing Species Checklists, Step-by-Step Guide](http://www.gbif.org/resources/2514)
 - [Best practice guide for compiling, maintaining, disseminating national species checklists](http://www.gbif.org/resources/2306)
 - [Best practice guidelines in the development and maintenance of regional marine species checklists](http://www.gbif.org/resources/2357)

Sadly they contradict each other in some areas and there is not a single, complete and up to date document available. Hopefully we can get a full publishing guide into our portal at some stage deprecating these pdf resource.

In addition to GBIF documents the Catalog of Life has invested a lot of energy into developing a stricter format for publishing taxonomies as Darwin Core archives which is very useful:

 - [i4Life Darwin Core Archive Profile](http://www.i4life.eu/i4lifewebsite/wp-content/uploads/2012/12/i4Life-DarwinCoreArchiveProfile.pdf)


# Messaging flow
The checklist indexing will share the initial dwc archive crawling with occurrences and only deviate after the metadata sync has been done:

- registry console --> StartCrawlMessage
- *CrawlerCoordinatorService* --> CrawlJob (dwca zookeeper queue)
- *DownloaderService* --> DwcaDownloadFinishedMessage
- *DwcaValidator* --> DwcaValidationFinishedMessage
- *DwcaMetasyncService* --> DwcaMetasyncFinishedMessage

***Note***: The validator needs to be adjusted for checklist validation. Likely new message class properties to separate occurrence & checklist validity (hasValidChecklist & hasValidOccurrences) as we also have occurrences in checklist extensions.

## New checklistbank listeners
- *ChecklistNormalizer* --> ChecklistNormalizedMessage
- *ChecklistImporter* --> ChecklistImportedMessage
- *ChecklistNubMatch* --> ChecklistMatchedMessage
- *ChecklistMetrics* --> ChecklistAnalyzedMessage

***Note***: Last 2 steps kept separate so we can rematch records to a modified backbone easily every month without reimporting all datasets.

# ChecklistNormalizer
As checklist are very relational with all records being related to each other in some way, it is important to get those relations right. Darwin Core offers various ways of relating taxa (name usages) to each other. This normalizer will prepare the entire checklist data and produce a standard representation that can then easily be imported into the ChecklistBank Postgres database.

In previous checklistbank implementations the write access to the Postgres database has been the major bottleneck and imports only happened sequentially and slow. A major goal for having a normalizer is to keep all load of postgres until we have very clean data that is easy to import. This allows us also to run this step in parallel if we need to.

As the biggest challenge in normalization is the rewiring of taxon relations a graphdatabase like [Neo4j] (http://docs.neo4j.org/chunked/stable/) is the preferred candidate for maintaining the temporary data. As a graphdatabase neo4j also allows to easily iterate over checklist records in a taxonomic hierarchy for example to generate the nested set indices. Neo4j can be used in an embedded mode that spills to disk. 

In the future it should be fairly simple to optionally produce valid dwc archives as an alternative output format of this step. This might be very useful for the community as a service to simplify dealing with checklist archives which can be very different from publisher to publisher.

### Neo checklist graph model
Neo4j uses a property graph model which allows to attach any number of properties to both graph nodes and relations. The graph created from a checklist will be kept simple with name usages (records) being a node potentially having the following relationships:

 - PARENT_OF
 - SYNONYM_OF
 - BASIONYM_OF

***TODO: example graph diagram***

### Taxonomic Classification
The Darwin Core provides two options for publishing classifications or taxonomic hierarchies; normalized and denormalised. A single checklist might even provide both flavors in the same archive, e.g. Catalog of Life does this. 

### Denormalised Classification
The denormalized format provides the major Linnean ranks of the classification as verbatim scientific names, mostly without the authorship (a canonical name) as part of each record. Darwin Core provides terms for the following ranks:

 - kingdom
 - phylum
 - class
 - order
 - family
 - genus
 - subgenus

Not all taxa of the given classification might explicitly exist in the dataset as a record.
For example image a checklist of just one record like this:

taxonID | scientificName | taxonRank | family | order | kingdom
--- | --- | --- | --- | --- | ---
t1 | Abies alba Mill. | species | Pinaceae | Pinales | Plantae

As checklist bank stores the classification in a parent child relationship exclusively, we need to "materialize" implicit higher taxa so we ultimately end up with 4 records like this:

taxonID | parentID | scientificName | rank | origin
--- | --- | --- | --- | ---
t1 | 001 | Abies alba Mill. | species | SOURCE
001 | 002 | Pinaceae | family | DENORMED_CLASSIFICATION
002 | 003 | Pinales | order | DENORMED_CLASSIFICATION
003 |  | Plantae | kingdom | DENORMED_CLASSIFICATION

Note that we have created new synthetic taxonIDs and kept track of *why* a record exists in checklist bank with the help of the [origin enumeration](http://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/Origin.html).

### Normalized Classification
The normalized classification uses a parent-child relationship with the child record pointing to it's parent. Darwin Core provides a *dwc:parentNameUsage* term that holds the verbatim scientific name of the parent and also an ID based parentNameUsageID term referring to the taxonID of another record in the dataset. To establish a PARENT_OF relationship in neo4j we analyze both terms if existing and try the following options in this order:

 1. parentNameUsageID exists: lookup the taxonID of an existing record
 2. parentNameUsage exists: lookup record with scientificName=parentNameUsage
 3. parentNameUsage exists: lookup record with a canonicalName=parentNameUsage
 4. create a new "implicit" record with a synthetic ID and a scientificName=parentNameUsage and origin=VERBATIM_PARENT

### Original name
The basionym or original name of a taxon can be indicated by either a verbatim scientific name in originalNameUsage or a pointer to an existing taxonID using originalNameUsageID. In the case originalNameUsage is given but no originalNameUsageID we populate the id value by either:

 1. looking up the taxonID of an existing record with that scientificName
 2. create a new record with a synthetic ID with that scientific name and origin=VERBATIM_BASIONYM

### Synonymy
Similar to the original name the accepted name of synonyms can be given as a verbatim name in acceptedNameUsage or as a taxonID key in acceptedNameUsageID. If the id term is missing we populate it with an existing taxonID like above doing either:

 1. looking up the taxonID of an existing record with scientificName=acceptedNameUsage
 2. create a new record with a synthetic ID with scientificName=acceptedNameUsage and origin=VERBATIM_ACCEPTED

Some checklists chain synonym relations so that the acceptedNameUsage(ID) points to another synonym which in turn points to an accepted name. We flatten these chains so that every synonym always directly points to a truely accepted name.

Rarely the acceptedNameUsage(ID) values can be a concatenated list. In that case parse them create multiple synonym relations in neo. These are so called pro-parte synonyms, i.e. taxa split into several taxa at some point.

The taxonomicStatus of the synonym record should also be set to SYNONYM if it is not indicated already.


### ScientificName
The scientific name should be as complete as possible including infraspecific rank markers and authorship incl the year. For example these are complete names:

	Gerardia paupercula var. borealis (Pennell) Deam
	Braunsia pumatica van Achterberg & Long, 2010

If a scientificName was given and in addition also the authorship in dwc:scientificNameAuthorship make sure the same authorship is part of that name already. If its a canonical name only, append the authorship to the scientific name:

scientificName | scientificNameAuthorship
--- | ---
Gerardia paupercula var. borealis  | (Pennell) Deam


If the scientificName is given in atomic parts reassemble the full name string using the ParsedName class:

genus | specificEpitheton | (verbatim)taxonRank | infraspecificEpitheton | scientificNameAuthorship
--- | --- | --- | --- | ---
Gerardia | paupercula | var. | borealis  | (Pennell) Deam


### Verbatim data
CLB stores the full verbatim data of a record as a JSON serlialized StarRecord instance from the dwca-reader. Store the json in a neo node property VERBATIM_JSON.

### Checklist issues
To keep track of issues during normalization a new NameUsageIssue enumeration should be created similar to OccurrenceIssue. A neo list property ISSUE should keep track of anything that happened.

### Dataset constituents
Checklistbank stores the datasetKey and the constituents datasetKey for each record. We need to lookup the constituent keys from the registry by using dwc:datasetName & dwc:datasetNameID from the archive record. Instead of registry lookups we could also consider to pass a consituents map via the DwcaMetasyncFinishedMessage?

### Extra validations
A set of extra validations should be done and any arising issues flagged with data otherwise unchanged:

 - classification & rank: verify the records place in the taxonomic classification matches the given rank. Most ranks are ordered and it is wrong to place a family record above a phylum record.
 - scientificName & rank: Verify the given rank of a record is suitable for a given scientific name. For example a species binomial cannot be of rank genus.

### Name usage metrics
Checklistbank has a name_usage_metrics table that keeps track of various counts for any record. It also holds the nested set lft/rgt indices that depend on the taxonomic parent relations in a checklist. The normalizer will generate all counts and nested set indices for each name usage record:
 
* count_descendants: number of all usages included
* count_children: number of all accepted taxa being direct children
* count_synonyms: number of synonyms for this (accepted) taxon
* count_k: number of distinct accepted kingdoms included
* count_p: number of distinct accepted phyla included
* count_c: number of distinct accepted classes included
* count_o: number of distinct accepted orders included
* count_f: number of distinct accepted families included
* count_g: number of distinct accepted genera included
* count_sg: number of distinct accepted subgenera included
* count_s: number of distinct accepted species included
* count_is: number of distinct accepted infraspecific taxa included
 * lft / rgt index: [nested set index](http://en.wikipedia.org/wiki/Nested_set_model#The_technique) - can be generated by iterating in taxonomic order from root taxa depth first to lowest taxa

### Generic data cleaning
*All* value strings should undergo a basic cleaning doing the following:

 - detect null values and entirely remove the term from record for the following values: 
	 -  "" (empty string)
	 -  \N
	 -  \NULL
	 -  NULL
 - trim whitespace at both ends
 - also replace tabs and linebreaks ???

### Enumerations
Parse all term values that endup as [enumeration values in the GBIF API](http://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/package-summary.html). If no parser exists new ones have to be created:

 - CitesAppendix (distribution extension)
 - Country
 - EstablishmentMeans (distribution extension)
 - Language
 - LifeStage (distribution extension)
 - NomenclaturalCode
 - NomenclaturalStatus[]
 - Rank 
 - Sex 
 - TaxonomicStatus
 - ThreatStatus

## Archive Extensions
Checklistbank is able to store information from 6 optional [GNA extensions](http://rs.gbif.org/extension/gbif/1.0/) as described in the [GBIF GNA Profile Reference Guide](http://www.gbif.org/resources/2562). For every record the entire processed extension data should be stored in a single neo4j property "EXTENSION_DATA" similar to the `Map<Extension, List<Map<Term, String>>>` we use in the VerbatimOccurrence class.

### description
### distribution
### alternative identifier
### images
CLB still has a db structure aimed at the older simple images extension. It would be good to update to the new multimedia extension and being able to also index the simple images one.

### literature
### species info
### types and specimen
Currently CLB stores specimen information together with name types for higher taxa coming from a single [Types and Specimen extension](http://rs.gbif.org/extension/gbif/1.0/typesandspecimen.xml). As we do index occurrences in checklist extensions now this is a redundant feature of ChecklistBank and it would make sense to restrict the information in CLB about type names, i.e. species can act as the type for a genus and a genus can be the type for a family. Ideally we refactor the CLB data model to only store typeSpecies and typeGenus and ignore any specimen information.

# ChecklistImporter
The data importer reads the embedded neo4j database, iterates over all records using a taxonomic depth-first traversal starting with the root taxa sorted in alphabetic order. This should allow us to produce a single insert statement for the core name_usage record avoiding slow subsequent update queries. 

- keep stable usage ids by loading a sourceID->usageKey map on init if it was indexed before
- delete all data for the given datasetKey in name_usage, raw_usage and all extension tables
- issue new name_usage ids via the name_usage sequence in postgres for new sourceIDs
- scientificName values are normalized in the name table and we need to find a name id first by checking if a name with that identical string exists already or if we need to insert the new and parsed name
- similar all bibliographic citations are normalized and we need to insert/get the citationID from core publishedIn, taxonAccordingTo, bibliographicCitation values
- for pro parte synonyms with multiple accepted taxa duplicate the core synonym record and insert several identical usage records with pointing to a different accepted parent
- for synonyms without an accepted parent relationship insert a new `incertae sedis` usage record which then can be used to establish the higher classification parent link.
- insert verbatim json data into `raw_usage` table

All extension records are inserted one by one with:

- insert/get citationID from extension vernacular.source, literature.citation, description.source, distribution.source

# ChecklistNubMatch
ChecklistBank stores a mapping to the GBIF backbone for every record in all checklists. This drives a lot of things, e.g. the [Appears in section of the species pages](http://www.gbif.org/species/7222316#appearsin). Every time the backbone taxonomy changes we need to re-match all checklist records to the new backbone. This match is stored in a dedicated table `nub_rel` in postgres. This allows us to truncate the entire table quickly and reinsert new matches as we go without the need to issue slow update queries on the large, main name_usage table.

# ChecklistMetrics
At the very end calculate and store dataset metrics used in a [dataset stats page](http://www.gbif.org/dataset/d7dddbf4-2cf0-4f39-9b2a-bb099caae36c/stats). Up to now we generate counts stored in the dataset_metrics table via a set of sql statements which should be easy to adapt to the latest db structure.
