# GBIF ChecklistBank

ChecklistBank is the taxonomy store with its associated [webservices](http://www.gbif.org/developer/species) that allows GBIF to index any number of checklist datasets and match them to a global management taxonomy, the [GBIF backbone taxonomy](http://www.gbif.org/dataset/d7dddbf4-2cf0-4f39-9b2a-bb099caae36c).

**Warning!** We have evolved [ChecklistBank](https://www.checklistbank.org) together with the [Catalogue of Life](https://www.catalogueoflife.org) into a standalone service based on a [new open code base](https://github.com/CatalogueOfLife/checklistbank). This project is still in use by GBIF, but not very active any longer.

This is a multi module project containing all ChecklistBank modules, from api to persistence layer to the webservice client. Integration tests are part of the respective modules, in particular in the webservice client and the mybatis modules.

# ChecklistBank database schema

Checklistbank relies on postgres 17 and uses the HStore extension.  The simplest way of enabling this is to add it to the postgres template database, which is used whenever postgres creates a new one.  Thus if you run the following (or similar)
before creating the database, you are all set:

    psql -u postgres template1 -c 'create extension hstore;'

You can create a database schema from scratch or update one to the latest version using the maven liquiabse plugin like this:

    mvn -P clb-local liquibase:update

A [diagram of the database schema](docs/schema.pdf) is available for convenience.
