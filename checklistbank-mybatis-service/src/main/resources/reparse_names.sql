-- copy all usage names to interim table
CREATE TABLE tmp_usage_names AS
  SELECT u.id, u.rank, n.scientific_name
  FROM name_usage u JOIN name n ON u.name_fk=n.id;

-- turn off name_fk fk constraint
ALTER TABLE name_usage DROP CONSTRAINT name_usage_name_fk_fkey;

-- remove all names & reset name sequence
TRUNCATE name RESTART IDENTITY;

-- reparse names with clb-admin CLI

-- turn on name_fk fk constraint again
ALTER TABLE ONLY name_usage ADD CONSTRAINT name_usage_name_fk_fkey FOREIGN KEY (name_fk) REFERENCES name(id) DEFERRABLE INITIALLY DEFERRED;
