/*
 update rank type according to extended enum
 https://github.com/gbif/gbif-api/commit/3826c67276b18dbaf1eae6b35e7334360bf3b9a5
*/
DROP VIEW kname;
DROP VIEW nub;
DROP VIEW nub_homonyms;
DROP FUNCTION groupInfraspecificRanks(rank);

UPDATE name_usage set rank = 'UNRANKED' WHERE rank = 'INFORMAL';
UPDATE typification set rank = 'UNRANKED' WHERE rank = 'INFORMAL';

-- add INFORMAL counts to UNRANKED in dataset metrics
-- liquibase specific note: use double question mark to become a literal ? in the final sql
UPDATE dataset_metrics set count_by_rank = count_by_rank - array['INFORMAL','UNRANKED'] || ('UNRANKED=>' || ((count_by_rank->'INFORMAL')::int + (count_by_rank->'UNRANKED')::int)::text)::hstore WHERE count_by_rank ?? 'INFORMAL';
ALTER TABLE name_usage ALTER COLUMN rank type text;
ALTER TABLE typification ALTER COLUMN rank type text;

DROP TYPE rank;
CREATE TYPE rank AS ENUM ('DOMAIN', 'SUPERKINGDOM', 'KINGDOM', 'SUBKINGDOM', 'INFRAKINGDOM', 'SUPERPHYLUM', 'PHYLUM', 'SUBPHYLUM', 'INFRAPHYLUM', 'SUPERCLASS', 'CLASS', 'SUBCLASS', 'INFRACLASS', 'PARVCLASS', 'SUPERLEGION', 'LEGION', 'SUBLEGION', 'INFRALEGION', 'SUPERCOHORT', 'COHORT', 'SUBCOHORT', 'INFRACOHORT', 'MAGNORDER', 'SUPERORDER', 'GRANDORDER', 'ORDER', 'SUBORDER', 'INFRAORDER', 'PARVORDER', 'SUPERFAMILY', 'FAMILY', 'SUBFAMILY', 'INFRAFAMILY', 'SUPERTRIBE', 'TRIBE', 'SUBTRIBE', 'INFRATRIBE', 'SUPRAGENERIC_NAME', 'GENUS', 'SUBGENUS', 'INFRAGENUS', 'SECTION', 'SUBSECTION', 'SERIES', 'SUBSERIES', 'INFRAGENERIC_NAME', 'SPECIES_AGGREGATE', 'SPECIES', 'INFRASPECIFIC_NAME', 'GREX', 'SUBSPECIES', 'CULTIVAR_GROUP', 'CONVARIETY', 'INFRASUBSPECIFIC_NAME', 'PROLES', 'RACE', 'NATIO', 'ABERRATION', 'MORPH', 'VARIETY', 'SUBVARIETY', 'FORM', 'SUBFORM', 'PATHOVAR', 'BIOVAR', 'CHEMOVAR', 'MORPHOVAR', 'PHAGOVAR', 'SEROVAR', 'CHEMOFORM', 'FORMA_SPECIALIS', 'CULTIVAR', 'STRAIN', 'OTHER', 'UNRANKED');

ALTER TABLE name_usage ALTER COLUMN rank type rank USING (rank::rank);
ALTER TABLE typification ALTER COLUMN rank type rank USING (rank::rank);
