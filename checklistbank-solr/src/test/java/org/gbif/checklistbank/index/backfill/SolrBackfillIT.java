package org.gbif.checklistbank.index.backfill;

import com.google.common.collect.Lists;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.vocabulary.Habitat;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test the index generation.
 */
//@Ignore("The test sometimes fails in Jenkins for unknown reasons. Appears to be overloaded postgres but needs investigations")
public class SolrBackfillIT {

  private static SolrTestSetup setup;

  @BeforeClass
  public static void setup() throws Exception {
    // creates squirrels db and solr index & server using its own injector
    setup = new SolrTestSetup(ClbDbTestRule.squirrels());
    setup.setup();
  }

  private EmbeddedSolrServer solr() {
    return setup.solr();
  }

  @Test
  public void testIndexBuild() throws IOException, SolrServerException, InterruptedException {
    NameUsageDocConverter converter = new NameUsageDocConverter();

    // test index and make sure we use the squirrels test data
    // number of all records
    SolrQuery query = new SolrQuery();
    query.setQuery("*:*");
    QueryResponse rsp = solr().query(query);
    assertEquals(46l, rsp.getResults().getNumFound());

    // number of all source records
    query = new SolrQuery();
    query.setQuery("origin_key:"+ Origin.SOURCE.ordinal());
    rsp = solr().query(query);
    assertEquals(46l, rsp.getResults().getNumFound());

    // vernacular name with umlaut
    query = new SolrQuery();
    query.setQuery("vernacular_name:\"Europäisches Eichhörnchen\"");
    rsp = solr().query(query);
    SolrDocumentList docs = rsp.getResults();
    assertEquals(1, docs.size());

    // single species
    query = new SolrQuery();
    query.setQuery("canonical_name:\"Sciurillus pusillus\"");
    rsp = solr().query(query);

    docs = rsp.getResults();
    assertEquals(1, docs.size());
    NameUsageSearchResult u1 = converter.toSearchUsage(docs.get(0), true);
    assertEquals((Integer) 100000007, u1.getKey());

    // extinct
    query = new SolrQuery();
    query.setQuery("key:100000025");
    rsp = solr().query(query);
    docs = rsp.getResults();
    assertEquals(1, docs.size());
    u1 = converter.toSearchUsage(docs.get(0), true);
    assertEquals((Integer) 100000025, u1.getKey());
    assertFalse(u1.isSynonym());
    assertFalse(u1.isExtinct());
    assertEquals(Lists.newArrayList(Habitat.TERRESTRIAL), u1.getHabitats());
    assertEquals(Rank.SPECIES, u1.getRank());
    assertEquals((Integer) 100000024, u1.getSubgenusKey());
    assertEquals((Integer) 100000011, u1.getGenusKey());
    assertEquals((Integer) 100000005, u1.getFamilyKey());
    assertEquals((Integer) 100000004, u1.getOrderKey());
    assertEquals((Integer) 100000003, u1.getClassKey());
    assertEquals((Integer) 100000002, u1.getPhylumKey());
    assertEquals((Integer) 100000001, u1.getKingdomKey());
    assertEquals((Integer) 100000024, u1.getParentKey());
    assertEquals("Sciurus", u1.getSubgenus());
    assertEquals("Sciurus", u1.getGenus());
    assertEquals("Sciuridae", u1.getFamily());
    assertEquals("Rodentia", u1.getOrder());
    assertEquals("Mammalia", u1.getClazz());
    assertEquals("Chordata", u1.getPhylum());
    assertEquals("Animalia", u1.getKingdom());
    assertEquals("Sciurus", u1.getParent());
    assertNull(u1.getAcceptedKey());
    assertEquals(6, u1.getDescriptions().size());
    assertEquals("2010030", u1.getTaxonID());

    query = new SolrQuery();
    query.setQuery("extinct:false");
    rsp = solr().query(query);
    docs = rsp.getResults();
    assertEquals(1, docs.size());
    u1 = converter.toSearchUsage(docs.get(0), true);
    assertEquals((Integer) 100000025, u1.getKey());

    // threat status
    query = new SolrQuery();
    query.setQuery("threat_status_key:*");
    rsp = solr().query(query);
    docs = rsp.getResults();
    assertEquals(1, docs.size());
    u1 = converter.toSearchUsage(docs.get(0), true);
    assertEquals((Integer) 100000007, u1.getKey());

    // habitat
    query = new SolrQuery();
    query.setQuery("habitat_key:*");
    rsp = solr().query(query);
    docs = rsp.getResults();
    assertEquals(2, docs.size());

    // scientific_name
    query = new SolrQuery();
    query.setQuery("scientific_name:\"Sciurus nadymensis Serebrennikov, 1928\"");
    rsp = solr().query(query);
    docs = rsp.getResults();
    assertEquals(1, docs.size());
    u1 = converter.toSearchUsage(docs.get(0), true);
    assertEquals((Integer) 100000027, u1.getKey());

    query = new SolrQuery();
    query.setQuery("key:100000004");
    rsp = solr().query(query);
    docs = rsp.getResults();
    assertEquals(1, docs.size());
    u1 = converter.toSearchUsage(docs.get(0), true);
    for (Description d : u1.getDescriptions()) {
      // make sure we don't have html tags in the descriptions
      assertFalse(d.getDescription().contains("<"));
    }

    // test a subfamily search
    query = new SolrQuery();
    query.setQuery("higher_taxon_key:100000042");
    rsp = solr().query(query);
    docs = rsp.getResults();
    assertEquals(5, docs.size());

    // constituent_key
    query = new SolrQuery();
    query.setQuery("constituent_key:\"211aea14-c252-4a85-96e2-f5f4d5d088f5\"");
    rsp = solr().query(query);
    docs = rsp.getResults();
    assertEquals(8, docs.size());

    query.setQuery("constituent_key:\"211aea14-c252-4a85-96e2-f5f4d5d088f4\"");
    query.setRows(25);
    rsp = solr().query(query);
    docs = rsp.getResults();
    assertEquals(17, docs.size());
  }


}
