package org.gbif.checklistbank.ws.client.guice;

import org.gbif.checklistbank.index.SolrIndexingTestModule;
import org.gbif.checklistbank.index.backfill.SolrBackfill;
import org.gbif.checklistbank.index.guice.EmbeddedSolrReference;
import org.gbif.checklistbank.index.guice.SearchTestModule;
import org.gbif.checklistbank.index.guice.SearchModule;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.gbif.checklistbank.ws.guice.ChecklistBankWsListener;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChecklistBankSearchWsTestListener extends ChecklistBankWsListener {
  private static final Logger LOG = LoggerFactory.getLogger(ChecklistBankSearchWsTestListener.class);
  private static final String PROPERTY_INDEXER_DEFAULT = "checklistbank-indexer-default.properties";

  public ChecklistBankSearchWsTestListener(Properties properties) {
    super(properties);
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    setupDb();

    List<Module> modules = super.getModules(properties);
    // very cowboy way of getting the solr index build first and "inject" the server here
    // replace regular search module with the test one using the embedded solr server
    Iterator<Module> iter = modules.iterator();
    while (iter.hasNext()) {
      Module m = iter.next();
      if (m instanceof SearchModule) {
        iter.remove();
      }
    }
    modules.add(new SearchTestModule(properties, setupSolr(properties).getSolr()));
    return modules;
  }


  /**
   * Starts up a solr server and indexes the test database.
   * Wrapped in a static method so we can set the solr server in the ChecklistBankSearchWsTestListener
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
      SolrBackfill nameUsageIndexer = injector.getInstance(SolrBackfill.class);
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
        @Override
        public void evaluate() throws Throwable {
          // do nothing
        }
      }, null).evaluate();
    } catch (Throwable throwable) {
      Throwables.propagate(throwable);
    }
  }
}
