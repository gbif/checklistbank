-- copy all usage names to interim table
CREATE TABLE tmp_usage_names AS
  SELECT DISTINCT n.id as name_key, n.scientific_name, u.rank
  FROM name_usage u JOIN name n ON u.name_fk=n.id;

-- turn off name_fk constraint
ALTER TABLE name_usage DROP CONSTRAINT name_usage_name_fk_fkey;

-- remove all names & reset name sequence
TRUNCATE name RESTART IDENTITY;

-- reparse names with clb-admin CLI

-- prepare for join to rewrite usage table
CREATE TABLE tmp_usage AS
  SELECT scientific_name, rank, unnest(keys) as usage_fk
  FROM tmp_usage_names;

ALTER table tmp_usage add primary key (usage_fk);

-- rewrite new usage table
CREATE TABLE name_usage2 AS
SELECT n.id as name_key, u.*
FROM name_usage u
  JOIN tmp_usage t on t.usage_fk=u.id
  JOIN name n on n.scientific_name=t.scientific_name and n.rank=t.rank;

-- make sure counts line up!
SELECT count(*) from name_usage;
SELECT count(*) from name_usage2;

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

-- drop old table
DROP TABLE name_usage;

-- rename new table to proper usage and recreate constraints and indices
ALTER TABLE name_usage2 RENAME TO name_usage;

-- recreate constraints
ALTER TABLE ONLY name_usage ADD PRIMARY KEY(id);
ALTER TABLE ONLY name_usage ADD CONSTRAINT FOREIGN KEY (name_fk) REFERENCES name(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY name_usage ADD CONSTRAINT FOREIGN KEY (according_to_fk) REFERENCES citation(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY name_usage ADD CONSTRAINT FOREIGN KEY (name_published_in_fk) REFERENCES citation(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY description ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY distribution ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY identifier ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY literature ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY media ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY name_usage_metrics ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY nub_rel ADD CONSTRAINT FOREIGN KEY (nub_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY nub_rel ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY raw_usage ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY species_info ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY typification ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
ALTER TABLE ONLY vernacular_name ADD CONSTRAINT FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED

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
