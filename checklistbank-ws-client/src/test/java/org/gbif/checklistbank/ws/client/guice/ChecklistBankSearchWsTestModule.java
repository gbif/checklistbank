package org.gbif.checklistbank.ws.client.guice;

import org.gbif.checklistbank.index.NameUsageIndexer;
import org.gbif.checklistbank.index.SolrIndexingTestModule;
import org.gbif.checklistbank.index.guice.EmbeddedSolrReference;
import org.gbif.checklistbank.search.SearchTestModule;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.gbif.checklistbank.ws.guice.ChecklistBankWsModule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChecklistBankSearchWsTestModule extends ChecklistBankWsModule {
  private static final Logger LOG = LoggerFactory.getLogger(ChecklistBankSearchWsTestModule.class);
  private static final String PROPERTY_INDEXER_DEFAULT = "checklistbank-indexer-default.properties";

  public ChecklistBankSearchWsTestModule(Properties properties) {
    super(properties);
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    setupDb();

    List<Module> modules = super.getModules(properties);
    // very cowboy way of getting the solr index build first and "inject" the server here
    // replace regular search module with the test one using the embedded solr server
    modules.remove(1);
    modules.add(new SearchTestModule(properties, setupSolr(properties).getSolr()));
    return modules;
  }


  /**
   * Starts up a solr server and indexes the test database.
   * Wrapped in a static method so we can set the solr server in the ChecklistBankSearchWsTestModule
   * which must have a default, empty constructor.
   */
  public static EmbeddedSolrReference setupSolr(Properties properties) {
    EmbeddedSolrReference solrRef;

    // Creates the injector, merging properties taken from default test indexing and checklistbank
    try {
      Properties props = PropertiesUtil.loadProperties(PROPERTY_INDEXER_DEFAULT);
      props.putAll(properties);
      Injector injector = Guice.createInjector(new SolrIndexingTestModule(props));

      // Gets the indexer instance
      solrRef = injector.getInstance(EmbeddedSolrReference.class);

      // build the solr index
      NameUsageIndexer nameUsageIndexer = injector.getInstance(NameUsageIndexer.class);
      LOG.info("Build solr index");
      nameUsageIndexer.run();

      return solrRef;

    } catch (IOException e) {
      throw new RuntimeException("Cant load properties to build solr index", e);
    }
  }

  public static void setupDb() {
    // run liquibase & dbSetup
    LOG.info("Run liquibase & squirrel test db once");
    try {
      ClbDbTestRule rule = ClbDbTestRule.squirrels();
      rule.apply(new Statement() {
        public void evaluate() throws Throwable {
          // do nothing
        }
      }, null).evaluate();
    } catch (Throwable throwable) {
      Throwables.propagate(throwable);
    }
  }
}
