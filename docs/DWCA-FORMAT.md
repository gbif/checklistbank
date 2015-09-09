# Checklist Darwin Core Archives
Darwin Core archive can be used to publish taxonomic or nomenclatoral datasets which GBIF generically calls ___checklists___. GBIF has published various documents that guide publishers in crafting archives:

 - [Publishing Species Checklists, Best Practices](http://www.gbif.org/resources/2548)
 - [GBIF GNA Profile Reference Guide for Darwin Core Archive, Core Terms and Extensions](http://www.gbif.org/resources/2562)
 - [Publishing Species Checklists, Step-by-Step Guide](http://www.gbif.org/resources/2514)
 - [Best practice guide for compiling, maintaining, disseminating national species checklists](http://www.gbif.org/resources/2306)
 - [Best practice guidelines in the development and maintenance of regional marine species checklists](http://www.gbif.org/resources/2357)

Sadly they contradict each other in some areas and there is not a single, complete and up to date document available. This is maintained with the code and if in doubt should contain the most up to date answer.

In addition to GBIF the Catalog of Life has invested a lot of energy into developing a stricter format for publishing taxonomies as Darwin Core archives which is very useful and supported by GBIF indexing:

 - [i4Life Darwin Core Archive Profile](http://www.i4life.eu/i4lifewebsite/wp-content/uploads/2012/12/i4Life-DarwinCoreArchiveProfile.pdf)

 
# dwc:Taxon = NameUsage
The core entity of a checklist archive is a record with a rowType of dwc:Taxon. Don't be mislead by the term "taxon". A record of type "Taxon" is a _name usage_, a generalization of a taxon or a name record. It can be anything between a purely nomenclatoral name record or a fully fledged taxon concept.

## taxonID
dwc:taxonID is the primary key of a name usage record. It must be unique, but can be a local identifier only. It is the value that other foreign key terms refer to in order to establish relations between name usages. The following "foreign key" terms must reference an existing taxonID value in some record:

 - dwc:parentNameUsageID
 - dwc:acceptedNameUsageID
 - dwc:originalNameUsageID

## scientificNameID
The scientificNameID is used to declare a nomeclators identifier for a given name. It is _not_ used to establish relationships inside the dataset. Ideally dwc:scientificNameID should hold a resolvable, globally unique id, e.g. a DOI, LSID or URL.

## taxonConceptID & taxonAccordingTo
The taxonConceptID can be used to identifiy the exact concept of a name usage. This is mostly useful to assert that different name usages refer to the same concept albeit using a different name. It is best used with a globally unique identifier that can be referred to across various datasets. The taxonConceptID should _not_ be used to establish additional relations inside the dataset.

In order to label different taxon concepts based on the same name an additional reference is usually given (sec. / sensu). dwc:taxonAccordingTo should be used for this:

    taxonConceptID: doi:10.8912:BA6504FA-5EE6-4464-B96B-765891017D3D
    taxonAccordingTo: Frey 1989


## scientific names
scientific names are accepted in 3 different formats:

__1 - entire name with authorship__ 

given as dwc:scientificName

    scientificName: Abies alba var. alpina Mill.

__2 - canonical & author__

    scientificName: Abies alba var. alpina
    scientificNameAuthorship: Mill.

__3 - atomized__

    genus: Abies
    specificEpithet: alba
    infraspecificEpithet: alpina
    scientificNameAuthorship: Mill.
    verbatimTaxonRank: var.
    taxonRank: variety

## rank
Try to use a controlled rank vocabulary or at least english rank names for dwc:taxonRank. For example the [GBIF rank enumeration](http://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/Rank.html)

## Verbatim vs ID terms
Terms used to express relations in a dataset often exist in two forms in Darwin Core. One that takes a verbatim scientific name and one that takes an identifier (taxonID) of another record inside the dataset. If possible prefer to use the id based term which is less doubtful. The following term twins are specified in Darwin Core:

 - parentNameUsage & parentNameUsageID
 - acceptedNameUsage & acceptedNameUsageID
 - originalNameUsage & originalNameUsageID

## Classification
The classification can be published in 2 main ways. Prefer the normalized form over the denormalized if possible as it is more precise and offers a flexible hierarchy with any number of ranks.

### Normalized classification
Normalized checklists use a parent child relation to express the taxonomic classification.
Use dwc:parentNameUsageID to refer to the taxonID value of the next higher parent name usage.

    taxonID: 100
    parentNameUsageID:  
    scientificName: Chordata
    taxonRank: phylum

    taxonID: 102
    parentNameUsageID:  100
    scientificName: Vertebrata
    taxonRank: subphylum

### Denormalized classification
A denormalized name usage record specifies the major Linnean ranks as verbatim canonical names. The complete list of all supported ranks are:

 - dwc:kingdom
 - dwc:phylum
 - dwc:class
 - dwc:order
 - dwc:family
 - dwc:genus
 - dwc:subgenus

Example:

    scientificName: Sciurus vulgaris Linnaeus, 1758
    kingdom: Animalia
    phylum: Chordata
    class: Mammalia
    order: Rodentia
    family: Sciuridae
    genus: Sciurus
    subgenus: Sciurus
    

## Synonyms
Synonyms and accepted names are both expected to be found as core records in a dwc checklist archive. Synonyms can have the following properties:

 - dwc:acceptedNameUsageID to refer to the accepted name usage record. It must be an existing taxonID. It is legal for accepted taxa to refer to themselves
 - synonyms should not be chained, acceptedNameUsageID should point to an accepted taxon not another synonym
 - [taxonomicStatus](http://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/TaxonomicStatus.html) can indicate kind of synonym, e.g. homotypic
 - acceptedNameUsageID takes precedence over parentNameUsageID

Example:

    taxonID: 100
    scientificName: Cygnus columbianus (Ord, 1815)
    taxonomicStatus: accepted

    taxonID: 101
    scientificName: Anas columbianus Ord, 1815
    taxonomicStatus: homotypic synonym
    
## Basionym
The basionym (or protonym) of a name can be indicated either as a verbatim name or as a reference to another name usage record in the dataset. If you can prefer the reference approach using dwc:originalNameUsageID
 
Example:

    taxonID: 100
    scientificName: Cygnus columbianus (Ord, 1815)
    originalNameUsageID: 101
    
    taxonID: 101
    scientificName: Anas columbianus Ord, 1815

## namePublishedIn
 - full reference citation of the publication the name was originally first published in
 - use namePublishedInID to express a DOI or URL of the publication

## Dataset constituents
An full dataset can be made up of subdatasets, known as constituents to GBIF. For example the Catalog of Life contains several GSD (Global Species Dataset) subdatasets.

Each constituent can have its own EML metadata file which is referred to be the core record using the datasetNameID term. A corresponding EML file with the same name as the datasetNameID and a suffix ".xml" should live under a datasets subfolder inside the archive.

# Extensions supported by GBIF
See the [GBIF extension enumeration](http://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/Extension.html) with their corresponding rowType or the list of [GBIF checklist extension definitions](http://rs.gbif.org/extension/gbif/1.0/).

A detailed description of the usage for each extension coming soon.

## Description
    rowType = http://rs.gbif.org/terms/1.0/Description

For supported terms see [http://rs.gbif.org/extension/gbif/1.0/description.xml](http://rs.gbif.org/extension/gbif/1.0/description.xml)


## Distribution
    rowType = http://rs.gbif.org/terms/1.0/Distribution

For supported terms see [http://rs.gbif.org/extension/gbif/1.0/distribution.xml](http://rs.gbif.org/extension/gbif/1.0/distribution.xml)


## Identifer
    rowType = http://rs.gbif.org/terms/1.0/Identifier

For supported terms see [http://rs.gbif.org/extension/gbif/1.0/identifier.xml](http://rs.gbif.org/extension/gbif/1.0/identifier.xml)


## Multimedia
    rowType = http://rs.gbif.org/terms/1.0/Multimedia

For supported terms see [http://rs.gbif.org/extension/gbif/1.0/multimedia.xml](http://rs.gbif.org/extension/gbif/1.0/multimedia.xml)


## References
    rowType = http://rs.gbif.org/terms/1.0/Reference

For supported terms see [http://rs.gbif.org/extension/gbif/1.0/references.xml](http://rs.gbif.org/extension/gbif/1.0/references.xml)


## SpeciesProfile
    rowType = http://rs.gbif.org/terms/1.0/SpeciesProfile

For supported terms see [http://rs.gbif.org/extension/gbif/1.0/speciesprofile.xml](http://rs.gbif.org/extension/gbif/1.0/speciesprofile.xml)


## Typification
    rowType = http://rs.gbif.org/terms/1.0/TypesAndSpecimen

For supported terms see [http://rs.gbif.org/extension/gbif/1.0/typesandspecimen.xml](http://rs.gbif.org/extension/gbif/1.0/typesandspecimen.xml)


## VernacularName
    rowType = http://rs.gbif.org/terms/1.0/VernacularName

For supported terms see [http://rs.gbif.org/extension/gbif/1.0/vernacularname.xml](http://rs.gbif.org/extension/gbif/1.0/vernacularname.xml)
