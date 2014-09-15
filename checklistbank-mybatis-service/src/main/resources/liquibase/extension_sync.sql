ALTER TABLE description DROP COLUMN dataset_key;

ALTER TABLE distribution DROP COLUMN dataset_key;
ALTER TABLE distribution ADD COLUMN temporal TEXT;
ALTER TABLE distribution ADD COLUMN life_stage life_stage;

ALTER TABLE identifier DROP COLUMN dataset_key;
ALTER TABLE identifier DROP COLUMN subject;

ALTER TABLE literature DROP COLUMN dataset_key;

ALTER TABLE species_info DROP COLUMN dataset_key;
ALTER TABLE species_info ADD COLUMN freshwater BOOLEAN;
ALTER TABLE species_info ADD COLUMN source_fk integer;
ALTER TABLE ONLY species_info ADD CONSTRAINT species_info_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE vernacular_name DROP COLUMN dataset_key;
ALTER TABLE vernacular_name DROP COLUMN temporal;
ALTER TABLE vernacular_name DROP COLUMN location_id;
ALTER TABLE vernacular_name ADD COLUMN plural BOOLEAN;
ALTER TABLE vernacular_name RENAME COLUMN locality TO area;


DROP TABLE image;
CREATE TYPE media_type AS ENUM ('StillImage', 'MovingImage', 'Sound');
CREATE TABLE media (
  id serial NOT NULL,
  usage_fk integer,
  type media_type,
  format text,
  identifier text,
  "references" text,
  title text,
  description text,
  audience text,
  created timestamp,
  creator text,
  contributor text,
  publisher text,
  license text,
  rights_holder text,
  source_fk integer,
  PRIMARY KEY (id)
);


DROP TABLE specimen;
CREATE TABLE typification (
  id serial NOT NULL,
  usage_fk integer,
  rank rank,
  scientific_name text,
  designated_by text,
  designation_type type_designation_type,
  source_fk integer,
  PRIMARY KEY (id)
);
