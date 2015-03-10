package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.vocabulary.Habitat;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Rank;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
    u.getIssues().add(NameUsageIssue.RANK_INVALID);
    u.getIssues().add(NameUsageIssue.BACKBONE_MATCH_FUZZY);

    SpeciesProfile sp = new SpeciesProfile();
    sp.setTerrestrial(true);
    sp.setHabitat("brackish");

    VernacularName v1 = new VernacularName();
    v1.setLanguage(Language.GERMAN);
    v1.setVernacularName("Weißtanne");
    VernacularName v2 = new VernacularName();
    v2.setLanguage(Language.GERMAN);
    v2.setVernacularName("Kohl Tanne");

    SolrInputDocument doc = conv.toObject(u, Lists.newArrayList(12,15,20,100), Lists.newArrayList(v1,v2), null, null, Lists.newArrayList(sp));
    assertEquals(u.getKey().toString(), doc.get("key").getValue());
    assertEquals(u.getDatasetKey().toString(), doc.get("dataset_key").getValue());
    assertEquals(u.getParentKey().toString(), doc.get("parent_key").getValue());
    assertEquals(u.getCanonicalName(), doc.get("canonical_name").getValue());
    assertEquals(u.getScientificName(), doc.get("scientific_name").getValue());
    assertEquals(u.getRank().ordinal(), doc.get("rank_key").getValue());
    assertThat((List<Integer>) doc.get("issues").getValue()).contains(NameUsageIssue.RANK_INVALID.ordinal(),
      NameUsageIssue.BACKBONE_MATCH_FUZZY.ordinal());
    assertThat((List<Integer>) doc.get("habitat_key").getValue()).containsOnlyOnce(Habitat.TERRESTRIAL.ordinal(), Habitat.FRESHWATER.ordinal());
    assertThat((List<Integer>) doc.get("higher_taxon_key").getValue()).containsOnlyOnce(12, 15, 20, 100);
  }
}