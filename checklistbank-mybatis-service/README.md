ChecklistBank MyBatis Service
-----------------------------

This service _only_ supports PostgreSQL. To run the integration tests you must have a PostgreSQL database available.

The database being used needs the "plpgsql" language and hstore extension enabled.
You can create a db from scratch or update to the latest version using the maven liquiabse plugin like this:

    mvn -P YOUR_SETTINGS liquibase:update

Alternatively there is a Makefile that one can use to generate a full SQL DDL file from all liquibase changesets.
You will need a local postgres db where the script generates an up to date clb database and then dumps it to a sql file.
Use it with make like this to generate the schema.sql file hosted in the main [docs](../docs/schema.sql) folder:

    make ddl

    
