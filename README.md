ChecklistBank
-------------

A multi module project containing all ChecklistBank  modules needed, from api to webservice client. Integration tests are part of the respective modules, in particular in the webservice client and the mybatis modules.

The individual modules refer to each other via relative paths and share common test resources (see below).
Therefore the entire project needs to be checked out and modules on their own are likely to be broken!


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

# ChecklistBank Database Schema

Checklistbank relies on postgres 9 and uses the HStore extension.  The simplest way of enabling this is to add it to the postgres template database, which is used whenever postgres creates a new one.  Thus if you run the following (or similar)
before creating the database, you are all set:

    psql -u postgres template1 -c 'create extension hstore;'

You can create a database schema from scratch or update one to the latest version using the maven liquiabse plugin like this:

    mvn -P clb-local liquibase:update

A [diagram of the database schema](schema.pdf) is available for convenience.
