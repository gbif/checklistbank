--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: hstore; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;


--
-- Name: EXTENSION hstore; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION hstore IS 'data type for storing sets of (key, value) pairs';


SET search_path = public, pg_catalog;

--
-- Name: cites_appendix; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE cites_appendix AS ENUM (
    'I',
    'II',
    'III'
);


ALTER TYPE public.cites_appendix OWNER TO postgres;

--
-- Name: establishment_means; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE establishment_means AS ENUM (
    'NATIVE',
    'INTRODUCED',
    'NATURALISED',
    'INVASIVE',
    'MANAGED',
    'UNCERTAIN'
);


ALTER TYPE public.establishment_means OWNER TO postgres;

--
-- Name: identifier_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE identifier_type AS ENUM (
    'URL',
    'LSID',
    'HANDLER',
    'DOI',
    'UUID',
    'FTP',
    'URI',
    'UNKNOWN'
);


ALTER TYPE public.identifier_type OWNER TO postgres;

--
-- Name: kingdom; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE kingdom AS ENUM (
    'INCERTAE_SEDIS',
    'ANIMALIA',
    'ARCHAEA',
    'BACTERIA',
    'CHROMISTA',
    'FUNGI',
    'PLANTAE',
    'PROTOZOA',
    'VIRUSES'
);


ALTER TYPE public.kingdom OWNER TO postgres;

--
-- Name: life_stage; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE life_stage AS ENUM (
    'ZYGOTE',
    'EMRYO',
    'LARVA',
    'JUVENILE',
    'ADULT',
    'SPOROPHYTE',
    'SPORE',
    'GAMETOPHYTE',
    'GAMETE'
);


ALTER TYPE public.life_stage OWNER TO postgres;

--
-- Name: media_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE media_type AS ENUM (
    'StillImage',
    'MovingImage',
    'Sound'
);


ALTER TYPE public.media_type OWNER TO postgres;

--
-- Name: name_part; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE name_part AS ENUM (
    'GENERIC',
    'INFRAGENERIC',
    'SPECIFIC',
    'INFRASPECIFIC'
);


ALTER TYPE public.name_part OWNER TO postgres;

--
-- Name: name_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE name_type AS ENUM (
    'SCIENTIFIC',
    'VIRUS',
    'HYBRID',
    'INFORMAL',
    'CULTIVAR',
    'CANDIDATUS',
    'DOUBTFUL',
    'PLACEHOLDER',
    'NO_NAME'
);


ALTER TYPE public.name_type OWNER TO postgres;

--
-- Name: nomenclatural_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE nomenclatural_status AS ENUM (
    'LEGITIMATE',
    'VALIDLY_PUBLISHED',
    'NEW_COMBINATION',
    'REPLACEMENT',
    'CONSERVED',
    'PROTECTED',
    'CORRECTED',
    'ORIGINAL_COMBINATION',
    'NEW_SPECIES',
    'NEW_GENUS',
    'ALTERNATIVE',
    'OBSCURE',
    'ABORTED',
    'CONSERVED_PROPOSED',
    'PROVISIONAL',
    'SUBNUDUM',
    'REJECTED_PROPOSED',
    'REJECTED_OUTRIGHT_PROPOSED',
    'DOUBTFUL',
    'AMBIGUOUS',
    'CONFUSED',
    'FORGOTTEN',
    'ORTHOGRAPHIC_VARIANT',
    'SUPERFLUOUS',
    'NUDUM',
    'NULL_NAME',
    'SUPPRESSED',
    'REJECTED_OUTRIGHT',
    'REJECTED',
    'ILLEGITIMATE',
    'INVALID',
    'DENIED'
);


ALTER TYPE public.nomenclatural_status OWNER TO postgres;

--
-- Name: occurrence_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE occurrence_status AS ENUM (
    'PRESENT',
    'COMMON',
    'RARE',
    'IRREGULAR',
    'DOUBTFUL',
    'EXCLUDED',
    'ABSENT'
);


ALTER TYPE public.occurrence_status OWNER TO postgres;

--
-- Name: origin_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE origin_type AS ENUM (
    'EX_AUTHOR_SYNONYM',
    'OTHER',
    'SOURCE',
    'DENORMED_CLASSIFICATION',
    'VERBATIM_PARENT',
    'VERBATIM_ACCEPTED',
    'VERBATIM_BASIONYM',
    'PROPARTE',
    'AUTONYM',
    'IMPLICIT_NAME',
    'MISSING_ACCEPTED',
    'BASIONYM_PLACEHOLDER'
);


ALTER TYPE public.origin_type OWNER TO postgres;

--
-- Name: rank; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE rank AS ENUM (
    'DOMAIN',
    'SUPERKINGDOM',
    'KINGDOM',
    'SUBKINGDOM',
    'INFRAKINGDOM',
    'SUPERPHYLUM',
    'PHYLUM',
    'SUBPHYLUM',
    'INFRAPHYLUM',
    'SUPERCLASS',
    'CLASS',
    'SUBCLASS',
    'INFRACLASS',
    'PARVCLASS',
    'SUPERLEGION',
    'LEGION',
    'SUBLEGION',
    'INFRALEGION',
    'SUPERCOHORT',
    'COHORT',
    'SUBCOHORT',
    'INFRACOHORT',
    'MAGNORDER',
    'SUPERORDER',
    'GRANDORDER',
    'ORDER',
    'SUBORDER',
    'INFRAORDER',
    'PARVORDER',
    'SUPERFAMILY',
    'FAMILY',
    'SUBFAMILY',
    'INFRAFAMILY',
    'SUPERTRIBE',
    'TRIBE',
    'SUBTRIBE',
    'INFRATRIBE',
    'SUPRAGENERIC_NAME',
    'GENUS',
    'SUBGENUS',
    'INFRAGENUS',
    'SECTION',
    'SUBSECTION',
    'SERIES',
    'SUBSERIES',
    'INFRAGENERIC_NAME',
    'SPECIES_AGGREGATE',
    'SPECIES',
    'INFRASPECIFIC_NAME',
    'GREX',
    'SUBSPECIES',
    'CULTIVAR_GROUP',
    'CONVARIETY',
    'INFRASUBSPECIFIC_NAME',
    'PROLES',
    'RACE',
    'NATIO',
    'ABERRATION',
    'MORPH',
    'VARIETY',
    'SUBVARIETY',
    'FORM',
    'SUBFORM',
    'PATHOVAR',
    'BIOVAR',
    'CHEMOVAR',
    'MORPHOVAR',
    'PHAGOVAR',
    'SEROVAR',
    'CHEMOFORM',
    'FORMA_SPECIALIS',
    'CULTIVAR',
    'STRAIN',
    'OTHER',
    'UNRANKED'
);


ALTER TYPE public.rank OWNER TO postgres;

--
-- Name: sex; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE sex AS ENUM (
    'NONE',
    'MALE',
    'FEMALE',
    'HERMAPHRODITE'
);


ALTER TYPE public.sex OWNER TO postgres;

--
-- Name: taxonomic_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE taxonomic_status AS ENUM (
    'ACCEPTED',
    'DOUBTFUL',
    'SYNONYM',
    'HETEROTYPIC_SYNONYM',
    'HOMOTYPIC_SYNONYM',
    'PROPARTE_SYNONYM',
    'MISAPPLIED',
    'INTERMEDIATE_RANK_SYNONYM',
    'DETERMINATION_SYNONYM'
);


ALTER TYPE public.taxonomic_status OWNER TO postgres;

--
-- Name: threat_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE threat_status AS ENUM (
    'LEAST_CONCERN',
    'NEAR_THREATENED',
    'VULNERABLE',
    'ENDANGERED',
    'CRITICALLY_ENDANGERED',
    'EXTINCT_IN_THE_WILD',
    'EXTINCT',
    'DATA_DEFICIENT',
    'NOT_EVALUATED'
);


ALTER TYPE public.threat_status OWNER TO postgres;

--
-- Name: type_designation_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE type_designation_type AS ENUM (
    'ORIGINAL_DESIGNATION',
    'PRESENT_DESIGNATION',
    'SUBSEQUENT_DESIGNATION',
    'MONOTYPY',
    'SUBSEQUENT_MONOTYPY',
    'TAUTONYMY',
    'ABSOLUTE_TAUTONYMY',
    'LINNAEAN_TAUTONYMY',
    'RULING_BY_COMMISSION'
);


ALTER TYPE public.type_designation_type OWNER TO postgres;

--
-- Name: type_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE type_status AS ENUM (
    'TYPE',
    'TYPE_SPECIES',
    'TYPE_GENUS',
    'ALLOLECTOTYPE',
    'ALLONEOTYPE',
    'ALLOTYPE',
    'COTYPE',
    'EPITYPE',
    'EXEPITYPE',
    'EXHOLOTYPE',
    'EXISOTYPE',
    'EXLECTOTYPE',
    'EXNEOTYPE',
    'EXPARATYPE',
    'EXSYNTYPE',
    'EXTYPE',
    'HAPANTOTYPE',
    'HOLOTYPE',
    'ICONOTYPE',
    'ISOLECTOTYPE',
    'ISONEOTYPE',
    'ISOSYNTYPE',
    'ISOTYPE',
    'LECTOTYPE',
    'NEOTYPE',
    'NOTATYPE',
    'ORIGINALMATERIAL',
    'PARALECTOTYPE',
    'PARANEOTYPE',
    'PARATYPE',
    'PLASTOHOLOTYPE',
    'PLASTOISOTYPE',
    'PLASTOLECTOTYPE',
    'PLASTONEOTYPE',
    'PLASTOPARATYPE',
    'PLASTOSYNTYPE',
    'PLASTOTYPE',
    'SECONDARYTYPE',
    'SUPPLEMENTARYTYPE',
    'SYNTYPE',
    'TOPOTYPE'
);


ALTER TYPE public.type_status OWNER TO postgres;

--
-- Name: authorship_full(character varying, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION authorship_full(authorship character varying, year character varying, authorship_basionym character varying, year_basionym character varying) RETURNS text
    LANGUAGE plpgsql IMMUTABLE
    AS $$ BEGIN RETURN (COALESCE(authorship, ''::character varying)::text || CASE WHEN year IS NOT NULL THEN ', '::text || year::text ELSE ''::text END) || CASE WHEN authorship_basionym IS NOT NULL OR year_basionym IS NOT NULL THEN (' ('::text || COALESCE((authorship_basionym::text || ', '::text) || year_basionym::text, authorship_basionym::text, year_basionym::text)) || ')'::text ELSE ''::text END; END; $$;


ALTER FUNCTION public.authorship_full(authorship character varying, year character varying, authorship_basionym character varying, year_basionym character varying) OWNER TO postgres;

--
-- Name: colkey(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION colkey() RETURNS uuid
    LANGUAGE sql IMMUTABLE
    AS $$
        SELECT '7ddf754f-d193-4cc9-b351-99906754a03b'::uuid
      $$;


ALTER FUNCTION public.colkey() OWNER TO postgres;

--
-- Name: groupinfraspecificranks(rank); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION groupinfraspecificranks(rnk rank) RETURNS rank
    LANGUAGE sql
    AS $$
      SELECT CASE WHEN rnk <= 'SPECIES'::rank THEN rnk
        ELSE 'INFRASPECIFIC_NAME'::rank
      END
      $$;


ALTER FUNCTION public.groupinfraspecificranks(rnk rank) OWNER TO postgres;

--
-- Name: nubkey(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION nubkey() RETURNS uuid
    LANGUAGE sql IMMUTABLE
    AS $$
        SELECT 'd7dddbf4-2cf0-4f39-9b2a-bb099caae36c'::uuid
      $$;


ALTER FUNCTION public.nubkey() OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: citation; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE citation (
    id integer NOT NULL,
    citation text,
    link text,
    identifier text
);


ALTER TABLE public.citation OWNER TO postgres;

--
-- Name: citation_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE citation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.citation_id_seq OWNER TO postgres;

--
-- Name: citation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE citation_id_seq OWNED BY citation.id;


--
-- Name: col_annotation; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE col_annotation (
    nub_fk integer NOT NULL,
    gsd text,
    annotated_name text,
    rejected boolean,
    status text,
    note text
);


ALTER TABLE public.col_annotation OWNER TO postgres;

--
-- Name: dataset; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE dataset (
    key uuid NOT NULL,
    title text
);


ALTER TABLE public.dataset OWNER TO postgres;

--
-- Name: dataset_metrics; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE dataset_metrics (
    id integer NOT NULL,
    dataset_key uuid NOT NULL,
    count_usages integer DEFAULT 0,
    count_synonyms integer DEFAULT 0,
    count_names integer DEFAULT 0,
    count_col integer DEFAULT 0,
    count_nub integer DEFAULT 0,
    count_by_rank hstore,
    count_by_kingdom hstore,
    count_by_origin hstore,
    count_vernacular_by_lang hstore,
    count_extensions hstore,
    count_other hstore,
    downloaded timestamp without time zone,
    created timestamp without time zone DEFAULT now(),
    latest boolean DEFAULT true,
    count_by_issue hstore,
    count_by_constituent hstore
);


ALTER TABLE public.dataset_metrics OWNER TO postgres;

--
-- Name: dataset_metrics_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE dataset_metrics_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataset_metrics_id_seq OWNER TO postgres;

--
-- Name: dataset_metrics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE dataset_metrics_id_seq OWNED BY dataset_metrics.id;


--
-- Name: description; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE description (
    id integer NOT NULL,
    usage_fk integer,
    description text,
    type text,
    language character(2),
    source_fk integer,
    creator text,
    contributor text,
    license text
);


ALTER TABLE public.description OWNER TO postgres;

--
-- Name: description_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE description_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.description_id_seq OWNER TO postgres;

--
-- Name: description_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE description_id_seq OWNED BY description.id;


--
-- Name: distribution; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE distribution (
    id integer NOT NULL,
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
    temporal text,
    life_stage life_stage
);


ALTER TABLE public.distribution OWNER TO postgres;

--
-- Name: distribution_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE distribution_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.distribution_id_seq OWNER TO postgres;

--
-- Name: distribution_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE distribution_id_seq OWNED BY distribution.id;


--
-- Name: identifier; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE identifier (
    id integer NOT NULL,
    usage_fk integer,
    type identifier_type NOT NULL,
    identifier text,
    title text
);


ALTER TABLE public.identifier OWNER TO postgres;

--
-- Name: identifier_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE identifier_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.identifier_id_seq OWNER TO postgres;

--
-- Name: identifier_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE identifier_id_seq OWNED BY identifier.id;


--
-- Name: name; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE name (
    id integer NOT NULL,
    scientific_name text,
    canonical_name text,
    type name_type NOT NULL,
    genus_or_above text,
    infra_generic text,
    specific_epithet text,
    infra_specific_epithet text,
    cultivar_epithet text,
    notho_type name_part,
    authors_parsed boolean DEFAULT false NOT NULL,
    authorship text,
    year text,
    year_int integer,
    bracket_authorship text,
    bracket_year text,
    nom_status text,
    sensu text,
    remarks text,
    modified timestamp without time zone DEFAULT now(),
    rank rank,
    parsed boolean
);


ALTER TABLE public.name OWNER TO postgres;

--
-- Name: name_usage; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE name_usage (
    id integer NOT NULL,
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
    origin origin_type,
    remarks text,
    modified timestamp without time zone,
    "references" text,
    taxon_id text,
    num_descendants integer,
    last_interpreted timestamp without time zone DEFAULT now(),
    issues text[],
    deleted timestamp without time zone,
    source_taxon_key integer
);


ALTER TABLE public.name_usage OWNER TO postgres;

--
-- Name: kname; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW kname AS
 SELECT u.id,
    u.rank,
    n.scientific_name,
    u.is_synonym,
    u.status,
    u.origin,
    kn.scientific_name AS kingdom,
    u.dataset_key
   FROM (((name_usage u
     JOIN name n ON ((u.name_fk = n.id)))
     LEFT JOIN name_usage ku ON ((u.kingdom_fk = ku.id)))
     LEFT JOIN name kn ON ((ku.name_fk = kn.id)));


ALTER TABLE public.kname OWNER TO postgres;

--
-- Name: literature; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE literature (
    id integer NOT NULL,
    usage_fk integer NOT NULL,
    citation_fk integer NOT NULL,
    type text,
    remarks text
);


ALTER TABLE public.literature OWNER TO postgres;

--
-- Name: literature_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE literature_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.literature_id_seq OWNER TO postgres;

--
-- Name: literature_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE literature_id_seq OWNED BY literature.id;


--
-- Name: media; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE media (
    id integer NOT NULL,
    usage_fk integer,
    type media_type,
    format text,
    identifier text,
    "references" text,
    title text,
    description text,
    audience text,
    created timestamp without time zone,
    creator text,
    contributor text,
    publisher text,
    license text,
    rights_holder text,
    source_fk integer
);


ALTER TABLE public.media OWNER TO postgres;

--
-- Name: media_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE media_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.media_id_seq OWNER TO postgres;

--
-- Name: media_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE media_id_seq OWNED BY media.id;


--
-- Name: name_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE name_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.name_id_seq OWNER TO postgres;

--
-- Name: name_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE name_id_seq OWNED BY name.id;


--
-- Name: name_usage_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE name_usage_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.name_usage_id_seq OWNER TO postgres;

--
-- Name: name_usage_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE name_usage_id_seq OWNED BY name_usage.id;


--
-- Name: name_usage_metrics; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE name_usage_metrics (
    usage_fk integer NOT NULL,
    dataset_key uuid NOT NULL,
    count_children integer DEFAULT 0,
    count_synonyms integer DEFAULT 0,
    count_p integer DEFAULT 0,
    count_c integer DEFAULT 0,
    count_o integer DEFAULT 0,
    count_f integer DEFAULT 0,
    count_g integer DEFAULT 0,
    count_sg integer DEFAULT 0,
    count_s integer DEFAULT 0,
    modified timestamp without time zone DEFAULT now()
);


ALTER TABLE public.name_usage_metrics OWNER TO postgres;

--
-- Name: nub; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW nub AS
 SELECT u.id,
    n.id AS nid,
    n.scientific_name,
    u.rank,
    u.status,
    u.origin,
    bu.id AS basionym_id,
    b.scientific_name AS basionym,
    pu.id AS parent_id,
    p.scientific_name AS parent,
    k.canonical_name AS kingdom,
    f.canonical_name AS family,
    (u.deleted IS NOT NULL) AS deleted,
    u.source_taxon_key AS source_id,
    u.constituent_key,
    u.issues
   FROM (((((((((name_usage u
     JOIN name n ON ((u.name_fk = n.id)))
     LEFT JOIN name_usage pu ON ((u.parent_fk = pu.id)))
     LEFT JOIN name p ON ((pu.name_fk = p.id)))
     LEFT JOIN name_usage ku ON ((u.kingdom_fk = ku.id)))
     LEFT JOIN name k ON ((ku.name_fk = k.id)))
     LEFT JOIN name_usage fu ON ((u.family_fk = fu.id)))
     LEFT JOIN name f ON ((fu.name_fk = f.id)))
     LEFT JOIN name_usage bu ON ((u.basionym_fk = bu.id)))
     LEFT JOIN name b ON ((bu.name_fk = b.id)))
  WHERE (u.dataset_key = nubkey());


ALTER TABLE public.nub OWNER TO postgres;

--
-- Name: nub_homonyms; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW nub_homonyms AS
 SELECT n1.canonical_name,
    u1.id AS id1,
    u1.rank AS rank1,
    n1.scientific_name AS scientific_name1,
    u1.is_synonym AS is_synonym1,
    u1.status AS status1,
    u1.kingdom_fk AS kingdom1,
    u1.phylum_fk AS phylum1,
    u1.class_fk AS class1,
    u1.order_fk AS order1,
    u1.family_fk AS family1,
    u1.genus_fk AS genus1,
    u2.id AS id2,
    u2.rank AS rank2,
    n2.scientific_name AS scientific_name2,
    u2.is_synonym AS is_synonym2,
    u2.status AS status2,
    u2.kingdom_fk AS kingdom2,
    u2.phylum_fk AS phylum2,
    u2.class_fk AS class2,
    u2.order_fk AS order2,
    u2.family_fk AS family2,
    u2.genus_fk AS genus2
   FROM (((name_usage u1
     JOIN name n1 ON ((u1.name_fk = n1.id)))
     JOIN name n2 ON ((n1.canonical_name = n2.canonical_name)))
     JOIN name_usage u2 ON (((u2.name_fk = n2.id) AND (u2.id <> u1.id))))
  WHERE ((((u1.dataset_key = nubkey()) AND (u2.dataset_key = nubkey())) AND (u1.deleted IS NULL)) AND (u2.deleted IS NULL));


ALTER TABLE public.nub_homonyms OWNER TO postgres;

--
-- Name: nub_rel; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE nub_rel (
    usage_fk integer NOT NULL,
    nub_fk integer NOT NULL,
    dataset_key uuid NOT NULL
);


ALTER TABLE public.nub_rel OWNER TO postgres;

--
-- Name: raw_usage; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE raw_usage (
    usage_fk integer NOT NULL,
    dataset_key uuid NOT NULL,
    json text
);


ALTER TABLE public.raw_usage OWNER TO postgres;

--
-- Name: species_info; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE species_info (
    id integer NOT NULL,
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
    freshwater boolean,
    source_fk integer
);


ALTER TABLE public.species_info OWNER TO postgres;

--
-- Name: species_info_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE species_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.species_info_id_seq OWNER TO postgres;

--
-- Name: species_info_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE species_info_id_seq OWNED BY species_info.id;


--
-- Name: typification; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE typification (
    id integer NOT NULL,
    usage_fk integer,
    rank rank,
    scientific_name text,
    designated_by text,
    designation_type type_designation_type,
    source_fk integer
);


ALTER TABLE public.typification OWNER TO postgres;

--
-- Name: typification_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE typification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.typification_id_seq OWNER TO postgres;

--
-- Name: typification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE typification_id_seq OWNED BY typification.id;


--
-- Name: v_backbone; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW v_backbone AS
 SELECT u.id,
    u.parent_fk,
    u.basionym_fk,
    u.is_synonym,
    u.status,
    u.rank,
    u.nom_status,
    u.constituent_key,
    u.origin,
    u.source_taxon_key,
    u.kingdom_fk,
    u.phylum_fk,
    u.class_fk,
    u.order_fk,
    u.family_fk,
    u.genus_fk,
    u.species_fk,
    n.id AS name_id,
    n.scientific_name,
    n.canonical_name,
    n.genus_or_above,
    n.specific_epithet,
    n.infra_specific_epithet,
    n.notho_type,
    n.authorship,
    n.year,
    n.bracket_authorship,
    n.bracket_year,
    cpi.citation AS name_published_in,
    u.issues
   FROM ((name_usage u
     JOIN name n ON ((u.name_fk = n.id)))
     LEFT JOIN citation cpi ON ((u.name_published_in_fk = cpi.id)))
  WHERE ((u.dataset_key = nubkey()) AND (u.deleted IS NULL));


ALTER TABLE public.v_backbone OWNER TO postgres;

--
-- Name: vernacular_name; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE vernacular_name (
    id integer NOT NULL,
    usage_fk integer NOT NULL,
    name text,
    language character(2),
    preferred boolean DEFAULT false,
    sex sex,
    life_stage life_stage,
    area text,
    country character(2),
    source_fk integer,
    plural boolean
);


ALTER TABLE public.vernacular_name OWNER TO postgres;

--
-- Name: vernacular_name_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE vernacular_name_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.vernacular_name_id_seq OWNER TO postgres;

--
-- Name: vernacular_name_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE vernacular_name_id_seq OWNED BY vernacular_name.id;


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY citation ALTER COLUMN id SET DEFAULT nextval('citation_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY dataset_metrics ALTER COLUMN id SET DEFAULT nextval('dataset_metrics_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY description ALTER COLUMN id SET DEFAULT nextval('description_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY distribution ALTER COLUMN id SET DEFAULT nextval('distribution_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY identifier ALTER COLUMN id SET DEFAULT nextval('identifier_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY literature ALTER COLUMN id SET DEFAULT nextval('literature_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY media ALTER COLUMN id SET DEFAULT nextval('media_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY name ALTER COLUMN id SET DEFAULT nextval('name_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY name_usage ALTER COLUMN id SET DEFAULT nextval('name_usage_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY species_info ALTER COLUMN id SET DEFAULT nextval('species_info_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY typification ALTER COLUMN id SET DEFAULT nextval('typification_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY vernacular_name ALTER COLUMN id SET DEFAULT nextval('vernacular_name_id_seq'::regclass);


--
-- Data for Name: citation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY citation (id, citation, link, identifier) FROM stdin;
\.


--
-- Name: citation_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('citation_id_seq', 1, false);


--
-- Data for Name: col_annotation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY col_annotation (nub_fk, gsd, annotated_name, rejected, status, note) FROM stdin;
\.


--
-- Data for Name: dataset; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY dataset (key, title) FROM stdin;
\.


--
-- Data for Name: dataset_metrics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY dataset_metrics (id, dataset_key, count_usages, count_synonyms, count_names, count_col, count_nub, count_by_rank, count_by_kingdom, count_by_origin, count_vernacular_by_lang, count_extensions, count_other, downloaded, created, latest, count_by_issue, count_by_constituent) FROM stdin;
\.


--
-- Name: dataset_metrics_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('dataset_metrics_id_seq', 1, false);


--
-- Data for Name: description; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY description (id, usage_fk, description, type, language, source_fk, creator, contributor, license) FROM stdin;
\.


--
-- Name: description_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('description_id_seq', 1, false);


--
-- Data for Name: distribution; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY distribution (id, usage_fk, location_id, locality, country, occurrence_status, threat_status, establishment_means, appendix_cites, start_day_of_year, end_day_of_year, start_year, end_year, source_fk, remarks, temporal, life_stage) FROM stdin;
\.


--
-- Name: distribution_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('distribution_id_seq', 1, false);


--
-- Data for Name: identifier; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY identifier (id, usage_fk, type, identifier, title) FROM stdin;
\.


--
-- Name: identifier_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('identifier_id_seq', 1, false);


--
-- Data for Name: literature; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY literature (id, usage_fk, citation_fk, type, remarks) FROM stdin;
\.


--
-- Name: literature_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('literature_id_seq', 1, false);


--
-- Data for Name: media; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY media (id, usage_fk, type, format, identifier, "references", title, description, audience, created, creator, contributor, publisher, license, rights_holder, source_fk) FROM stdin;
\.


--
-- Name: media_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('media_id_seq', 1, false);


--
-- Data for Name: name; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY name (id, scientific_name, canonical_name, type, genus_or_above, infra_generic, specific_epithet, infra_specific_epithet, cultivar_epithet, notho_type, authors_parsed, authorship, year, year_int, bracket_authorship, bracket_year, nom_status, sensu, remarks, modified, rank, parsed) FROM stdin;
\.


--
-- Name: name_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('name_id_seq', 1, false);


--
-- Data for Name: name_usage; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY name_usage (id, dataset_key, constituent_key, name_fk, rank, parent_fk, is_synonym, pp_synonym_fk, status, nom_status, basionym_fk, kingdom_fk, phylum_fk, class_fk, order_fk, family_fk, genus_fk, subgenus_fk, species_fk, name_published_in_fk, according_to_fk, origin, remarks, modified, "references", taxon_id, num_descendants, last_interpreted, issues, deleted, source_taxon_key) FROM stdin;
\.


--
-- Name: name_usage_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('name_usage_id_seq', 1, false);


--
-- Data for Name: name_usage_metrics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY name_usage_metrics (usage_fk, dataset_key, count_children, count_synonyms, count_p, count_c, count_o, count_f, count_g, count_sg, count_s, modified) FROM stdin;
\.


--
-- Data for Name: nub_rel; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY nub_rel (usage_fk, nub_fk, dataset_key) FROM stdin;
\.


--
-- Data for Name: raw_usage; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY raw_usage (usage_fk, dataset_key, json) FROM stdin;
\.


--
-- Data for Name: species_info; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY species_info (id, usage_fk, marine, terrestrial, extinct, hybrid, living_period, age_in_days, size_in_millimeter, mass_in_gram, life_form, habitat, freshwater, source_fk) FROM stdin;
\.


--
-- Name: species_info_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('species_info_id_seq', 1, false);


--
-- Data for Name: typification; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY typification (id, usage_fk, rank, scientific_name, designated_by, designation_type, source_fk) FROM stdin;
\.


--
-- Name: typification_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('typification_id_seq', 1, false);


--
-- Data for Name: vernacular_name; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY vernacular_name (id, usage_fk, name, language, preferred, sex, life_stage, area, country, source_fk, plural) FROM stdin;
\.


--
-- Name: vernacular_name_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('vernacular_name_id_seq', 1, false);


--
-- Name: citation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY citation
    ADD CONSTRAINT citation_pkey PRIMARY KEY (id);


--
-- Name: col_annotation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY col_annotation
    ADD CONSTRAINT col_annotation_pkey PRIMARY KEY (nub_fk);


--
-- Name: dataset_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY dataset_metrics
    ADD CONSTRAINT dataset_metrics_pkey PRIMARY KEY (id);


--
-- Name: dataset_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY dataset
    ADD CONSTRAINT dataset_pkey PRIMARY KEY (key);


--
-- Name: description_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY description
    ADD CONSTRAINT description_pkey PRIMARY KEY (id);


--
-- Name: distribution_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY distribution
    ADD CONSTRAINT distribution_pkey PRIMARY KEY (id);


--
-- Name: identifier_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY identifier
    ADD CONSTRAINT identifier_pkey PRIMARY KEY (id);


--
-- Name: literature_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY literature
    ADD CONSTRAINT literature_pkey PRIMARY KEY (id);


--
-- Name: media_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY media
    ADD CONSTRAINT media_pkey PRIMARY KEY (id);


--
-- Name: name_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY name
    ADD CONSTRAINT name_pkey PRIMARY KEY (id);


--
-- Name: name_scientific_name_rank_key; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY name
    ADD CONSTRAINT name_scientific_name_rank_key UNIQUE (scientific_name, rank);


--
-- Name: name_usage_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY name_usage_metrics
    ADD CONSTRAINT name_usage_metrics_pkey PRIMARY KEY (usage_fk);


--
-- Name: name_usage_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY name_usage
    ADD CONSTRAINT name_usage_pkey PRIMARY KEY (id);


--
-- Name: nub_rel_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY nub_rel
    ADD CONSTRAINT nub_rel_pkey PRIMARY KEY (usage_fk);


--
-- Name: raw_usage_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY raw_usage
    ADD CONSTRAINT raw_usage_pkey PRIMARY KEY (usage_fk);


--
-- Name: species_info_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY species_info
    ADD CONSTRAINT species_info_pkey PRIMARY KEY (id);


--
-- Name: typification_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY typification
    ADD CONSTRAINT typification_pkey PRIMARY KEY (id);


--
-- Name: vernacular_name_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY vernacular_name
    ADD CONSTRAINT vernacular_name_pkey PRIMARY KEY (id);


--
-- Name: citation_md5_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE UNIQUE INDEX citation_md5_idx ON citation USING btree (md5(citation));


--
-- Name: dataset_metrics_dataset_key_created_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX dataset_metrics_dataset_key_created_idx ON dataset_metrics USING btree (dataset_key, created DESC);


--
-- Name: dataset_metrics_dataset_key_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE UNIQUE INDEX dataset_metrics_dataset_key_idx ON dataset_metrics USING btree (dataset_key) WHERE latest;


--
-- Name: description_usage_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX description_usage_fk_idx ON description USING btree (usage_fk);


--
-- Name: distribution_usage_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX distribution_usage_fk_idx ON distribution USING btree (usage_fk);


--
-- Name: identifier_identifier_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE UNIQUE INDEX identifier_identifier_idx ON identifier USING btree (identifier);


--
-- Name: identifier_type_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX identifier_type_idx ON identifier USING btree (type);


--
-- Name: identifier_usage_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX identifier_usage_fk_idx ON identifier USING btree (usage_fk);


--
-- Name: literature_type_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX literature_type_idx ON literature USING btree (type);


--
-- Name: literature_usage_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX literature_usage_fk_idx ON literature USING btree (usage_fk);


--
-- Name: media_usage_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX media_usage_fk_idx ON media USING btree (usage_fk);


--
-- Name: name_lower_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_lower_idx ON name USING btree (lower(canonical_name));


--
-- Name: name_notho_type_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_notho_type_idx ON name USING btree (notho_type);


--
-- Name: name_type_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_type_idx ON name USING btree (type);


--
-- Name: name_usage_basionym_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_basionym_fk_idx ON name_usage USING btree (basionym_fk) WHERE (deleted IS NULL);


--
-- Name: name_usage_dataset_key_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_dataset_key_idx ON name_usage USING btree (dataset_key) WHERE (deleted IS NULL);


--
-- Name: name_usage_dataset_key_idx1; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_dataset_key_idx1 ON name_usage USING btree (dataset_key) WHERE (((deleted IS NULL) AND (parent_fk IS NULL)) AND (is_synonym = false));


--
-- Name: name_usage_dataset_key_last_interpreted_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_dataset_key_last_interpreted_idx ON name_usage USING btree (dataset_key, last_interpreted) WHERE (deleted IS NULL);


--
-- Name: name_usage_dataset_key_taxon_id_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_dataset_key_taxon_id_idx ON name_usage USING btree (dataset_key, taxon_id);


--
-- Name: name_usage_deleted_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_deleted_idx ON name_usage USING btree (deleted) WHERE (deleted IS NULL);


--
-- Name: name_usage_is_synonym_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_is_synonym_idx ON name_usage USING btree (is_synonym);


--
-- Name: name_usage_name_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_name_fk_idx ON name_usage USING btree (name_fk);


--
-- Name: name_usage_origin_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_origin_idx ON name_usage USING btree (origin);


--
-- Name: name_usage_parent_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_parent_fk_idx ON name_usage USING btree (parent_fk) WHERE ((deleted IS NULL) AND (is_synonym = false));


--
-- Name: name_usage_parent_fk_idx1; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_parent_fk_idx1 ON name_usage USING btree (parent_fk) WHERE ((deleted IS NULL) AND (is_synonym = true));


--
-- Name: name_usage_pp_synonym_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_pp_synonym_fk_idx ON name_usage USING btree (pp_synonym_fk);


--
-- Name: name_usage_rank_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_rank_idx ON name_usage USING btree (rank);


--
-- Name: name_usage_status_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX name_usage_status_idx ON name_usage USING btree (status);


--
-- Name: nub_rel_dataset_key_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX nub_rel_dataset_key_idx ON nub_rel USING btree (dataset_key);


--
-- Name: nub_rel_nub_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX nub_rel_nub_fk_idx ON nub_rel USING btree (nub_fk);


--
-- Name: nub_rel_nub_fk_usage_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX nub_rel_nub_fk_usage_fk_idx ON nub_rel USING btree (nub_fk, usage_fk) WHERE (dataset_key <> ALL (ARRAY['cbb6498e-8927-405a-916b-576d00a6289b'::uuid, 'cd9fa1dd-d29f-47c6-bac1-31245a9f08e9'::uuid, '16c3f9cb-4b19-4553-ac8e-ebb90003aa02'::uuid]));


--
-- Name: species_info_usage_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX species_info_usage_fk_idx ON species_info USING btree (usage_fk);


--
-- Name: typification_usage_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX typification_usage_fk_idx ON typification USING btree (usage_fk);


--
-- Name: vernacular_name_lower_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX vernacular_name_lower_idx ON vernacular_name USING btree (lower(name));


--
-- Name: vernacular_name_usage_fk_idx; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX vernacular_name_usage_fk_idx ON vernacular_name USING btree (usage_fk);


--
-- Name: description_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY description
    ADD CONSTRAINT description_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: description_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY description
    ADD CONSTRAINT description_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: distribution_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY distribution
    ADD CONSTRAINT distribution_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: distribution_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY distribution
    ADD CONSTRAINT distribution_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: identifier_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY identifier
    ADD CONSTRAINT identifier_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: literature_citation_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY literature
    ADD CONSTRAINT literature_citation_fk_fkey FOREIGN KEY (citation_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: literature_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY literature
    ADD CONSTRAINT literature_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: media_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY media
    ADD CONSTRAINT media_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: media_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY media
    ADD CONSTRAINT media_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: name_usage_according_to_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY name_usage
    ADD CONSTRAINT name_usage_according_to_fk_fkey FOREIGN KEY (according_to_fk) REFERENCES citation(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: name_usage_metrics_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY name_usage_metrics
    ADD CONSTRAINT name_usage_metrics_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: name_usage_name_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY name_usage
    ADD CONSTRAINT name_usage_name_fk_fkey FOREIGN KEY (name_fk) REFERENCES name(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: name_usage_name_published_in_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY name_usage
    ADD CONSTRAINT name_usage_name_published_in_fk_fkey FOREIGN KEY (name_published_in_fk) REFERENCES citation(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nub_rel_nub_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY nub_rel
    ADD CONSTRAINT nub_rel_nub_fk_fkey FOREIGN KEY (nub_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nub_rel_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY nub_rel
    ADD CONSTRAINT nub_rel_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: raw_usage_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY raw_usage
    ADD CONSTRAINT raw_usage_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: species_info_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY species_info
    ADD CONSTRAINT species_info_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: species_info_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY species_info
    ADD CONSTRAINT species_info_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: typification_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY typification
    ADD CONSTRAINT typification_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: typification_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY typification
    ADD CONSTRAINT typification_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: vernacular_name_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY vernacular_name
    ADD CONSTRAINT vernacular_name_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: vernacular_name_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY vernacular_name
    ADD CONSTRAINT vernacular_name_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

