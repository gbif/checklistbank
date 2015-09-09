# Backbone synonyms and the Occurrence API
Retrieving occurrences or showing occurrence metrics and maps for taxa requires GBIF to organize occurrences to the GBIF backbone to make use of a single and comparable classification and synonymy.
Expanding a search to also include occurrences which match a name regarded as some sort of synonym in the backbone is currently being done automatically. 
Similar child taxa are included in operations where a higher taxon is used as a filter, e.g. child species and infraspecific taxa are included for a genus search.

The following outputs of the GBIF API and portal are subject to a synonym expansion:
 - occurrence search with a taxonKey filter: http://api.gbif.org/v1/occurrence/search
 - occurrence downloads with a taxonKey filter
 - occurrence metrics with a taxonKey filter: http://api.gbif.org/v1/occurrence/count/schema
 - occurrence map tiles with a taxonKey filter
 - occurrence dataset inventories with a taxonKey filter: http://api.gbif.org/v1/occurrence/counts/datasets
 
# Synonym types

## Objective synonyms
Objective synonyms (also called nomenclatural or homotypical synonyms) refer to taxa with the same type and same rank (more or less the same taxon, although circumscription may vary, even widely). The symbol ≡ is often used to indicate this type of synonymy. As the type defines the circumscription, i.e. the concept of a taxon, two homotypical names cannot be accepted in the same taxonomy.

### Basionym / protonym
The basionym (a botanical term, zoologists prefer protonym) is the first name described for a given type. Subsequent recombinations based on that first name should use the original author in brackets. 

___Marptusa elegans___ Koch L., 1879 is the basionym of two accepted spider names in the current Catalog of Life. That is an obvious error as they share the same type and something we would like to avoid in the GBIF backbone:
 
	Abracadabrella elegans (Koch L., 1879)
	= Marptusa elegans Koch L., 1879

	Holoplatys elegans (Koch L., 1879)	
	= Marptusa elegans Koch L., 1879

### Junior synonyms / later homonym
Sometimes several names are published based on the same type, often not knowing about each other. The earliest name published validly takes precedence and subsequent names are junior synonyms. 


## Subjective synonyms
Subjective synonyms are heteroytpic synonyms that refer to different types with which different names are associated, but which the GBIF backbone considers to be the same taxon.

Heterotypic synonyms reflect a scientific opinion about distinctness of taxa. Heterotypic synonyms always have different sets of types. One of the taxa is the currently accepted taxon. Heterotypic synonyms concerns most often taxa at the terminal or subterminal levels.
 
## Misapplied names
You often find remarks that a name was consistently misapplied to some other taxon. This is often based on bad determination. Taxonomists list missaplied names sometimes in the synonymy of a name when the misapplication was in wide use in literature.


# Scientific actions
Various scientific actions or patterns can lead to the above synonyms. We try to discuss the most frequent cases here.

## Classification change
### Move species to different genus
___Pinus abies___ is recombined into genus Picea. In a taxonomy where the genus Pinus is accepted Picea abies then is a homotypical synonym of Pinus abies which is the basionym:

    Pinus abies L.  ≡  Picea abies (L.) H.Karst.


___Macrorhynchus humile___ was moved into the genus _Troximon_ which has a different gender. The species epithet must change accordingly and can thus change slightly:

    Macrorhynchus humilis Benth., 1849  ≡  Troximon humile (Benth.) A. Gray, 1883

### Promote infraspecific taxon to species rank
___Agoseris eastwoodiae___ Fedde, 1904
 ≡  _Agoseris apargioides_ var. _eastwoodiae_ (Fedde) Munz, 1958

TODO: is this a split/merge???

## Cicumscription change
If the circumscription of a taxon changes it can either shrink (a taxon is split into several) or grow (merged taxa). Unfortunately the name can stay the same (for parts) and thus represent different concepts. In general when looking at two concepts with the same name it is common to refer to the larger concept of a name as "sensu latu" or s.l., the smaller one "sensu strictu", s.str.

### Split taxa
 - Pro parte
 - sensu latu (s.l.) and sensu strictu (s.str.)
 - split out same rank or below

### Merge taxa
 - merge 2 species
 - merge genera or families

## Nomenclature change
### Replace illegal/unavailable name


# Complex synonymy use case

![Agoseris apargioides synonymy](Agoseris_apargioides.png)




# TODO

### Illegal names
Here is a case when a name was badly published and hence a later publication from 1891 takes precedence:

___Agoseris apargioides___ (Less.) Greene, 1891
 ≡ _Barkhausia lessingii_ Hook. & Arn., 1833 [nom. illeg.]

___Agoseris eastwoodiae___ Fedde, 1904
 ≡  _Agoseris maritima_ Eastw., 1903



### Replacement name
sometimes an older name cannot be used because another animal was described earlier with exactly the same name. For example, Lindholm discovered in 1913 that a generic name _Jelskia_ established by Bourguignat in 1877 for a European freshwater snail could not be used because another author Taczanowski had proposed the same name in 1871 for a spider. So Lindholm proposed a new replacement name _Borysthenia_. This is an objective synonym of _Jelskia_ Bourguignat, 1877 as it has the same type species.

_Jelskia_ Bourguignat, 1877  ≡  _Borysthenia_ Lindholm, 1913

About 1% of the currently used zoological names are considered to be new replacement names.



### A single species is split into multiple species for which subspecies were not already named:
Aus bus1 -> { Aus bus2, Aus cus }

 - Data under name Aus cus is unambiguous
 - Data under name Aus bus may relate to Aus bus2 or Aus cus
 - CoL data probably never tell us that Aus cus was once part of Aus bus1 so have false confidence for precision of data under name Aus bus
 - If a user searches for Aus cus, ideally they should be offered all data under name Aus cus and warned that some data organized under Aus bus2 may relate to this species
 - If a user searches for Aus bus, ideally they should be offered all data under name Aus bus and warned that some of these data may relate to other recently separated species

### A single species is split into multiple species for which subspecies were already named:
Aus bus1 -> { Aus bus2, Aus cus }

 - Aus bus bus -> Aus bus2
 - Aus bus cus -> Aus cus
 - All the same results as first case
 - Data under name Aus bus cus may formerly have appeared within pool of Aus bus1 but are not subject to the same ambiguity
 - Data under name Aus bus bus are the only records unambiguously relating to Aus bus2
 - If a user searches for Aus cus, ideally they should be offered all data under name Aus cus or Aus bus cus and warned that some data organized under Aus bus2 may relate to this species
 - If a user searches for Aus bus, ideally they should be 1) offered all data under name Aus bus bus as definitely associated with Aus bus and 2) offered all data under name Aus bus with warning that some of these data may relate to other recently separated species – this may imply some kind of taxonomicAssignmentConfidence flag to make this distinction possible
 - If a user searches for Aus bus cus, they should be advised that this is an unambiguous synonym for Aus cus and directed to that species
 - If a user searches for Aus bus bus, they should be advised that this is an unambiguous synonym for Aus bus2and directed to that species (with all the resulting warnings)

### Multiple species are merged into a single species:
{ Aus bus1, Aus cus } -> Aus bus2

 - Aus bus1 -> Aus bus bus
 - Aus cus -> Aus bus cus
 - There are no ambiguities at the species level
 - It may not be possible to discover all data associated with either subspecies since newer data may be under the name Aus bus and cannot necessarily be separated from older data relating to Aus bus1
 - If a user searches for Aus cus, ideally they should be warned that this is a partial synonym for Aus bus2 and unambiguous synonym for Aus bus cus and given the opportunity to choose between accessing the data organized under Aus bus2 or the data organized under Aus bus cus (with all the resulting warnings in each case)
 - If a user searches for Aus bus, ideally they should be offered all data for all these names
 - If a user searches for Aus bus cus, they should be offered all data under name Aus cus or Aus bus cus – there is no need for further warnings since we always have partially resolved data at higher ranks and so the standard expectation would be that some data under Aus bus, Aus, Aidae, Aiformes, etc. may relate to this subspecies
 - If a user searches for Aus bus bus, they should be offered all name under Aus bus bus – there is no need for further warnings since we always have partially resolved data at higher ranks and so the standard expectation would be that some data under Aus bus, Aus, Aidae, Aiformes, etc. may relate to this subspecies – in this case, there is loss of information since some of the older data under Aus bus will indeed unambiguously relate to Aus bus bus but we have no safe automated way to detect this


