-- copy all usage names to interim table
CREATE TABLE tmp_usage_names AS
  SELECT DISTINCT n.id as name_key, n.scientific_name, u.rank
  FROM name_usage u JOIN name n ON u.name_fk=n.id;

-- turn off name_fk fk constraint
ALTER TABLE name_usage DROP CONSTRAINT name_usage_name_fk_fkey;

-- remove all names & reset name sequence
TRUNCATE name RESTART IDENTITY;

-- reparse names with clb-admin CLI

-- rewrite new usage table
CREATE TABLE name_usage2 AS
SELECT n.id as name_key, u.*
FROM name_usage u
  JOIN tmp_usage_names t on t.name_key=u.name_fk
  JOIN name n on n.scientific_name=t.scientific_name and n.rank=t.rank;

-- turn on name_fk fk constraint again
ALTER TABLE ONLY name_usage ADD CONSTRAINT name_usage_name_fk_fkey FOREIGN KEY (name_fk) REFERENCES name(id) DEFERRABLE INITIALLY DEFERRED;
