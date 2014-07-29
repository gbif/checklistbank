ALTER TABLE name_usage RENAME COLUMN source_id TO taxon_id;
CREATE INDEX ON name_usage USING btree (dataset_key, taxon_id);
ALTER TABLE name_usage ADD COLUMN last_interpreted timestamp DEFAULT now();

-- use binary smile for verbatim data
ALTER TABLE raw_usage DROP COLUMN json;
ALTER TABLE raw_usage ADD COLUMN smile bytea;

-- add a latest column to easily filter the latest metrics record for joins with name usages
ALTER TABLE dataset_metrics ADD COLUMN latest boolean default true;
CREATE TABLE latest_metrics AS SELECT DISTINCT ON (dataset_key) id, dataset_key from dataset_metrics ORDER BY dataset_key, created DESC;
UPDATE dataset_metrics SET latest=false;
UPDATE dataset_metrics SET latest=true WHERE id IN (SELECT id FROM latest_metrics);
DROP TABLE latest_metrics;
CREATE UNIQUE INDEX ON dataset_metrics (dataset_key) WHERE latest;

-- drop unused columns
ALTER TABLE raw_usage DROP COLUMN created;
ALTER TABLE name_usage_metrics DROP COLUMN count_is;
ALTER TABLE name_usage_metrics DROP COLUMN count_k;
ALTER TABLE name_usage DROP COLUMN citation_fk;

-- correct defaults
ALTER TABLE name_usage ALTER COLUMN origin DROP DEFAULT;
ALTER TABLE name_usage ALTER COLUMN modified DROP DEFAULT;
