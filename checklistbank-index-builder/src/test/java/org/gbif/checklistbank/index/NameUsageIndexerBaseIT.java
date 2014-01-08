package org.gbif.checklistbank.index;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.gbif.checklistbank.index.guice.EmbeddedSolrReference;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Properties;

/**
 * Base class for integration tests using the squirrels test solr index.
 * The class builds a fresh index before all tests run.
 */
public abstract class NameUsageIndexerBaseIT {
  private static final String PROPERTY_FILE = "checklistbank.properties";
  private static final String PROPERTY_DEFAULT_FILE = "checklistbank-indexer-default.properties";

  private static final Logger LOG = LoggerFactory.getLogger(NameUsageIndexerBaseIT.class);
  private static EmbeddedSolrReference solrRef;
  private static NameUsageIndexer nameUsageIndexer;

  @Rule
  public DatabaseDrivenChecklistBankTestRule<?> squirrel = new DatabaseDrivenChecklistBankTestRule();

  @BeforeClass
  public static void setup() throws IOException, SAXException, ParserConfigurationException {
    // Creates the injector, merging properties taken from default test indexing and checklistbank
    Properties props = PropertiesUtil.loadProperties(PROPERTY_DEFAULT_FILE);
    Properties props2 = PropertiesUtil.loadProperties(PROPERTY_FILE);
    props.putAll(props2);
    Injector injector = Guice.createInjector(new IndexingTestModule(props));
    // Gets the indexer instance
    solrRef = injector.getInstance(EmbeddedSolrReference.class);
    nameUsageIndexer = injector.getInstance(NameUsageIndexer.class);
    nameUsageIndexer.run();
  }

  protected static EmbeddedSolrServer solr() {
    return solrRef.getSolr();
  }

  protected static NameUsageIndexer indexer() {
    return nameUsageIndexer;
  }

}
