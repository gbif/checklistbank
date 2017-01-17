package org.gbif.checklistbank.nub;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ParentStackTest {

  @Test
  public void testClear() throws Exception {
    ParentStack parents = new ParentStack(null);

    assertEquals(0, parents.size());
    assertNull(parents.nubParent());

    parents.add(src(1, null));
    parents.add(src(2, 1));
    NubUsage nub = nub("nub#3");
    parents.put(nub);
    assertNull(parents.nubParent());

    parents.add(src(3, 2));
    assertEquals(3, parents.size());
    assertEquals(nub, parents.nubParent());

    parents.add(src(4, 1));
    assertEquals(2, parents.size());
    assertNull(parents.nubParent());
  }

  @Test
  public void testClassification() throws Exception {
    ParentStack parents = new ParentStack(null);

    assertEquals(0, parents.size());
    assertNull(parents.nubParent());

    parents.add(src(1, null, Rank.DOMAIN, "Life"));
    parents.add(src(2, 1, Rank.KINGDOM, "Animals"));
    parents.add(src(3, 2, Rank.PHYLUM, "Vertebrata"));
    parents.add(src(4, 3, Rank.ORDER, "Mammaliales"));
    parents.add(src(5, 4, Rank.SUBFAMILY, "Mamafamilia"));
    assertClassification(parents.classification(), "Animals", "Vertebrata", null, "Mammaliales", null, null);
    assertEquals(5, parents.size());

    parents.add(src(6, 5, Rank.GENUS, "Mama"));
    assertClassification(parents.classification(), "Animals", "Vertebrata", null, "Mammaliales", null, "Mama");

    parents.add(src(7, 3, Rank.GENUS, "Papa"));
    assertClassification(parents.classification(), "Animals", "Vertebrata", null, null, null, "Papa");

    parents.add(src(8, 1, Rank.SUBCLASS, "Subclassic"));
    assertClassification(parents.classification(), null, null, null, null, null, null);
    assertEquals(2, parents.size());

    parents.add(src(9, null, Rank.SUBCLASS, "Subclassic"));
    assertClassification(parents.classification(), null, null, null, null, null, null);

  }

  private void assertClassification(LinneanClassification cl, String k, String p, String c, String o, String f, String g) {
    assertEquals(k, cl.getKingdom());
    assertEquals(p, cl.getPhylum());
    assertEquals(c, cl.getClazz());
    assertEquals(o, cl.getOrder());
    assertEquals(f, cl.getFamily());
    assertEquals(g, cl.getGenus());
  }

  private SrcUsage src(int key, Integer parentKey) {
    SrcUsage u = new SrcUsage();
    u.key = key;
    u.parentKey = parentKey;
    u.scientificName = "Sciname #" + key;
    return u;
  }

  private SrcUsage src(int key, Integer parentKey, Rank rank, String name) {
    SrcUsage u = new SrcUsage();
    u.key = key;
    u.parentKey = parentKey;
    u.scientificName = name;
    u.rank = rank;
    return u;
  }

  private NubUsage nub(String name) {
    NubUsage n = new NubUsage();
    n.parsedName = new ParsedName();
    n.parsedName.setScientificName(name);
    return n;
  }
}