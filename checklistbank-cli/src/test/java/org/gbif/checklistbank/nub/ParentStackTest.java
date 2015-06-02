package org.gbif.checklistbank.nub;

import org.gbif.api.model.checklistbank.ParsedName;
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

  private SrcUsage src(int key, Integer parentKey) {
    SrcUsage u = new SrcUsage();
    u.key = key;
    u.parentKey = parentKey;
    u.scientificName = "Sciname #" + key;
    return u;
  }

  private NubUsage nub(String name) {
    NubUsage n = new NubUsage();
    n.parsedName = new ParsedName();
    n.parsedName.setScientificName(name);
    return n;
  }
}