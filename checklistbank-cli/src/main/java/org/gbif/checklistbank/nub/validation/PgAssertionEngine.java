package org.gbif.checklistbank.nub.validation;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.neo4j.helpers.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgAssertionEngine implements AssertionEngine {
  private static final Logger LOG = LoggerFactory.getLogger(PgAssertionEngine.class);

  private final NameUsageService usageService;

  private boolean valid = true;

  public PgAssertionEngine(NameUsageService usageService) {
    this.usageService = usageService;
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void assertParentsContain(String searchName, Rank searchRank, String parent) {
    try {
      NameUsage start = findUsageByCanonical(searchName, searchRank);
      assertParentsContain(start.getKey(), null, parent);

    } catch (AssertionError e) {
      valid = false;
      LOG.error("Classification for {} {} lacks parent {}", searchRank, searchName, parent, e);
    }
  }

  @Override
  public void assertParentsContain(int usageKey, @Nullable Rank parentRank, String parent) {
    try {
      boolean found = false;
      for (NameUsage p : usageService.listParents(usageKey, null)) {
        if (parent.equalsIgnoreCase(p.getCanonicalOrScientificName()) && (parentRank == null || parentRank.equals(p.getRank()))) {
          found = true;
          break;
        }
      }
      Assert.assertTrue(found);

    } catch (AssertionError e) {
      valid = false;
      LOG.error("Classification for usage {} missing {}", usageKey, parent, e);
    }
  }

  @Override
  public void assertClassification(int usageKey, LinneanClassification classification) {
    try {
      NameUsage u = usageService.get(usageKey, null);
      for (Rank r : Rank.DWC_RANKS) {
        if (!Strings.isBlank(classification.getHigherRank(r))) {
          Assert.assertEquals(classification.getHigherRank(r), u.getHigherRank(r));
        }
      }

    } catch (AssertionError e) {
      valid = false;
      LOG.error("Classification assertion failed for {}", usageKey, e);
    }
  }

  @Override
  public void assertClassification(int usageKey, String... classification) {
    Iterator<String> expected = Lists.newArrayList(classification).iterator();
    Iterator<NameUsage> parents = Lists.reverse(usageService.listParents(usageKey, null)).iterator();

    try {
      while (expected.hasNext()) {
        Assert.assertTrue(parents.hasNext());
        Assert.assertEquals(expected.next(), parents.next().getCanonicalName());
      }
      Assert.assertFalse(parents.hasNext());

    } catch (AssertionError e) {
      valid = false;
      LOG.error("Classification for usage {} wrong", usageKey, e);
    }
  }

  @Override
  public void assertSearchMatch(int expectedSearchMatches, String name) {
    assertSearchMatch(expectedSearchMatches, name, null);
  }

  @Override
  public void assertSearchMatch(int expectedSearchMatches, String name, Rank rank) {
    List<NameUsage> matches = Lists.newArrayList();
    try {
      matches = findUsagesByCanonical(name, rank);
      Assert.assertEquals(expectedSearchMatches, matches.size());
    } catch (AssertionError e) {
      valid = false;
      LOG.error("Expected {} matches, but found {} for name {} with rank {}", expectedSearchMatches, matches.size(), name, rank);
    }
  }

  @Override
  public void assertNotExisting(String name, Rank rank) {
    List<NameUsage> matches = Lists.newArrayList();
    try {
      matches = findUsagesByCanonical(name, rank);
      Assert.assertTrue(matches.isEmpty());
    } catch (AssertionError e) {
      valid = false;
      LOG.error("Found {} {} expected to be missing: {}", rank, name, matches.get(0));
    }
  }

  @Override
  public void assertUsage(int usageKey, Rank rank, String name, String accepted, Kingdom kingdom) {
    NameUsage u = null;
    try {
      u = usageService.get(usageKey, null);
      Assert.assertNotNull("Usage "+usageKey+" not existing", u);
      Assert.assertEquals("Wrong rank", rank, u.getRank());
      Assert.assertTrue("Wrong scientific name", u.getScientificName().startsWith(name));

      if (Strings.isBlank(accepted)) {
        Assert.assertFalse("Not accepted", u.isSynonym());
      } else {
        Assert.assertTrue("Not a synonym", u.isSynonym());
        Assert.assertTrue("Wrong accepted name", u.getAccepted().startsWith(accepted));
      }

      if (kingdom != null) {
        Assert.assertEquals("Wrong kingdom", kingdom.nubUsageID(), u.getKingdomKey());
        Assert.assertEquals("Wrong kingdom", kingdom.scientificName(), u.getKingdom());
      }

    } catch (AssertionError e) {
      LOG.error("Usage {} {} wrong: {}\n{}", usageKey, name, e.getMessage(), u);
      valid = false;
    }
  }

  private NameUsage findUsageByCanonical(String name, Rank rank) {
    List<NameUsage> matches = findUsagesByCanonical(name, rank);
    if (matches.size() > 1 || matches.isEmpty()) {
      valid = false;
      LOG.error("{} matches when expecting single match for {} {}", matches.size(), rank, name);
      throw new AssertionError("No single match for " + name);
    }
    return matches.get(0);
  }

  private List<NameUsage> findUsagesByCanonical(String name, @Nullable Rank rank) {
    // avoid paging and do a large, single page
    PagingRequest req = new PagingRequest(0, 1000);
    List<NameUsage> matches = Lists.newArrayList();

    for (NameUsage u : usageService.listByCanonicalName(null, name, req, Constants.NUB_DATASET_KEY).getResults()) {
      if (rank == null || rank.equals(u.getRank())) {
        matches.add(u);
      }
    }
    return matches;
  }
}