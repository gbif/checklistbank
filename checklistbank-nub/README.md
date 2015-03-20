# The GBIF Backbone
A module with code dealing with the GBIF backbone:
http://www.gbif.org/dataset/d7dddbf4-2cf0-4f39-9b2a-bb099caae36c

# Backbone Matching
The species matching service at GBIF allows to lookup a matching taxon from the GBIF backbone given a scientific name and some optional verbatim classification. See /species/match service in our developer docs:

http://www.gbif.org/developer/species#searching

## Similarity
The interface StringSimilarity returns a distance between 2 strings measured as a double with 0 being no overlap and 100 being identical strings.

### DamerauLevenshtein similarity
### Jaro Winkler similarity


# Backbone Building
Code that regenerates a new backbone dataset ...

# Backbone Exports
Code that regenerates a new backbone dataset ...