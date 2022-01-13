-- update origin type
ALTER TABLE name_usage ALTER COLUMN origin type text;
DROP TYPE origin_type;
CREATE TYPE origin_type AS ENUM ('OTHER', 'SOURCE', 'DENORMED_CLASSIFICATION', 'VERBATIM_PARENT', 'VERBATIM_ACCEPTED', 'VERBATIM_BASIONYM', 'PROPARTE', 'AUTONYM', 'IMPLICIT_NAME', 'MISSING_ACCEPTED');
ALTER TABLE name_usage ALTER COLUMN origin type origin_type USING (origin::origin_type);
