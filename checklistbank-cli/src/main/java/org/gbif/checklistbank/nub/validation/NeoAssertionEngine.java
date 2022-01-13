/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.nub.validation;

import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.nub.NubDb;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.utils.NameFormatter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class NeoAssertionEngine implements AssertionEngine {
  private static final Logger LOG = LoggerFactory.getLogger(NeoAssertionEngine.class);

  private final NubDb db;
  private final Int2IntMap usage2NubKey = new Int2IntOpenHashMap();
  private boolean valid = true;

  public NeoAssertionEngine(NubDb db) {
    this.db = db;
    LOG.info("Populate reverse key map for neo ids");
    for (Map.Entry<Long, NubUsage> nub : db.dao().nubUsages()) {
      int nubKey = (int) (long) nub.getKey();
      int usageKey = nub.getValue().usageKey;
      Preconditions.checkArgument(!usage2NubKey.containsKey(usageKey), "usageKey " + usageKey + " not unique");
      usage2NubKey.put(usageKey, nubKey);
    }
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void assertParentsContain(String searchName, Rank searchRank, String parent) {
    try (Transaction tx = db.beginTx()) {
      Node start = findUsageByCanonical(searchName, searchRank).node;
      assertParentsContain(start, null, parent);
    } catch (AssertionError e) {
      valid = false;
      LOG.error("Classification for {} {} lacks parent {}", searchRank, searchName, parent, e);
    }
  }

  @Override
  public void assertParentsContain(int usageKey, Rank parentRank, String parent) {
    try (Transaction tx = db.beginTx()) {
      Node start = nodeById(usageKey);
      assertParentsContain(start, parentRank, parent);
    } catch (AssertionError e) {
      valid = false;
      LOG.error("Classification for usage {} missing {}", usageKey, parent, e);
    }
  }

  private void assertParentsContain(Node start, @Nullable Rank parentRank, String parent) throws AssertionError {
    try (Transaction tx = db.beginTx()) {
      boolean found = false;
      for (Node p : Traversals.PARENTS.traverse(start).nodes()) {
        NubUsage u = db.dao().readNub(p);
        if (parent.equalsIgnoreCase(NameFormatter.canonicalOrScientificName(u.parsedName)) && (parentRank == null || u.rank.equals(parentRank))) {
          found = true;
        }
      }
      Assert.assertTrue(found);
    }
  }

  @Override
  public void assertClassification(int usageKey, LinneanClassification classification) {
    try (Transaction tx = db.beginTx()) {
      NubUsage usage = getUsage(usageKey);
      Map<Rank, String> parents = db.parentsMap(usage.node);
      for (Rank r : Rank.DWC_RANKS) {
        if (!StringUtils.isBlank(classification.getHigherRank(r))) {
          if (!parents.get(r).equalsIgnoreCase(classification.getHigherRank(r))) {
            valid = false;
            LOG.error("Unexpected {} {} for {} {}", r, classification.getHigherRank(r), usage.toStringComplete());
          }
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
    try (Transaction tx = db.beginTx()) {
      Node start = nodeById(usageKey);
      for (Node p : Traversals.PARENTS.traverse(start).nodes()) {
        NubUsage u = db.dao().readNub(p);
        Assert.assertEquals(expected.next(), NameFormatter.canonicalOrScientificName(u.parsedName));
      }
      Assert.assertFalse(expected.hasNext());
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
    List<NubUsage> matches = Lists.newArrayList();
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
    List<NubUsage> matches = Lists.newArrayList();
    try {
      matches = findUsagesByCanonical(name, rank);
      Assert.assertTrue(matches.isEmpty());
    } catch (AssertionError e) {
      valid = false;
      LOG.error("Found name expected to be missing: {} {} with rank {}", matches.get(0).node, name, rank);
    }
  }

  private NubUsage getUsage(int usageKey) {
    long nodeId = usage2NubKey.get(usageKey);
    return db.dao().readNub(nodeId);
  }

  @Override
  public void assertUsage(int usageKey, Rank rank, String name, String accepted, Kingdom kingdom) {
    NubUsage u = null;
    try (Transaction tx = db.beginTx()) {
      u = getUsage(usageKey);
      Assert.assertNotNull(u);
      Assert.assertEquals(rank, u.rank);
      Assert.assertTrue(u.parsedName.canonicalNameComplete().startsWith(name));
      if (StringUtils.isBlank(accepted)) {
        Assert.assertTrue(u.status.isAccepted());
      } else {
        Assert.assertTrue(u.status.isSynonym());
        NubUsage p = db.parent(u);
        Assert.assertTrue(p.parsedName.canonicalNameComplete().startsWith(accepted));
      }
      NubUsage ku = findRootUsage(u);
      if (kingdom != null) {
        Assert.assertEquals(kingdom, ku.kingdom);
        Assert.assertEquals(Rank.KINGDOM, ku.rank);
        Assert.assertEquals(kingdom.scientificName(), ku.parsedName.getScientificName());
      }
    } catch (AssertionError e) {
      LOG.error("Usage {}, {} wrong: {}", usageKey, name, e);
      valid = false;
    }
  }

  private Node nodeById(int usageKey) throws AssertionError {
    try {
      return db.getNode(usage2NubKey.get(usageKey));
    } catch (NotFoundException e) {
      throw new AssertionError("Usage " + usageKey + " not found");
    }
  }

  private NubUsage findRootUsage(NubUsage u) {
    try (Transaction tx = db.beginTx()) {
      Node root = Iterables.last(Traversals.PARENTS.traverse(u.node).nodes());
      return db.dao().readNub(root);
    }
  }

  private NubUsage findUsageByCanonical(String name, Rank rank) {
    List<NubUsage> matches = findUsagesByCanonical(name, rank);
    if (matches.size() > 1 || matches.isEmpty()) {
      valid = false;
      LOG.error("{} matches when expecting single match for {} {}", matches.size(), rank, name);
      throw new AssertionError("No single match for " + name);
    }
    return matches.get(0);
  }

  private List<NubUsage> findUsagesByCanonical(String name, @Nullable Rank rank) {
    List<NubUsage> matches = Lists.newArrayList();
    try (Transaction tx = db.beginTx()) {
      for (Node n : db.dao().findByName(name)) {
        NubUsage u = db.dao().readNub(n);
        if (rank == null || rank.equals(u.rank)) {
          matches.add(u);
        }
      }
    }
    return matches;
  }
}