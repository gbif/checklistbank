
CREATE TABLE backbone (
 id int PRIMARY KEY,
 parent_key int,
 basionym_key int,
 is_synonym boolean,
 status text,
 rank text,
 nom_status text[],
 constituent_key text,
 origin text,
 source_taxon_key int,

 kingdom_key int,
 phylum_key int,
 class_key int,
 order_key int,
 family_key int,
 genus_key int,
 species_key int,

 name_id int,
 scientific_name text,
 canonical_name text,
 genus_or_above text,
 specific_epithet text,
 infra_specific_epithet text,
 notho_type text,
 authorship text,
 year text,
 bracket_authorship text,
 bracket_year text,
 
 name_published_in text,
 issues text[]
)
