ChecklistBank MyBatis Service
-----------------------------

This service _only_ supports PostgreSQL. To run the integration tests you must have a PostgreSQL database available.

The database being used needs the "plpgsql" language and hstore extension enabled.
You can create a db from scratch or update to the latest version using the maven liquiabse plugin like this:

    mvn -P YOUR_SETTINGS liquibase:update

Alternatively we provide a full postgres schema.sql file hosted in the main [docs](../docs/schema.sql) folder: to create a db manually.

    
