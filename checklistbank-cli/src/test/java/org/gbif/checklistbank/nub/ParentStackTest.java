package org.gbif.checklistbank.nub;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import org.junit.Test;

import static org.junit.Assert.*;

public class ParentStackTest {

  @Test
  public void testStack() throws Exception {
    ParentStack parents = new ParentStack(null);

    assertEquals(0, parents.size());
    assertNull(parents.nubParent());

    parents.add(src(1, null));
    parents.add(src(2, 1));
    NubUsage nub = nub("nub#3");
    parents.put(nub);
    assertNull(parents.nubParent());

    assertFalse(parents.isDoubtful());
    parents.markSubtreeAsDoubtful(); // doubtful key=2
    assertTrue(parents.isDoubtful());

    parents.add(src(3, 2));
    assertEquals(3, parents.size());
    assertEquals(nub, parents.nubParent());
    assertTrue(parents.isDoubtful());

    parents.add(src(4, 1)); // this removes all but the first key
    assertEquals(2, parents.size());
    assertFalse(parents.isDoubtful());
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