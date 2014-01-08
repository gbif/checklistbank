\copy name (id,scientific_name,canonical_name,type,genus_or_above,infra_generic,specific_epithet,infra_specific_epithet,notho_type,rank_marker,authors_parsed,authorship,year,year_int,bracket_authorship,bracket_year,nom_status,modified) FROM 'name.txt'

\copy citation (id,citation) FROM 'citation.txt'

\copy name_usage (id,dataset_key,constituent_key,name_fk,rank,parent_fk,is_synonym,pp_synonym_fk,status,nom_status,basionym_fk,kingdom_fk,phylum_fk,class_fk,order_fk,family_fk,genus_fk,subgenus_fk,species_fk,name_published_in_fk,according_to_fk,citation_fk,origin,lft,rgt,remarks,modified) FROM 'name_usage.txt'

\copy name_usage_metrics (usage_fk,dataset_key,count_descendants,count_children,count_synonyms,count_p,count_c,count_o,count_f,count_g,count_sg,count_s,count_is,modified) FROM 'name_usage_metrics.txt'

\copy nub_rel (usage_fk,nub_fk,dataset_key) FROM 'nub_rel.txt'

\copy raw_usage (dataset_key,usage_fk,json,created) FROM 'raw_usage.txt'

\copy vernacular_name (dataset_key,usage_fk,name,language,sex,life_stage,temporal,country,locality,location_id) FROM 'vernacular_name.txt'

\copy description (dataset_key,usage_fk,description,type,language,source_fk,creator,contributor,license) FROM 'description.txt'

\copy species_info (dataset_key,usage_fk,marine,terrestrial,extinct,hybrid,living_period,age_in_days,size_in_millimeter,mass_in_gram,life_form,habitat) FROM 'species_info.txt'

\copy specimen (dataset_key,usage_fk,citation_fk,type_designated_by,scientific_name,rank,occurrence_id,institution_code,collection_code,catalog_number,locality,recorded_by,source_fk,verbatim_event_date,verbatim_label,verbatim_longitude,verbatim_latitude,type_designation_type,type_status) FROM 'specimen.txt'

\copy literature (dataset_key,usage_fk,citation_fk,type,remarks) FROM 'literature.txt'

\copy image (dataset_key,usage_fk,url,link,title,description,license,creator,created,publisher,rating) FROM 'image.txt'

\copy identifier (dataset_key,usage_fk,type,identifier,title,subject) FROM 'identifier.txt'

\copy distribution (dataset_key,usage_fk,country,locality,location_id,occurrence_status,threat_status,establishment_means,appendix_cites,start_day_of_year,end_day_of_year,start_year,end_year,source_fk,remarks) FROM 'distribution.txt'

-- finally rebuild the dataset metrics from scratch
INSERT INTO dataset_metrics (dataset_key,count_usages,count_synonyms,count_names,count_col,count_nub,  count_by_rank, count_by_kingdom, count_by_origin, count_extensions, count_vernacular_by_lang)
SELECT nu.dataset_key,
    count(*),
    (SELECT count(*) FROM name_usage WHERE dataset_key=nu.dataset_key AND is_synonym=true),
    (SELECT count(distinct(name_fk)) FROM name_usage WHERE dataset_key=nu.dataset_key),
    (SELECT count(distinct(u.id)) FROM name_usage u JOIN nub_rel rel ON rel.usage_fk=u.id JOIN nub_rel col ON col.nub_fk=rel.nub_fk AND col.dataset_key='7ddf754f-d193-4cc9-b351-99906754a03b' WHERE u.dataset_key=nu.dataset_key),
    (SELECT count(distinct(u.id)) FROM name_usage u JOIN nub_rel rel ON rel.usage_fk=u.id WHERE u.dataset_key=nu.dataset_key),

    (SELECT array_to_string(array_agg( key || '=>' || cnt), ', ')::hstore FROM
        (SELECT rank AS key, count(*)::text AS cnt FROM name_usage WHERE dataset_key=nu.dataset_key AND is_synonym=false GROUP BY rank) AS ranks
    ),
    (SELECT array_to_string(array_agg( key || '=>' || cnt), ', ')::hstore FROM
        (SELECT CASE nub.kingdom_fk
            WHEN 0 THEN 'INCERTAE_SEDIS'
            WHEN 1 THEN 'ANIMALIA'
            WHEN 2 THEN 'ARCHAEA'
            WHEN 3 THEN 'BACTERIA'
            WHEN 4 THEN 'CHROMISTA'
            WHEN 5 THEN 'FUNGI'
            WHEN 6 THEN 'PLANTAE'
            WHEN 7 THEN 'PROTOZOA'
            WHEN 8 THEN 'VIRUSES'
            ELSE 'INCERTAE_SEDIS' END as key, count(*) as cnt
        FROM name_usage u JOIN nub_rel rel ON rel.usage_fk=u.id JOIN name_usage nub on nub.id=rel.nub_fk
          WHERE u.dataset_key=nu.dataset_key AND u.is_synonym=false
          GROUP BY nub.kingdom_fk) AS kingdoms
    ),
    (SELECT array_to_string(array_agg( key || '=>' || cnt), ', ')::hstore FROM
        (SELECT origin AS key, count(*)::text AS cnt FROM name_usage WHERE dataset_key=nu.dataset_key GROUP BY origin) AS origins
    ),
    (SELECT hstore('DESCRIPTION', (SELECT count(*)::text FROM description WHERE dataset_key=nu.dataset_key))
        || hstore('DISTRIBUTION', (SELECT count(*)::text FROM distribution WHERE dataset_key=nu.dataset_key))
        || hstore('IDENTIFIER', (SELECT count(*)::text FROM identifier WHERE dataset_key=nu.dataset_key))
        || hstore('IMAGE', (SELECT count(*)::text FROM image WHERE dataset_key=nu.dataset_key))
        || hstore('REFERENCE', (SELECT count(*)::text FROM literature WHERE dataset_key=nu.dataset_key))
        || hstore('SPECIES_PROFILE', (SELECT count(*)::text FROM species_info WHERE dataset_key=nu.dataset_key))
        || hstore('TYPES_AND_SPECIMEN', (SELECT count(*)::text FROM specimen WHERE dataset_key=nu.dataset_key))
        || hstore('VERNACULAR_NAME', (SELECT count(*)::text FROM vernacular_name WHERE dataset_key=nu.dataset_key))
    ),
    (SELECT array_to_string(array_agg( key || '=>' || cnt), ', ')::hstore FROM
        (SELECT language AS key, count(*)::text AS cnt FROM vernacular_name WHERE dataset_key=nu.dataset_key GROUP BY language) AS ranks
    )
FROM name_usage nu
GROUP BY nu.dataset_key;


-- calc extension and vernacular langs metrics once more for the nub specifically
UPDATE dataset_metrics set count_extensions =
( SELECT hstore('DESCRIPTION', (SELECT count(*)::text FROM description e JOIN nub_rel rel on rel.usage_fk=e.usage_fk))
    || hstore('DISTRIBUTION', (SELECT count(*)::text FROM distribution e JOIN nub_rel rel on rel.usage_fk=e.usage_fk))
    || hstore('IDENTIFIER', (SELECT count(*)::text FROM identifier e JOIN nub_rel rel on rel.usage_fk=e.usage_fk))
    || hstore('IMAGE', (SELECT count(*)::text FROM image e JOIN nub_rel rel on rel.usage_fk=e.usage_fk))
    || hstore('REFERENCE', (SELECT count(*)::text FROM literature  e JOIN nub_rel rel on rel.usage_fk=e.usage_fk))
    || hstore('SPECIES_PROFILE', (SELECT count(*)::text FROM species_info e JOIN nub_rel rel on rel.usage_fk=e.usage_fk))
    || hstore('TYPES_AND_SPECIMEN', (SELECT count(*)::text FROM specimen e JOIN nub_rel rel on rel.usage_fk=e.usage_fk))
    || hstore('VERNACULAR_NAME', (SELECT count(*)::text FROM vernacular_name e JOIN nub_rel rel on rel.usage_fk=e.usage_fk))
) WHERE dataset_key='d7dddbf4-2cf0-4f39-9b2a-bb099caae36c';

UPDATE dataset_metrics set count_vernacular_by_lang =
( SELECT array_to_string(array_agg( key || '=>' || cnt), ', ')::hstore FROM
    (SELECT language AS key, count(*)::text AS cnt FROM vernacular_name e JOIN nub_rel rel on rel.usage_fk=e.usage_fk GROUP BY language) AS names
) WHERE dataset_key='d7dddbf4-2cf0-4f39-9b2a-bb099caae36c';
