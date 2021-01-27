CREATE VIEW nub2 AS (
    SELECT u.id, u.parent_fk as pid, basionym_fk as bid, u.rank, u.status, is_synonym AS syn, u.origin, scientific_name, num_descendants AS size, kingdom_fk,family_fk,taxon_id as source_id, u.modified, u.deleted IS NOT NULL AS del, constituent_key, d.title
    FROM name_usage u JOIN name n ON u.name_fk=n.id LEFT JOIN dataset d ON d.key=constituent_key
    WHERE u.dataset_key=nubKey()
);

CREATE VIEW nubcl AS (
    SELECT u.id, u.rank, u.status, u.is_synonym AS syn, n.scientific_name, u.num_descendants AS size, u.parent_fk as pid, npa.scientific_name AS parent,
    nf.scientific_name AS family, no.scientific_name AS "order", nc.scientific_name AS "class", np.scientific_name AS phylum, nk.scientific_name AS kingdom
    FROM name_usage u JOIN name n ON u.name_fk=n.id
        LEFT JOIN name_usage upa ON upa.id=u.parent_fk JOIN name npa ON upa.name_fk=npa.id
        LEFT JOIN name_usage uf ON uf.id=u.family_fk JOIN name nf ON uf.name_fk=nf.id
        LEFT JOIN name_usage uo ON uo.id=u.order_fk JOIN name no ON uo.name_fk=no.id
        LEFT JOIN name_usage uc ON uc.id=u.class_fk JOIN name nc ON uc.name_fk=nc.id
        LEFT JOIN name_usage up ON up.id=u.phylum_fk JOIN name np ON up.name_fk=np.id
        LEFT JOIN name_usage uk ON uk.id=u.kingdom_fk JOIN name nk ON uk.name_fk=nk.id
    WHERE u.dataset_key=nubKey() and u.deleted IS NULL
);
