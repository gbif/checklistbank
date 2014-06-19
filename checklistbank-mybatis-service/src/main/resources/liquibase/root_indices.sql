DROP INDEX name_usage_dataset_key_idx1;
CREATE INDEX ON name_usage USING btree (dataset_key) WHERE (parent_fk IS NULL AND is_synonym=FALSE);
