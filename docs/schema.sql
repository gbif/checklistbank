--
-- PostgreSQL database dump
--

-- Dumped from database version 11.0
-- Dumped by pg_dump version 11.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: hstore; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;


--
-- Name: EXTENSION hstore; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION hstore IS 'data type for storing sets of (key, value) pairs';


--
-- Name: cites_appendix; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.cites_appendix AS ENUM (
    'I',
    'II',
    'III'
);


ALTER TYPE public.cites_appendix OWNER TO markus;

--
-- Name: establishment_means; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.establishment_means AS ENUM (
    'NATIVE',
    'INTRODUCED',
    'NATURALISED',
    'INVASIVE',
    'MANAGED',
    'UNCERTAIN'
);


ALTER TYPE public.establishment_means OWNER TO markus;

--
-- Name: identifier_type; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.identifier_type AS ENUM (
    'URL',
    'LSID',
    'HANDLER',
    'DOI',
    'UUID',
    'FTP',
    'URI',
    'UNKNOWN'
);


ALTER TYPE public.identifier_type OWNER TO markus;

--
-- Name: kingdom; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.kingdom AS ENUM (
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


ALTER TYPE public.kingdom OWNER TO markus;

--
-- Name: life_stage; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.life_stage AS ENUM (
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


ALTER TYPE public.life_stage OWNER TO markus;

--
-- Name: media_type; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.media_type AS ENUM (
    'StillImage',
    'MovingImage',
    'Sound'
);


ALTER TYPE public.media_type OWNER TO markus;

--
-- Name: name_part; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.name_part AS ENUM (
    'GENERIC',
    'INFRAGENERIC',
    'SPECIFIC',
    'INFRASPECIFIC'
);


ALTER TYPE public.name_part OWNER TO markus;

--
-- Name: name_type; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.name_type AS ENUM (
    'SCIENTIFIC',
    'VIRUS',
    'HYBRID',
    'INFORMAL',
    'CULTIVAR',
    'CANDIDATUS',
    'OTU',
    'DOUBTFUL',
    'PLACEHOLDER',
    'NO_NAME',
    'BLACKLISTED'
);


ALTER TYPE public.name_type OWNER TO markus;

--
-- Name: nomenclatural_status; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.nomenclatural_status AS ENUM (
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


ALTER TYPE public.nomenclatural_status OWNER TO markus;

--
-- Name: occurrence_status; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.occurrence_status AS ENUM (
    'PRESENT',
    'COMMON',
    'RARE',
    'IRREGULAR',
    'DOUBTFUL',
    'EXCLUDED',
    'ABSENT'
);


ALTER TYPE public.occurrence_status OWNER TO markus;

--
-- Name: origin_type; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.origin_type AS ENUM (
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


ALTER TYPE public.origin_type OWNER TO markus;

--
-- Name: rank; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.rank AS ENUM (
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


ALTER TYPE public.rank OWNER TO markus;

--
-- Name: sex; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.sex AS ENUM (
    'NONE',
    'MALE',
    'FEMALE',
    'HERMAPHRODITE'
);


ALTER TYPE public.sex OWNER TO markus;

--
-- Name: taxonomic_status; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.taxonomic_status AS ENUM (
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


ALTER TYPE public.taxonomic_status OWNER TO markus;

--
-- Name: threat_status; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.threat_status AS ENUM (
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


ALTER TYPE public.threat_status OWNER TO markus;

--
-- Name: type_designation_type; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.type_designation_type AS ENUM (
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


ALTER TYPE public.type_designation_type OWNER TO markus;

--
-- Name: type_status; Type: TYPE; Schema: public; Owner: markus
--

CREATE TYPE public.type_status AS ENUM (
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


ALTER TYPE public.type_status OWNER TO markus;

--
-- Name: authorship_full(character varying, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: markus
--

CREATE FUNCTION public.authorship_full(authorship character varying, year character varying, authorship_basionym character varying, year_basionym character varying) RETURNS text
    LANGUAGE plpgsql IMMUTABLE
    AS $$ BEGIN RETURN (COALESCE(authorship, ''::character varying)::text || CASE WHEN year IS NOT NULL THEN ', '::text || year::text ELSE ''::text END) || CASE WHEN authorship_basionym IS NOT NULL OR year_basionym IS NOT NULL THEN (' ('::text || COALESCE((authorship_basionym::text || ', '::text) || year_basionym::text, authorship_basionym::text, year_basionym::text)) || ')'::text ELSE ''::text END; END; $$;


ALTER FUNCTION public.authorship_full(authorship character varying, year character varying, authorship_basionym character varying, year_basionym character varying) OWNER TO markus;

--
-- Name: colkey(); Type: FUNCTION; Schema: public; Owner: markus
--

CREATE FUNCTION public.colkey() RETURNS uuid
    LANGUAGE sql IMMUTABLE
    AS $$
        SELECT '7ddf754f-d193-4cc9-b351-99906754a03b'::uuid
      $$;


ALTER FUNCTION public.colkey() OWNER TO markus;

--
-- Name: groupinfraspecificranks(public.rank); Type: FUNCTION; Schema: public; Owner: markus
--

CREATE FUNCTION public.groupinfraspecificranks(rnk public.rank) RETURNS public.rank
    LANGUAGE sql
    AS $$
      SELECT CASE WHEN rnk <= 'SPECIES'::rank THEN rnk
        ELSE 'INFRASPECIFIC_NAME'::rank
      END
      $$;


ALTER FUNCTION public.groupinfraspecificranks(rnk public.rank) OWNER TO markus;

--
-- Name: nubkey(); Type: FUNCTION; Schema: public; Owner: markus
--

CREATE FUNCTION public.nubkey() RETURNS uuid
    LANGUAGE sql IMMUTABLE
    AS $$
        SELECT 'd7dddbf4-2cf0-4f39-9b2a-bb099caae36c'::uuid
      $$;


ALTER FUNCTION public.nubkey() OWNER TO markus;

--
-- Name: plazikey(); Type: FUNCTION; Schema: public; Owner: markus
--

CREATE FUNCTION public.plazikey() RETURNS uuid
    LANGUAGE sql IMMUTABLE
    AS $$
        SELECT '7ce8aef0-9e92-11dc-8738-b8a03c50a862'::uuid
      $$;


ALTER FUNCTION public.plazikey() OWNER TO markus;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: citation; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.citation (
    id integer NOT NULL,
    citation text,
    link text,
    identifier text
);


ALTER TABLE public.citation OWNER TO markus;

--
-- Name: citation_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.citation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.citation_id_seq OWNER TO markus;

--
-- Name: citation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.citation_id_seq OWNED BY public.citation.id;


--
-- Name: dataset; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.dataset (
    key uuid NOT NULL,
    title text,
    parent uuid,
    publisher uuid
);


ALTER TABLE public.dataset OWNER TO markus;

--
-- Name: dataset_metrics; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.dataset_metrics (
    id integer NOT NULL,
    dataset_key uuid NOT NULL,
    count_usages integer DEFAULT 0,
    count_synonyms integer DEFAULT 0,
    count_names integer DEFAULT 0,
    count_col integer DEFAULT 0,
    count_nub integer DEFAULT 0,
    count_by_rank public.hstore,
    count_by_kingdom public.hstore,
    count_by_origin public.hstore,
    count_vernacular_by_lang public.hstore,
    count_extensions public.hstore,
    count_other public.hstore,
    downloaded timestamp without time zone,
    created timestamp without time zone DEFAULT now(),
    latest boolean DEFAULT true,
    count_by_issue public.hstore,
    count_by_constituent public.hstore
);


ALTER TABLE public.dataset_metrics OWNER TO markus;

--
-- Name: dataset_metrics_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.dataset_metrics_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataset_metrics_id_seq OWNER TO markus;

--
-- Name: dataset_metrics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.dataset_metrics_id_seq OWNED BY public.dataset_metrics.id;


--
-- Name: description; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.description (
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


ALTER TABLE public.description OWNER TO markus;

--
-- Name: description_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.description_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.description_id_seq OWNER TO markus;

--
-- Name: description_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.description_id_seq OWNED BY public.description.id;


--
-- Name: distribution; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.distribution (
    id integer NOT NULL,
    usage_fk integer,
    location_id text,
    locality text,
    country character(2),
    occurrence_status public.occurrence_status,
    threat_status public.threat_status,
    establishment_means public.establishment_means,
    appendix_cites public.cites_appendix,
    start_day_of_year integer,
    end_day_of_year integer,
    start_year integer,
    end_year integer,
    source_fk integer,
    remarks text,
    temporal text,
    life_stage public.life_stage
);


ALTER TABLE public.distribution OWNER TO markus;

--
-- Name: distribution_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.distribution_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.distribution_id_seq OWNER TO markus;

--
-- Name: distribution_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.distribution_id_seq OWNED BY public.distribution.id;


--
-- Name: identifier; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.identifier (
    id integer NOT NULL,
    usage_fk integer,
    type public.identifier_type NOT NULL,
    identifier text,
    title text
);


ALTER TABLE public.identifier OWNER TO markus;

--
-- Name: identifier_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.identifier_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.identifier_id_seq OWNER TO markus;

--
-- Name: identifier_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.identifier_id_seq OWNED BY public.identifier.id;


--
-- Name: name; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.name (
    id integer NOT NULL,
    scientific_name text,
    canonical_name text,
    type public.name_type NOT NULL,
    genus_or_above text,
    infra_generic text,
    specific_epithet text,
    infra_specific_epithet text,
    cultivar_epithet text,
    notho_type public.name_part,
    parsed_partially boolean DEFAULT false NOT NULL,
    authorship text,
    year text,
    year_int integer,
    bracket_authorship text,
    bracket_year text,
    nom_status text,
    sensu text,
    remarks text,
    modified timestamp without time zone DEFAULT now(),
    rank public.rank,
    parsed boolean,
    strain text
);


ALTER TABLE public.name OWNER TO markus;

--
-- Name: name_usage; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.name_usage (
    id integer NOT NULL,
    dataset_key uuid NOT NULL,
    constituent_key uuid,
    name_fk integer NOT NULL,
    rank public.rank,
    parent_fk integer,
    is_synonym boolean NOT NULL,
    pp_synonym_fk integer,
    status public.taxonomic_status,
    nom_status public.nomenclatural_status[],
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
    origin public.origin_type,
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


ALTER TABLE public.name_usage OWNER TO markus;

--
-- Name: kname; Type: VIEW; Schema: public; Owner: markus
--

CREATE VIEW public.kname AS
 SELECT u.id,
    u.rank,
    n.scientific_name,
    u.is_synonym,
    u.status,
    u.origin,
    kn.scientific_name AS kingdom,
    u.dataset_key
   FROM (((public.name_usage u
     JOIN public.name n ON ((u.name_fk = n.id)))
     LEFT JOIN public.name_usage ku ON ((u.kingdom_fk = ku.id)))
     LEFT JOIN public.name kn ON ((ku.name_fk = kn.id)));


ALTER TABLE public.kname OWNER TO markus;

--
-- Name: literature; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.literature (
    id integer NOT NULL,
    usage_fk integer NOT NULL,
    citation_fk integer NOT NULL,
    type text,
    remarks text
);


ALTER TABLE public.literature OWNER TO markus;

--
-- Name: literature_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.literature_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.literature_id_seq OWNER TO markus;

--
-- Name: literature_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.literature_id_seq OWNED BY public.literature.id;


--
-- Name: media; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.media (
    id integer NOT NULL,
    usage_fk integer,
    type public.media_type,
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


ALTER TABLE public.media OWNER TO markus;

--
-- Name: media_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.media_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.media_id_seq OWNER TO markus;

--
-- Name: media_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.media_id_seq OWNED BY public.media.id;


--
-- Name: name_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.name_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.name_id_seq OWNER TO markus;

--
-- Name: name_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.name_id_seq OWNED BY public.name.id;


--
-- Name: name_usage_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.name_usage_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.name_usage_id_seq OWNER TO markus;

--
-- Name: name_usage_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.name_usage_id_seq OWNED BY public.name_usage.id;


--
-- Name: name_usage_metrics; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.name_usage_metrics (
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


ALTER TABLE public.name_usage_metrics OWNER TO markus;

--
-- Name: nub; Type: VIEW; Schema: public; Owner: markus
--

CREATE VIEW public.nub AS
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
   FROM (((((((((public.name_usage u
     JOIN public.name n ON ((u.name_fk = n.id)))
     LEFT JOIN public.name_usage pu ON ((u.parent_fk = pu.id)))
     LEFT JOIN public.name p ON ((pu.name_fk = p.id)))
     LEFT JOIN public.name_usage ku ON ((u.kingdom_fk = ku.id)))
     LEFT JOIN public.name k ON ((ku.name_fk = k.id)))
     LEFT JOIN public.name_usage fu ON ((u.family_fk = fu.id)))
     LEFT JOIN public.name f ON ((fu.name_fk = f.id)))
     LEFT JOIN public.name_usage bu ON ((u.basionym_fk = bu.id)))
     LEFT JOIN public.name b ON ((bu.name_fk = b.id)))
  WHERE (u.dataset_key = public.nubkey());


ALTER TABLE public.nub OWNER TO markus;

--
-- Name: nub_homonyms; Type: VIEW; Schema: public; Owner: markus
--

CREATE VIEW public.nub_homonyms AS
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
   FROM (((public.name_usage u1
     JOIN public.name n1 ON ((u1.name_fk = n1.id)))
     JOIN public.name n2 ON ((n1.canonical_name = n2.canonical_name)))
     JOIN public.name_usage u2 ON (((u2.name_fk = n2.id) AND (u2.id <> u1.id))))
  WHERE ((u1.dataset_key = public.nubkey()) AND (u2.dataset_key = public.nubkey()) AND (u1.deleted IS NULL) AND (u2.deleted IS NULL));


ALTER TABLE public.nub_homonyms OWNER TO markus;

--
-- Name: nub_rel; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.nub_rel (
    usage_fk integer NOT NULL,
    nub_fk integer NOT NULL,
    dataset_key uuid NOT NULL
);


ALTER TABLE public.nub_rel OWNER TO markus;

--
-- Name: raw_usage; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.raw_usage (
    usage_fk integer NOT NULL,
    dataset_key uuid NOT NULL,
    json text
);


ALTER TABLE public.raw_usage OWNER TO markus;

--
-- Name: species_info; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.species_info (
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


ALTER TABLE public.species_info OWNER TO markus;

--
-- Name: species_info_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.species_info_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.species_info_id_seq OWNER TO markus;

--
-- Name: species_info_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.species_info_id_seq OWNED BY public.species_info.id;


--
-- Name: typification; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.typification (
    id integer NOT NULL,
    usage_fk integer,
    rank public.rank,
    scientific_name text,
    designated_by text,
    designation_type public.type_designation_type,
    source_fk integer
);


ALTER TABLE public.typification OWNER TO markus;

--
-- Name: typification_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.typification_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.typification_id_seq OWNER TO markus;

--
-- Name: typification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.typification_id_seq OWNED BY public.typification.id;


--
-- Name: v_backbone; Type: VIEW; Schema: public; Owner: markus
--

CREATE VIEW public.v_backbone AS
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
   FROM ((public.name_usage u
     JOIN public.name n ON ((u.name_fk = n.id)))
     LEFT JOIN public.citation cpi ON ((u.name_published_in_fk = cpi.id)))
  WHERE ((u.dataset_key = public.nubkey()) AND (u.deleted IS NULL));


ALTER TABLE public.v_backbone OWNER TO markus;

--
-- Name: v_nub_linnean; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_nub_linnean AS
 SELECT n.scientific_name,
    u.rank,
    u.id,
    u.status,
    u.source_taxon_key AS source,
    u.num_descendants,
    an.scientific_name AS accepted,
    fn.scientific_name AS family,
    orn.scientific_name AS "order",
    cn.scientific_name AS class,
    pn.scientific_name AS phylum,
    kn.scientific_name AS kingdom,
    n.canonical_name,
    a.id AS accepted_key,
    u.family_fk AS family_key,
    u.order_fk AS order_key,
    u.class_fk AS class_key,
    u.phylum_fk AS phylum_key,
    u.kingdom_fk AS kingdom_key
   FROM (((((((((((((public.name_usage u
     JOIN public.name n ON ((u.name_fk = n.id)))
     LEFT JOIN public.name_usage a ON (((u.parent_fk = a.id) AND u.is_synonym)))
     LEFT JOIN public.name an ON ((a.name_fk = an.id)))
     LEFT JOIN public.name_usage f ON ((u.family_fk = f.id)))
     LEFT JOIN public.name fn ON ((f.name_fk = fn.id)))
     LEFT JOIN public.name_usage o ON ((u.order_fk = o.id)))
     LEFT JOIN public.name orn ON ((o.name_fk = orn.id)))
     LEFT JOIN public.name_usage c ON ((u.class_fk = c.id)))
     LEFT JOIN public.name cn ON ((c.name_fk = cn.id)))
     LEFT JOIN public.name_usage p ON ((u.phylum_fk = p.id)))
     LEFT JOIN public.name pn ON ((p.name_fk = pn.id)))
     LEFT JOIN public.name_usage k ON ((u.kingdom_fk = k.id)))
     LEFT JOIN public.name kn ON ((k.name_fk = kn.id)))
  WHERE ((u.dataset_key = public.nubkey()) AND (u.deleted IS NULL));


ALTER TABLE public.v_nub_linnean OWNER TO postgres;

--
-- Name: vernacular_name; Type: TABLE; Schema: public; Owner: markus
--

CREATE TABLE public.vernacular_name (
    id integer NOT NULL,
    usage_fk integer NOT NULL,
    name text,
    language character(2),
    preferred boolean DEFAULT false,
    sex public.sex,
    life_stage public.life_stage,
    area text,
    country character(2),
    source_fk integer,
    plural boolean
);


ALTER TABLE public.vernacular_name OWNER TO markus;

--
-- Name: vernacular_name_id_seq; Type: SEQUENCE; Schema: public; Owner: markus
--

CREATE SEQUENCE public.vernacular_name_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.vernacular_name_id_seq OWNER TO markus;

--
-- Name: vernacular_name_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: markus
--

ALTER SEQUENCE public.vernacular_name_id_seq OWNED BY public.vernacular_name.id;


--
-- Name: citation id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.citation ALTER COLUMN id SET DEFAULT nextval('public.citation_id_seq'::regclass);


--
-- Name: dataset_metrics id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.dataset_metrics ALTER COLUMN id SET DEFAULT nextval('public.dataset_metrics_id_seq'::regclass);


--
-- Name: description id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.description ALTER COLUMN id SET DEFAULT nextval('public.description_id_seq'::regclass);


--
-- Name: distribution id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.distribution ALTER COLUMN id SET DEFAULT nextval('public.distribution_id_seq'::regclass);


--
-- Name: identifier id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.identifier ALTER COLUMN id SET DEFAULT nextval('public.identifier_id_seq'::regclass);


--
-- Name: literature id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.literature ALTER COLUMN id SET DEFAULT nextval('public.literature_id_seq'::regclass);


--
-- Name: media id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.media ALTER COLUMN id SET DEFAULT nextval('public.media_id_seq'::regclass);


--
-- Name: name id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.name ALTER COLUMN id SET DEFAULT nextval('public.name_id_seq'::regclass);


--
-- Name: name_usage id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.name_usage ALTER COLUMN id SET DEFAULT nextval('public.name_usage_id_seq'::regclass);


--
-- Name: species_info id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.species_info ALTER COLUMN id SET DEFAULT nextval('public.species_info_id_seq'::regclass);


--
-- Name: typification id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.typification ALTER COLUMN id SET DEFAULT nextval('public.typification_id_seq'::regclass);


--
-- Name: vernacular_name id; Type: DEFAULT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.vernacular_name ALTER COLUMN id SET DEFAULT nextval('public.vernacular_name_id_seq'::regclass);


--
-- Name: citation citation_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.citation
    ADD CONSTRAINT citation_pkey PRIMARY KEY (id);


--
-- Name: dataset_metrics dataset_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.dataset_metrics
    ADD CONSTRAINT dataset_metrics_pkey PRIMARY KEY (id);


--
-- Name: dataset dataset_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.dataset
    ADD CONSTRAINT dataset_pkey PRIMARY KEY (key);


--
-- Name: description description_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.description
    ADD CONSTRAINT description_pkey PRIMARY KEY (id);


--
-- Name: distribution distribution_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.distribution
    ADD CONSTRAINT distribution_pkey PRIMARY KEY (id);


--
-- Name: identifier identifier_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.identifier
    ADD CONSTRAINT identifier_pkey PRIMARY KEY (id);


--
-- Name: literature literature_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.literature
    ADD CONSTRAINT literature_pkey PRIMARY KEY (id);


--
-- Name: media media_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.media
    ADD CONSTRAINT media_pkey PRIMARY KEY (id);


--
-- Name: name name_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.name
    ADD CONSTRAINT name_pkey PRIMARY KEY (id);


--
-- Name: name name_scientific_name_rank_key; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.name
    ADD CONSTRAINT name_scientific_name_rank_key UNIQUE (scientific_name, rank);


--
-- Name: name_usage_metrics name_usage_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.name_usage_metrics
    ADD CONSTRAINT name_usage_metrics_pkey PRIMARY KEY (usage_fk);


--
-- Name: name_usage name_usage_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.name_usage
    ADD CONSTRAINT name_usage_pkey PRIMARY KEY (id);


--
-- Name: nub_rel nub_rel_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.nub_rel
    ADD CONSTRAINT nub_rel_pkey PRIMARY KEY (usage_fk);


--
-- Name: raw_usage raw_usage_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.raw_usage
    ADD CONSTRAINT raw_usage_pkey PRIMARY KEY (usage_fk);


--
-- Name: species_info species_info_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.species_info
    ADD CONSTRAINT species_info_pkey PRIMARY KEY (id);


--
-- Name: typification typification_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.typification
    ADD CONSTRAINT typification_pkey PRIMARY KEY (id);


--
-- Name: vernacular_name vernacular_name_pkey; Type: CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.vernacular_name
    ADD CONSTRAINT vernacular_name_pkey PRIMARY KEY (id);


--
-- Name: citation_md5_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE UNIQUE INDEX citation_md5_idx ON public.citation USING btree (md5(citation));


--
-- Name: dataset_metrics_dataset_key_created_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX dataset_metrics_dataset_key_created_idx ON public.dataset_metrics USING btree (dataset_key, created DESC);


--
-- Name: dataset_metrics_dataset_key_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE UNIQUE INDEX dataset_metrics_dataset_key_idx ON public.dataset_metrics USING btree (dataset_key) WHERE latest;


--
-- Name: description_usage_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX description_usage_fk_idx ON public.description USING btree (usage_fk);


--
-- Name: distribution_usage_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX distribution_usage_fk_idx ON public.distribution USING btree (usage_fk);


--
-- Name: identifier_identifier_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE UNIQUE INDEX identifier_identifier_idx ON public.identifier USING btree (identifier);


--
-- Name: identifier_type_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX identifier_type_idx ON public.identifier USING btree (type);


--
-- Name: identifier_usage_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX identifier_usage_fk_idx ON public.identifier USING btree (usage_fk);


--
-- Name: literature_type_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX literature_type_idx ON public.literature USING btree (type);


--
-- Name: literature_usage_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX literature_usage_fk_idx ON public.literature USING btree (usage_fk);


--
-- Name: media_usage_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX media_usage_fk_idx ON public.media USING btree (usage_fk);


--
-- Name: name_lower_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_lower_idx ON public.name USING btree (lower(canonical_name));


--
-- Name: name_notho_type_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_notho_type_idx ON public.name USING btree (notho_type);


--
-- Name: name_type_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_type_idx ON public.name USING btree (type);


--
-- Name: name_usage_basionym_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_basionym_fk_idx ON public.name_usage USING btree (basionym_fk) WHERE (deleted IS NULL);


--
-- Name: name_usage_dataset_key_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_dataset_key_idx ON public.name_usage USING btree (dataset_key) WHERE (deleted IS NULL);


--
-- Name: name_usage_dataset_key_idx1; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_dataset_key_idx1 ON public.name_usage USING btree (dataset_key) WHERE ((deleted IS NULL) AND (parent_fk IS NULL) AND (is_synonym = false));


--
-- Name: name_usage_dataset_key_last_interpreted_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_dataset_key_last_interpreted_idx ON public.name_usage USING btree (dataset_key, last_interpreted) WHERE (deleted IS NULL);


--
-- Name: name_usage_dataset_key_taxon_id_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_dataset_key_taxon_id_idx ON public.name_usage USING btree (dataset_key, taxon_id);


--
-- Name: name_usage_deleted_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_deleted_idx ON public.name_usage USING btree (deleted) WHERE (deleted IS NULL);


--
-- Name: name_usage_is_synonym_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_is_synonym_idx ON public.name_usage USING btree (is_synonym);


--
-- Name: name_usage_name_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_name_fk_idx ON public.name_usage USING btree (name_fk);


--
-- Name: name_usage_origin_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_origin_idx ON public.name_usage USING btree (origin);


--
-- Name: name_usage_parent_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_parent_fk_idx ON public.name_usage USING btree (parent_fk) WHERE ((deleted IS NULL) AND (is_synonym = false));


--
-- Name: name_usage_parent_fk_idx1; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_parent_fk_idx1 ON public.name_usage USING btree (parent_fk) WHERE ((deleted IS NULL) AND (is_synonym = true));


--
-- Name: name_usage_pp_synonym_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_pp_synonym_fk_idx ON public.name_usage USING btree (pp_synonym_fk);


--
-- Name: name_usage_rank_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_rank_idx ON public.name_usage USING btree (rank);


--
-- Name: name_usage_status_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX name_usage_status_idx ON public.name_usage USING btree (status);


--
-- Name: nub_rel_dataset_key_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX nub_rel_dataset_key_idx ON public.nub_rel USING btree (dataset_key);


--
-- Name: nub_rel_nub_fk_dataset_key_usage_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX nub_rel_nub_fk_dataset_key_usage_fk_idx ON public.nub_rel USING btree (nub_fk, dataset_key, usage_fk);


--
-- Name: nub_rel_nub_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX nub_rel_nub_fk_idx ON public.nub_rel USING btree (nub_fk);


--
-- Name: nub_rel_nub_fk_usage_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX nub_rel_nub_fk_usage_fk_idx ON public.nub_rel USING btree (nub_fk, usage_fk) WHERE (dataset_key <> ALL (ARRAY['cbb6498e-8927-405a-916b-576d00a6289b'::uuid, 'cd9fa1dd-d29f-47c6-bac1-31245a9f08e9'::uuid, '16c3f9cb-4b19-4553-ac8e-ebb90003aa02'::uuid]));


--
-- Name: species_info_usage_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX species_info_usage_fk_idx ON public.species_info USING btree (usage_fk);


--
-- Name: typification_usage_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX typification_usage_fk_idx ON public.typification USING btree (usage_fk);


--
-- Name: vernacular_name_lower_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX vernacular_name_lower_idx ON public.vernacular_name USING btree (lower(name));


--
-- Name: vernacular_name_usage_fk_idx; Type: INDEX; Schema: public; Owner: markus
--

CREATE INDEX vernacular_name_usage_fk_idx ON public.vernacular_name USING btree (usage_fk);


--
-- Name: description description_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.description
    ADD CONSTRAINT description_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES public.citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: description description_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.description
    ADD CONSTRAINT description_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: distribution distribution_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.distribution
    ADD CONSTRAINT distribution_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES public.citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: distribution distribution_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.distribution
    ADD CONSTRAINT distribution_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: identifier identifier_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.identifier
    ADD CONSTRAINT identifier_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: literature literature_citation_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.literature
    ADD CONSTRAINT literature_citation_fk_fkey FOREIGN KEY (citation_fk) REFERENCES public.citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: literature literature_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.literature
    ADD CONSTRAINT literature_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: media media_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.media
    ADD CONSTRAINT media_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES public.citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: media media_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.media
    ADD CONSTRAINT media_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: name_usage name_usage_according_to_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.name_usage
    ADD CONSTRAINT name_usage_according_to_fk_fkey FOREIGN KEY (according_to_fk) REFERENCES public.citation(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: name_usage_metrics name_usage_metrics_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.name_usage_metrics
    ADD CONSTRAINT name_usage_metrics_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: name_usage name_usage_name_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.name_usage
    ADD CONSTRAINT name_usage_name_fk_fkey FOREIGN KEY (name_fk) REFERENCES public.name(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: name_usage name_usage_name_published_in_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.name_usage
    ADD CONSTRAINT name_usage_name_published_in_fk_fkey FOREIGN KEY (name_published_in_fk) REFERENCES public.citation(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nub_rel nub_rel_nub_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.nub_rel
    ADD CONSTRAINT nub_rel_nub_fk_fkey FOREIGN KEY (nub_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: nub_rel nub_rel_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.nub_rel
    ADD CONSTRAINT nub_rel_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: raw_usage raw_usage_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.raw_usage
    ADD CONSTRAINT raw_usage_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: species_info species_info_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.species_info
    ADD CONSTRAINT species_info_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES public.citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: species_info species_info_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.species_info
    ADD CONSTRAINT species_info_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: typification typification_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.typification
    ADD CONSTRAINT typification_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES public.citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: typification typification_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.typification
    ADD CONSTRAINT typification_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: vernacular_name vernacular_name_source_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.vernacular_name
    ADD CONSTRAINT vernacular_name_source_fk_fkey FOREIGN KEY (source_fk) REFERENCES public.citation(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- Name: vernacular_name vernacular_name_usage_fk_fkey; Type: FK CONSTRAINT; Schema: public; Owner: markus
--

ALTER TABLE ONLY public.vernacular_name
    ADD CONSTRAINT vernacular_name_usage_fk_fkey FOREIGN KEY (usage_fk) REFERENCES public.name_usage(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;


--
-- PostgreSQL database dump complete
--

