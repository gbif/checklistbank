# Checklist Darwin Core Archives
tbd.

## scientific names
scientific names are accepted in 3 different formats:

 1) entire name with authorship is given as dwc:scientificName: 

    scientificName: Abies alba var. alpina Mill.

 2) canonical & author

    scientificName: Abies alba var. alpina
    scientificNameAuthorship: Mill.

 3) atomized:

    genus: Abies
    specificEpithet: alba
    infraspecificEpithet: alpina
    scientificNameAuthorship: Mill.
    verbatimTaxonRank: var.
    taxonRank: variety

## rank
Try to use a controlled rank vocabulary or at least english rank names for dwc:taxonRank. For example the [GBIF rank enumeration](http://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/Rank.html)

## Name Usage
A name usage is a generalization of taxon or name. It is a record of a scientific name being used and it can be both a purely nomenclatoral name record or a fully fledged taxon concept.

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

 
## Verbatim vs ID terms
 - parentNameUsage & parentNameUsageID

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


