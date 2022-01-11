
ALTER TABLE name_usage ADD COLUMN "references" text;
ALTER TABLE name_usage ADD COLUMN source_id text;

-- copy data from identifier table to name_usage
UPDATE name_usage nu set source_id=i.identifier
FROM (
  SELECT distinct on (usage_fk) usage_fk, identifier
  FROM identifier WHERE type='SOURCE_ID'
) AS i
WHERE nu.id = i.usage_fk;

UPDATE name_usage nu set "references"=i.identifier
FROM (
  SELECT distinct on (usage_fk) usage_fk, identifier
  FROM identifier WHERE type='URL'
) AS i
WHERE nu.id = i.usage_fk;

-- remove sourceid from id types and records
DELETE from identifier WHERE type='SOURCE_ID';
ALTER TABLE identifier ALTER COLUMN type type text;
DROP TYPE identifier_type;
CREATE TYPE identifier_type AS ENUM ('URL', 'LSID', 'HANDLER', 'DOI', 'UUID', 'FTP', 'URI', 'UNKNOWN');
ALTER TABLE identifier ALTER COLUMN type type identifier_type USING (type::identifier_type);
