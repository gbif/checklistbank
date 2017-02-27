\copy (
SELECT u.id, u.parent_fk, u.basionym_fk, u.is_synonym, u.status, u.rank, u.nom_status, u.constituent_key, u.origin, u.source_taxon_key,
 u.kingdom_fk, u.phylum_fk, u.class_fk, u.order_fk, u.family_fk, u.genus_fk, u.species_fk,
 n.id as name_id, n.scientific_name, n.canonical_name, 
 n.genus_or_above, n.specific_epithet, n.infra_specific_epithet, n.notho_type, n.authorship, n.year, n.bracket_authorship, n.bracket_year, 
 cpi.citation as name_published_in, u.issues 
FROM name_usage u 
 JOIN name n ON u.name_fk=n.id 
 LEFT JOIN citation cpi ON u.name_published_in_fk=cpi.id 
WHERE u.dataset_key=nubKey() and u.deleted IS NULL
) to 'backbone.txt'
