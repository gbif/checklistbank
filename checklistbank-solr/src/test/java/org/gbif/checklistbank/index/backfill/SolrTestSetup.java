package org.gbif.checklistbank.index.backfill;

import org.gbif.checklistbank.index.SolrIndexingTestModule;
import org.gbif.checklistbank.index.guice.EmbeddedSolrReference;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for integration tests using the squirrels test solr index.
 * The class builds a fresh index before all tests run.
 */
public class SolrTestSetup {

  private static final Logger LOG = LoggerFactory.getLogger(SolrTestSetup.class);
  private static EmbeddedSolrReference solrRef;
  private static SolrBackfill nameUsageIndexer;
  private final ClbDbTestRule rule;

  public SolrTestSetup(ClbDbTestRule rule) {
    this.rule = rule;
  }

  public EmbeddedSolrReference setup() throws Exception {

    // run liquibase & dbSetup
    LOG.info("Run liquibase & dbSetup once");
    /**
    try {
      rule.apply(new Statement() {
        @Override
        public void evaluate() throws Throwable {
          // do nothing
        }
      }, null).evaluate();
    } catch (Throwable throwable) {
      Throwables.propagate(throwable);
    }*/

    // Creates the injector, merging properties taken from default test indexing and checklistbank
    Properties props = PropertiesUtil.loadProperties(IndexingConfigKeys.CLB_PROPERTY_FILE);
    Properties props2 = PropertiesUtil.loadProperties(IndexingConfigKeys.CLB_INDEXING_PROPERTY_TEST_FILE);
    props.putAll(props2);
    Injector injector = Guice.createInjector(new SolrIndexingTestModule(props));
    // Gets the indexer instance
    solrRef = injector.getInstance(EmbeddedSolrReference.class);
    LOG.info("Setup test solr at {}", solr());
    nameUsageIndexer = injector.getInstance(SolrBackfill.class);
    nameUsageIndexer.run();
    return solrRef;
  }

  public static EmbeddedSolrServer solr() {
    return solrRef.getSolr();
  }

}
