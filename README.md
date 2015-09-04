# GBIF ChecklistBank

ChecklistBank is the taxonomy store with its associated [webservices](http://www.gbif.org/developer/species) that allows GBIF to index any number of checklist datasets and match them to a global management taxonomy, the [GBIF backbone taxonomy](http://www.gbif.org/dataset/d7dddbf4-2cf0-4f39-9b2a-bb099caae36c).

This is a multi module project containing all ChecklistBank modules, from api to persistence layer to the webservice client. Integration tests are part of the respective modules, in particular in the webservice client and the mybatis modules.

### Maven profile
To run all tests you need a maven profile with the following properties defined:

    <profile>
      <id>clb-local</id>
      <properties>
        <checklistbank.db.host>localhost</checklistbank.db.host>
        <checklistbank.db.name>clb</checklistbank.db.name>
        <checklistbank.db.username>postgres</checklistbank.db.username>
        <checklistbank.db.password>123456</checklistbank.db.password>
      </properties>
    </profile>

# ChecklistBank database schema

Checklistbank relies on postgres 9 and uses the HStore extension.  The simplest way of enabling this is to add it to the postgres template database, which is used whenever postgres creates a new one.  Thus if you run the following (or similar)
before creating the database, you are all set:

    psql -u postgres template1 -c 'create extension hstore;'

You can create a database schema from scratch or update one to the latest version using the maven liquiabse plugin like this:

    mvn -P clb-local liquibase:update

A [diagram of the database schema](docs/schema.pdf) is available for convenience.
