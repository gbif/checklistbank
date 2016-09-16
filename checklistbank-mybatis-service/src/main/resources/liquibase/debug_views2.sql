CREATE VIEW kname AS
  SELECT u.id, u.rank, n.scientific_name, u.is_synonym, u.status, u.origin, kn.scientific_name as kingdom, u.dataset_key
  FROM name_usage u
    JOIN name n on u.name_fk=n.id
    LEFT JOIN name_usage ku on u.kingdom_fk=ku.id
    LEFT JOIN name kn on ku.name_fk=kn.id;

CREATE VIEW nub as
SELECT u.id, n.id as nid, n.scientific_name, u.rank, u.status, u.origin, bu.id as basionym_id, b.scientific_name as basionym, pu.id as parent_id, p.scientific_name as parent, k.canonical_name as kingdom, f.canonical_name as family, u.deleted is not null as deleted, u.source_taxon_key as source_id, u.constituent_key, u.issues
FROM name_usage u JOIN name n on u.name_fk=n.id
  LEFT JOIN name_usage pu on u.parent_fk=pu.id LEFT JOIN name p on pu.name_fk=p.id
  LEFT JOIN name_usage ku on u.kingdom_fk=ku.id LEFT JOIN name k on ku.name_fk=k.id
  LEFT JOIN name_usage fu on u.family_fk=fu.id LEFT JOIN name f on fu.name_fk=f.id
  LEFT JOIN name_usage bu on u.basionym_fk=bu.id LEFT JOIN name b on bu.name_fk=b.id
WHERE u.dataset_key=nubKey();

CREATE VIEW nub_homonyms AS
  SELECT n1.canonical_name, u1.id as id1, u1.rank rank1, n1.scientific_name scientific_name1, u1.is_synonym is_synonym1, u1.status status1, u1.kingdom_fk kingdom1, u1.phylum_fk phylum1, u1.class_fk class1, u1.order_fk order1, u1.family_fk family1, u1.genus_fk genus1,  u2.id as id2, u2.rank rank2, n2.scientific_name scientific_name2, u2.is_synonym is_synonym2, u2.status status2, u2.kingdom_fk kingdom2, u2.phylum_fk phylum2, u2.class_fk class2, u2.order_fk order2, u2.family_fk family2, u2.genus_fk genus2
  FROM name_usage u1 JOIN name n1 on u1.name_fk=n1.id JOIN name n2 on n1.canonical_name=n2.canonical_name JOIN name_usage u2 on u2.name_fk=n2.id and u2.id!=u1.id
  WHERE u1.dataset_key=nubKey() and u2.dataset_key=nubKey() and u1.deleted is null and u2.deleted is null;
