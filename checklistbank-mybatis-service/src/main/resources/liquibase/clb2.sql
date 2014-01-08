--
-- PostgreSQL database dump
--

-- NOTE: THIS REQUIRES the hstore extension in place!
CREATE EXTENSION IF NOT EXISTS hstore;

SET search_path = public, pg_catalog;
SET client_encoding = 'UTF8';

SET statement_timeout = 0;
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;
SET default_tablespace = '';
SET default_with_oids = false;


---
--- ENUMERATIONS
---

CREATE TYPE cites_appendix AS ENUM ('I', 'II', 'III');

CREATE TYPE establishment_means AS ENUM ('NATIVE', 'INTRODUCED', 'NATURALISED', 'INVASIVE', 'MANAGED', 'UNCERTAIN');

CREATE TYPE identifier_type AS ENUM ('SOURCE_ID', 'URL', 'LSID', 'HANDLER', 'DOI', 'UUID', 'FTP', 'URI', 'UNKNOWN');

CREATE TYPE kingdom AS ENUM ('INCERTAE_SEDIS', 'ANIMALIA', 'ARCHAEA', 'BACTERIA', 'CHROMISTA', 'FUNGI', 'PLANTAE', 'PROTOZOA', 'VIRUSES');

CREATE TYPE life_stage AS ENUM ('ZYGOTE', 'EMRYO', 'LARVA', 'JUVENILE', 'ADULT', 'SPOROPHYTE', 'SPORE', 'GAMETOPHYTE', 'GAMETE');

CREATE TYPE name_part AS ENUM ('GENERIC', 'INFRAGENERIC', 'SPECIFIC', 'INFRASPECIFIC');

CREATE TYPE name_type AS ENUM ('SCINAME', 'WELLFORMED', 'DOUBTFUL', 'BLACKLISTED', 'VIRUS', 'HYBRID', 'INFORMAL', 'CULTIVAR', 'CANDIDATUS');

CREATE TYPE nomenclatural_status AS ENUM ('LEGITIMATE', 'VALIDLY_PUBLISHED', 'NEW_COMBINATION', 'REPLACEMENT', 'CONSERVED', 'PROTECTED', 'CORRECTED', 'ORIGINAL_COMBINATION', 'NEW_SPECIES', 'NEW_GENUS', 'ALTERNATIVE', 'OBSCURE', 'ABORTED', 'CONSERVED_PROPOSED', 'PROVISIONAL', 'SUBNUDUM', 'REJECTED_PROPOSED', 'REJECTED_OUTRIGHT_PROPOSED', 'DOUBTFUL', 'AMBIGUOUS', 'CONFUSED', 'FORGOTTEN', 'ORTHOGRAPHIC_VARIANT', 'SUPERFLUOUS', 'NUDUM', 'NULL_NAME', 'SUPPRESSED', 'REJECTED_OUTRIGHT', 'REJECTED', 'ILLEGITIMATE', 'INVALID', 'DENIED');

CREATE TYPE occurrence_status AS ENUM ('PRESENT', 'COMMON', 'RARE', 'IRREGULAR', 'DOUBTFUL', 'EXCLUDED', 'ABSENT');

CREATE TYPE origin_type AS ENUM ('OTHER', 'SOURCE', 'DENORMED_CLASSIFICATION', 'VERBATIM_PARENT', 'VERBATIM_BASIONYM', 'PROPARTE', 'AUTONYM', 'IMPLICIT_NAME', 'MISSING_ACCEPTED');

CREATE TYPE rank AS ENUM ('DOMAIN', 'KINGDOM', 'SUBKINGDOM', 'SUPERPHYLUM', 'PHYLUM', 'SUBPHYLUM', 'SUPERCLASS', 'CLASS', 'SUBCLASS', 'SUPERORDER', 'ORDER', 'SUBORDER', 'INFRAORDER', 'SUPERFAMILY', 'FAMILY', 'SUBFAMILY', 'TRIBE', 'SUBTRIBE', 'SUPRAGENERIC_NAME', 'GENUS', 'SUBGENUS', 'SECTION', 'SUBSECTION', 'SERIES', 'SUBSERIES', 'INFRAGENERIC_NAME', 'SPECIES', 'INFRASPECIFIC_NAME', 'SUBSPECIES', 'INFRASUBSPECIFIC_NAME', 'VARIETY', 'SUBVARIETY', 'FORM', 'SUBFORM', 'CULTIVAR_GROUP','CULTIVAR', 'STRAIN', 'INFORMAL', 'UNRANKED');

CREATE TYPE sex AS ENUM ('NONE', 'MALE', 'FEMALE', 'HERMAPHRODITE');

CREATE TYPE taxonomic_status AS ENUM ('ACCEPTED', 'DOUBTFUL', 'SYNONYM', 'HETEROTYPIC_SYNONYM', 'HOMOTYPIC_SYNONYM', 'PROPARTE_SYNONYM', 'MISAPPLIED', 'INTERMEDIATE_RANK_SYNONYM', 'DETERMINATION_SYNONYM');

CREATE TYPE threat_status AS ENUM ('LEAST_CONCERN', 'NEAR_THREATENED', 'VULNERABLE', 'ENDANGERED', 'CRITICALLY_ENDANGERED', 'EXTINCT_IN_THE_WILD', 'EXTINCT', 'DATA_DEFICIENT', 'NOT_EVALUATED');

CREATE TYPE type_designation_type AS ENUM ('ORIGINAL_DESIGNATION', 'PRESENT_DESIGNATION', 'SUBSEQUENT_DESIGNATION', 'MONOTYPY', 'SUBSEQUENT_MONOTYPY', 'TAUTONYMY', 'ABSOLUTE_TAUTONYMY', 'LINNAEAN_TAUTONYMY', 'RULING_BY_COMMISSION');

CREATE TYPE type_status AS ENUM ('TYPE', 'TYPE_SPECIES', 'TYPE_GENUS', 'ALLOLECTOTYPE', 'ALLONEOTYPE', 'ALLOTYPE', 'COTYPE', 'EPITYPE', 'EXEPITYPE', 'EXHOLOTYPE', 'EXISOTYPE', 'EXLECTOTYPE', 'EXNEOTYPE', 'EXPARATYPE', 'EXSYNTYPE', 'EXTYPE', 'HAPANTOTYPE', 'HOLOTYPE', 'ICONOTYPE', 'ISOLECTOTYPE', 'ISONEOTYPE', 'ISOSYNTYPE', 'ISOTYPE', 'LECTOTYPE', 'NEOTYPE', 'NOTATYPE', 'ORIGINALMATERIAL', 'PARALECTOTYPE', 'PARANEOTYPE', 'PARATYPE', 'PLASTOHOLOTYPE', 'PLASTOISOTYPE', 'PLASTOLECTOTYPE', 'PLASTONEOTYPE', 'PLASTOPARATYPE', 'PLASTOSYNTYPE', 'PLASTOTYPE', 'SECONDARYTYPE', 'SUPPLEMENTARYTYPE', 'SYNTYPE', 'TOPOTYPE');




---
--- FUNCTIONS
---

CREATE or replace FUNCTION authorship_full(authorship character varying, year character varying, authorship_basionym character varying, year_basionym character varying) RETURNS text
      AS $$ BEGIN RETURN (COALESCE(authorship, ''::character varying)::text || CASE WHEN year IS NOT NULL THEN ', '::text || year::text ELSE ''::text END) || CASE WHEN authorship_basionym IS NOT NULL OR year_basionym IS NOT NULL THEN (' ('::text || COALESCE((authorship_basionym::text || ', '::text) || year_basionym::text, authorship_basionym::text, year_basionym::text)) || ')'::text ELSE ''::text END; END; $$ LANGUAGE plpgsql IMMUTABLE;


---
--- TABLES
---

CREATE TABLE citation (
    id serial NOT NULL,
    citation text,
    link text,
    identifier text,
    PRIMARY KEY (id)
);


CREATE TABLE dataset_metrics (
    id serial NOT NULL,
    dataset_key uuid NOT NULL,
   -- basic metrics
    count_usages integer DEFAULT 0,
    count_synonyms integer DEFAULT 0,
    count_names integer DEFAULT 0,
    -- coverage
    count_col integer DEFAULT 0,
    count_nub integer DEFAULT 0,
    -- hstore based count maps
    count_by_rank hstore,
    count_by_kingdom hstore,
    count_by_origin hstore,
    count_vernacular_by_lang hstore,
    count_extensions hstore,
    count_other hstore,
    --
    downloaded timestamp,
    created timestamp DEFAULT now(),
    PRIMARY KEY (id)
);

CREATE TABLE description (
    id serial NOT NULL,
    dataset_key uuid NOT NULL,
    usage_fk integer,
    description text,
    type text,
    language character(2),
    source_fk integer,
    creator text,
    contributor text,
    license text,
    PRIMARY KEY (id)
);

CREATE TABLE distribution (
    id serial NOT NULL,
    dataset_key uuid NOT NULL,
    usage_fk integer,
    location_id text,
    locality text,
    country character(2),
    occurrence_status occurrence_status,
    threat_status threat_status,
    establishment_means establishment_means,
    appendix_cites cites_appendix,
    start_day_of_year integer,
    end_day_of_year integer,
    start_year integer,
    end_year integer,
    source_fk integer,
    remarks text,
    PRIMARY KEY (id)
);

CREATE TABLE identifier (
    id serial NOT NULL,
    dataset_key uuid NOT NULL,
    usage_fk integer,
    type identifier_type NOT NULL,
    identifier text,
    title text,
    subject text,
    PRIMARY KEY (id)
);


CREATE TABLE image (
    id serial NOT NULL,
    dataset_key uuid NOT NULL,
    usage_fk integer,
    url text,
    link text,
    title text,
    description text,
    license text,
    creator text,
    created text,
    publisher text,
    rating smallint,
    PRIMARY KEY (id)
);

CREATE TABLE name (
    id serial NOT NULL,
    scientific_name text,
    canonical_name text,
    type name_type NOT NULL,
    genus_or_above text,
    infra_generic text,
    specific_epithet text,
    infra_specific_epithet text,
    cultivar_epithet text,
    notho_type name_part,
    rank_marker text,
    authors_parsed boolean DEFAULT false NOT NULL,
    authorship text,
    year text,
    year_int integer,
    bracket_authorship text,
    bracket_year text,
    nom_status text,
    sensu text,
    remarks text,
    modified timestamp DEFAULT now(),
    PRIMARY KEY (id)
);

CREATE TABLE nub_rel (
    usage_fk integer,
    nub_fk integer NOT NULL,
    dataset_key uuid NOT NULL,
    PRIMARY KEY (usage_fk)
);

CREATE TABLE name_usage (
    id serial NOT NULL,
    dataset_key uuid NOT NULL,
    constituent_key uuid,
    name_fk integer NOT NULL,
    rank rank,
    parent_fk integer,
    is_synonym boolean NOT NULL,
    pp_synonym_fk integer,
    status taxonomic_status,
    nom_status nomenclatural_status[],
    basionym_fk integer,
    kingdom_fk integer,
    phylum_fk integer,
    class_fk integer,
    order_fk integer,
    family_fk integer,
    genus_fk integer,
    subgenus_fk integer,
    species_fk integer,
    name_published_in_fk integer,
    according_to_fk integer,
    citation_fk integer,
    origin origin_type DEFAULT 'SOURCE',
    remarks text,
    lft integer,
    rgt integer,
    modified timestamp DEFAULT now(),
    PRIMARY KEY (id)
);


CREATE TABLE name_usage_metrics (
    usage_fk integer NOT NULL,
    dataset_key uuid NOT NULL,
    count_descendants integer DEFAULT 0,
    count_children integer DEFAULT 0,
    count_synonyms integer DEFAULT 0,
    count_k integer DEFAULT 0,
    count_p integer DEFAULT 0,
    count_c integer DEFAULT 0,
    count_o integer DEFAULT 0,
    count_f integer DEFAULT 0,
    count_g integer DEFAULT 0,
    count_sg integer DEFAULT 0,
    count_s integer DEFAULT 0,
    count_is integer DEFAULT 0,
    modified timestamp DEFAULT now(),
    PRIMARY KEY (usage_fk)
);


CREATE TABLE literature (
    id serial NOT NULL,
    usage_fk integer NOT NULL,
    citation_fk integer NOT NULL,
    dataset_key uuid NOT NULL,
    type text,
    remarks text,
    PRIMARY KEY(id)
);

CREATE TABLE raw_usage (
    usage_fk integer NOT NULL,
    dataset_key uuid NOT NULL,
    json text,
    created timestamp DEFAULT now(),
    PRIMARY KEY (usage_fk)
);

CREATE TABLE vernacular_name (
    id serial NOT NULL,
    dataset_key uuid NOT NULL,
    usage_fk integer NOT NULL,
    name text,
    language character(2),
    preferred boolean DEFAULT false,
    sex sex,
    life_stage life_stage,
    temporal text,
    location_id text,
    locality text,
    country character(2),
    source_fk integer,
    PRIMARY KEY (id)
);

CREATE TABLE species_info (
    id serial NOT NULL,
    dataset_key uuid NOT NULL,
    usage_fk integer NOT NULL,
    marine boolean,
    terrestrial boolean,
    extinct boolean,
    hybrid boolean,
    living_period text,
    age_in_days integer,
    size_in_millimeter numeric,
    mass_in_gram numeric,
    life_form text,
    habitat text,
    PRIMARY KEY (id)
);

CREATE TABLE specimen (
    id serial NOT NULL,
    dataset_key uuid NOT NULL,
    usage_fk integer,
    citation_fk integer,
    type_status type_status,
    type_designation_type type_designation_type,
    type_designated_by text,
    scientific_name text,
    rank rank,
    occurrence_id text,
    institution_code text,
    collection_code text,
    catalog_number text,
    locality text,
    recorded_by text,
    source_fk integer,
    verbatim_event_date text,
    verbatim_label text,
    verbatim_longitude text,
    verbatim_latitude text,
    PRIMARY KEY (id)
);


CREATE TABLE user_checklist (
    id serial NOT NULL,
    title text,
    description text,
    kingdom kingdom,
    username text,
    num_names integer DEFAULT 0,
    modified timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);

CREATE TABLE user_checklist_name (
    id serial NOT NULL,
    source_id text,
    scientific_name text,
    classification text,
    rank rank,
    nub_fk integer,
    user_checklist_fk integer NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE user_checklist_crossmap (
    id serial NOT NULL,
    user_checklist_fk integer NOT NULL,
    dataset_key uuid NOT NULL,
    modified timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);



-----------------
-- INDICES --
-----------------

CREATE INDEX ON citation USING btree (hashtext(citation));

CREATE INDEX ON dataset_metrics USING btree (dataset_key, created DESC);

CREATE INDEX ON description USING btree (usage_fk);
CREATE INDEX ON description USING btree (dataset_key);

CREATE INDEX ON distribution USING btree (usage_fk);
CREATE INDEX ON distribution USING btree (dataset_key);

CREATE INDEX ON identifier USING btree (hashtext(identifier));
CREATE INDEX ON identifier USING btree (type);
CREATE INDEX ON identifier USING btree (usage_fk);
CREATE INDEX ON identifier USING btree (dataset_key);

CREATE INDEX ON image USING btree (usage_fk);
CREATE INDEX ON image USING btree (dataset_key);

CREATE INDEX ON user_checklist_name USING btree (user_checklist_fk);

CREATE INDEX ON name USING btree (lower(canonical_name));
CREATE INDEX ON name USING btree (type);
CREATE INDEX ON name USING btree (notho_type);

CREATE INDEX ON name_usage USING btree (dataset_key);
CREATE INDEX ON name_usage USING btree (dataset_key) WHERE (parent_fk IS NULL);
CREATE INDEX ON name_usage USING btree (parent_fk);
CREATE INDEX ON name_usage USING btree (rank);
CREATE INDEX ON name_usage USING btree (is_synonym);
CREATE INDEX ON name_usage USING btree (name_fk);
CREATE INDEX ON name_usage USING btree (status);
CREATE INDEX ON name_usage USING btree (origin);
CREATE INDEX ON name_usage USING btree (pp_synonym_fk);

CREATE INDEX ON nub_rel USING btree (dataset_key);
CREATE INDEX ON nub_rel USING btree (nub_fk);

CREATE INDEX ON literature USING btree (type);
CREATE INDEX ON literature USING btree (usage_fk);
CREATE INDEX ON literature USING btree (dataset_key);

CREATE INDEX ON species_info USING btree (usage_fk);
CREATE INDEX ON species_info USING btree (dataset_key);

CREATE INDEX ON specimen USING btree (type_status);
CREATE INDEX ON specimen USING btree (usage_fk);
CREATE INDEX ON specimen USING btree (dataset_key);

CREATE INDEX ON vernacular_name USING btree (lower(name));
CREATE INDEX ON vernacular_name USING btree (usage_fk);
CREATE INDEX ON vernacular_name USING btree (dataset_key);


-----------------
-- CONSTRAINTS --
-----------------

ALTER TABLE ONLY user_checklist_crossmap ADD CONSTRAINT user_checklist_crossmap_user_checklist_fk_fkey FOREIGN KEY (user_checklist_fk) REFERENCES user_checklist(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY description ADD CONSTRAINT description_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY description ADD CONSTRAINT description_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY distribution ADD CONSTRAINT distribution_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY distribution ADD CONSTRAINT distribution_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY identifier ADD CONSTRAINT identifier_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY image ADD CONSTRAINT image_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY user_checklist_name ADD CONSTRAINT user_checklist_name_user_checklist_fk_fkey FOREIGN KEY (user_checklist_fk) REFERENCES user_checklist(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY user_checklist_name ADD CONSTRAINT user_checklist_name_nub_fk_fkey FOREIGN KEY (nub_fk) REFERENCES name_usage(id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY name_usage ADD CONSTRAINT name_usage_according_to_fk_fkey FOREIGN KEY (according_to_fk) REFERENCES citation(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY name_usage ADD CONSTRAINT name_usage_citation_fk_fkey FOREIGN KEY (citation_fk) REFERENCES citation(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY name_usage ADD CONSTRAINT name_usage_name_fk_fkey FOREIGN KEY (name_fk) REFERENCES name(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY name_usage ADD CONSTRAINT name_usage_name_published_in_fk_fkey FOREIGN KEY (name_published_in_fk) REFERENCES citation(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY literature ADD CONSTRAINT literature_citation_fk_fkey FOREIGN KEY (citation_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY literature ADD CONSTRAINT literature_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY raw_usage ADD CONSTRAINT raw_usage_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY nub_rel ADD CONSTRAINT nub_rel_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY nub_rel ADD CONSTRAINT nub_rel_nub_fk_fkey FOREIGN KEY (nub_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY species_info ADD CONSTRAINT species_info_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY specimen ADD CONSTRAINT specimen_citation_fk_fkey FOREIGN KEY (citation_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY specimen ADD CONSTRAINT specimen_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY specimen ADD CONSTRAINT specimen_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY vernacular_name ADD CONSTRAINT vernacular_name_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE ONLY vernacular_name ADD CONSTRAINT vernacular_name_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

