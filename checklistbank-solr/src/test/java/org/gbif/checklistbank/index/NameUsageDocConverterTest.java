package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Rank;

import java.util.UUID;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NameUsageDocConverterTest {

  @Test
  public void testToObject() throws Exception {
    NameUsageDocConverter conv = new NameUsageDocConverter();
    NameUsage u = new NameUsage();
    u.setKey(12);
    u.setDatasetKey(UUID.randomUUID());
    u.setScientificName("Abies alba Mill.");
    u.setCanonicalName("Abies alba");
    u.setRank(Rank.SPECIES);
    u.setSynonym(false);
    u.setParentKey(1);

    SolrInputDocument doc = conv.toObject(u, null, null, null, null);
    assertEquals(u.getKey().toString(), doc.get("key").getValue());
    assertEquals(u.getDatasetKey().toString(), doc.get("dataset_key").getValue());
    assertEquals(u.getParentKey().toString(), doc.get("parent_key").getValue());
    assertEquals(u.getCanonicalName(), doc.get("canonical_name").getValue());
    assertEquals(u.getScientificName(), doc.get("scientific_name").getValue());
    assertEquals(u.getRank().ordinal(), doc.get("rank_key").getValue());
  }
}