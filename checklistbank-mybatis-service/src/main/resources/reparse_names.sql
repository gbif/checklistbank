-- keep name persistence problems in a separate table
CREATE TABLE tmp_name_failure (
  tmp_usage_name_id int PRIMARY KEY,
  rank rank,
  scientific_name text
);

-- add tmp_usage_name_id column to name table - will be rmeoved at the end again
ALTER TABLE name ADD COLUMN tmp_usage_name_id int;

-- copy all distinct usage names to interim table
CREATE TABLE tmp_usage_name (
  id SERIAL PRIMARY KEY,
  scientific_name text,
  rank rank,
  keys int[]
);

INSERT INTO tmp_usage_name (scientific_name, rank, keys)
  SELECT n.scientific_name, u.rank, array_agg(u.id) as keys
  FROM name_usage u JOIN name n ON u.name_fk=n.id
  GROUP BY 1,2;

-- turn off name_fk constraint
ALTER TABLE name_usage DROP CONSTRAINT name_usage_name_fk_fkey;

-- remove all names & reset name sequence
TRUNCATE name RESTART IDENTITY;

-- reparse names with clb-admin CLI: clb.admin.sh REPARSE

-- prepare for join to rewrite usage table
CREATE TABLE tmp_usage AS
  SELECT unnest(t.keys) as key, n.id as name_fk
  FROM tmp_usage_name t
    JOIN name n on n.tmp_usage_name_id=t.id;

INSERT INTO tmp_usage
  SELECT unnest(t.keys) as key, n.id as name_fk
  FROM tmp_usage_name t
    JOIN tmp_name_failure f on t.id=f.tmp_usage_name_id
    JOIN name n on n.rank=f.rank and n.scientific_name=f.scientific_name;

ALTER TABLE tmp_usage add primary key (key);

-- make sure counts line up!
SELECT count(*) from name_usage;
SELECT count(*) from tmp_usage;

-- rewrite new usage table
CREATE TABLE name_usage2 AS
SELECT t.name_fk as name_key, u.*
FROM name_usage u
  JOIN tmp_usage t on t.key=u.id;

-- use new names column
ALTER TABLE name_usage2 DROP COLUMN name_fk;
ALTER TABLE name_usage2 RENAME COLUMN name_key TO name_fk;

-- drop constraints for old usage table
ALTER TABLE description DROP CONSTRAINT description_usage_fk_fkey;
ALTER TABLE distribution DROP CONSTRAINT distribution_usage_fk_fkey;
ALTER TABLE identifier DROP CONSTRAINT identifier_usage_fk_fkey;
ALTER TABLE literature DROP CONSTRAINT literature_usage_fk_fkey;
ALTER TABLE media DROP CONSTRAINT media_usage_fk_fkey;
ALTER TABLE name_usage_metrics DROP CONSTRAINT name_usage_metrics_usage_fk_fkey;
ALTER TABLE nub_rel DROP CONSTRAINT nub_rel_nub_fk_fkey;
ALTER TABLE nub_rel DROP CONSTRAINT nub_rel_usage_fk_fkey;
ALTER TABLE raw_usage DROP CONSTRAINT raw_usage_usage_fk_fkey;
ALTER TABLE species_info DROP CONSTRAINT species_info_usage_fk_fkey;
ALTER TABLE typification DROP CONSTRAINT typification_usage_fk_fkey;
ALTER TABLE vernacular_name DROP CONSTRAINT vernacular_name_usage_fk_fkey;

-- drop dependend views
DROP VIEW nub;
DROP VIEW nub_homonyms;
DROP VIEW kname;

-- drop old table
DROP TABLE name_usage;

-- rename new table to proper usage and recreate constraints and indices
ALTER TABLE name_usage2 RENAME TO name_usage;

-- recreate constraints
ALTER TABLE ONLY name_usage ADD PRIMARY KEY(id);
CREATE SEQUENCE name_usage_id_seq;
SELECT setval('name_usage_id_seq', (select max(id) from name_usage)+1);
ALTER TABLE name_usage ALTER COLUMN id SET DEFAULT nextval('name_usage_id_seq');
ALTER TABLE name_usage ALTER COLUMN modified SET DEFAULT now();
ALTER TABLE name_usage ALTER COLUMN last_interpreted SET DEFAULT now();
ALTER TABLE name_usage ALTER COLUMN dataset_key SET NOT NULL;
ALTER TABLE name_usage ALTER COLUMN name_fk SET NOT NULL;
ALTER TABLE name_usage ALTER COLUMN is_synonym SET NOT NULL;

ALTER TABLE ONLY name_usage ADD FOREIGN KEY (name_fk) REFERENCES name(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY name_usage ADD FOREIGN KEY (according_to_fk) REFERENCES citation(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY name_usage ADD FOREIGN KEY (name_published_in_fk) REFERENCES citation(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY description ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY distribution ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY identifier ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY literature ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY media ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY name_usage_metrics ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY nub_rel ADD FOREIGN KEY (nub_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY nub_rel ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY raw_usage ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY species_info ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY typification ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY vernacular_name ADD FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

-- indices
CREATE INDEX ON name_usage USING btree (basionym_fk) WHERE deleted IS NULL;
CREATE INDEX ON name_usage USING btree (dataset_key) WHERE deleted IS NULL;
CREATE INDEX ON name_usage USING btree (dataset_key) WHERE deleted IS NULL AND parent_fk IS NULL AND is_synonym = false;
CREATE INDEX ON name_usage USING btree (dataset_key, last_interpreted) WHERE deleted IS NULL;
CREATE INDEX ON name_usage USING btree (dataset_key, taxon_id);
CREATE INDEX ON name_usage USING btree (deleted) WHERE deleted IS NULL;
CREATE INDEX ON name_usage USING btree (is_synonym);
CREATE INDEX ON name_usage USING btree (name_fk);
CREATE INDEX ON name_usage USING btree (origin);
CREATE INDEX ON name_usage USING btree (parent_fk) WHERE deleted IS NULL AND is_synonym = false;
CREATE INDEX ON name_usage USING btree (parent_fk) WHERE deleted IS NULL AND is_synonym = true;
CREATE INDEX ON name_usage USING btree (pp_synonym_fk);
CREATE INDEX ON name_usage USING btree (rank);
CREATE INDEX ON name_usage USING btree (status);


-- drop tmp tables and columns
DROP TABLE tmp_usage;
DROP TABLE tmp_usage_name;
DROP TABLE tmp_name_failure;
ALTER TABLE name DROP COLUMN tmp_usage_name_id;
