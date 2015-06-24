ChecklistBank
-------------

A multi module project containing all ChecklistBank  modules needed, from api to webservice client.
Integration tests are part of the respective modules, in particular in the webservice client and the mybatis modules.

The individual modules refer to each other via relative paths and share common test resources (see below).
Therefore the entire project needs to be checked out and modules on their own are likely to be broken!

Checklistbank relies on postgres with and uses the HStore.  The simplest way of enabling this is to add it to the
postgres template database, which is used whenever postgres creates a new one.  Thus if you run the following (or similar)
before creating the database, you are all set:
  $ psql -u postgres template1 -c 'create extension hstore;'

To run all tests you need a profile in your ~/.m2/settings.xml file with the following properties defined:

  * checklistbank.db.url
  * checklistbank.db.username
  * checklistbank.db.password

An example profile could look like this:

  <profile>
    <id>checklistbank-local-postgres</id>
    <properties>
      <checklistbank.db.url>jdbc:postgresql://localhost/checklistbank</checklistbank.db.url>
      <checklistbank.db.username>checklistbank</checklistbank.db.username>
      <checklistbank.db.password>checklistbank</checklistbank.db.password>
    </properties>
  </profile>


*** Shared Test Resources
For all tests in all modules the mybatis module contains the sole liquibase and dbunit test resources,
while the index-builder maintains the canned solr index and configurations for integration tests.
