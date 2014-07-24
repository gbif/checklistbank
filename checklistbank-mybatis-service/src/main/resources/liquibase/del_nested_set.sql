ALTER TABLE name_usage ADD COLUMN num_descendants INTEGER;
UPDATE name_usage SET num_descendants = (rgt-lft-1)/2;
ALTER TABLE name_usage DROP COLUMN lft;
ALTER TABLE name_usage DROP COLUMN rgt;
