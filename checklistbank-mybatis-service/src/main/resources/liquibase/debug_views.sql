CREATE VIEW kname AS
  SELECT u.id, u.rank, n.scientific_name, u.is_synonym, u.status, u.origin, kn.scientific_name as kingdom, u.dataset_key
  FROM name_usage u
    JOIN name n on u.name_fk=n.id
    LEFT JOIN name_usage ku on u.kingdom_fk=ku.id
    LEFT JOIN name kn on ku.name_fk=kn.id;
