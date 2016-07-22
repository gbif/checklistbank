---
--- script that adds 4 foreign tables from the registry:
---
--- dataset
--- dataset_endpoint
--- endpoint
--- publisher
---
-- --- requires a valid registry password!

CREATE SCHEMA registry;
CREATE SERVER registry FOREIGN DATA WRAPPER postgres_fdw OPTIONS (host 'localhost', dbname 'prod_b_registry', port '5432');
CREATE USER MAPPING FOR postgres SERVER registry OPTIONS (user 'registry', password 'PASSWORD');
CREATE USER MAPPING FOR clb SERVER registry OPTIONS (user 'registry', password 'PASSWORD');

CREATE FOREIGN TABLE registry.dataset (
    key UUID NOT NULL,
    title text,
    publishing_organization_key UUID,
    type text,
    homepage text,
    doi text,
    deleted timestamp
)
SERVER registry OPTIONS (schema_name 'public');


CREATE FOREIGN TABLE registry.endpoint (
    key integer NOT NULL,
    type text,
    url text
)
SERVER registry OPTIONS (schema_name 'public');


CREATE FOREIGN TABLE registry.dataset_endpoint (
    dataset_key UUID NOT NULL,
    endpoint_key integer NOT NULL
)
SERVER registry OPTIONS (schema_name 'public');


CREATE FOREIGN TABLE registry.publisher (
    key UUID NOT NULL,
    title text,
    country text,
    logo_url text
)
SERVER registry OPTIONS (schema_name 'public', table_name 'organization');
