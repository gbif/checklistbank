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
package org.gbif.checklistbank.neo;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.model.NameUsageNode;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.logging.LogContext;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.utils.NameParsers;

import java.util.*;

import javax.annotation.Nullable;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.helpers.collection.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;


public class ImportDb {

  private static final Logger LOG = LoggerFactory.getLogger(ImportDb.class);

  protected final UUID datasetKey;
  protected final UsageDao dao;

  public ImportDb(UUID datasetKey, UsageDao dao) {
    this.datasetKey = datasetKey;
    this.dao = dao;
    LogContext.startDataset(datasetKey);
  }

  /**
   * @return the single matching node with the taxonID or null
   */
  protected Node nodeByTaxonId(String taxonID) {
    return Iterators.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.TAXON_ID, taxonID));
  }

  /**
   * @return the single matching node with the canonical name or null
   */
  protected Node nodeByCanonical(String canonical) throws NotUniqueException {
    try {
      return Iterators
          .singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.CANONICAL_NAME, canonical));
    } catch (NoSuchElementException e) {
      throw new NotUniqueException(canonical, "Canonical name not unique: " + canonical);
    }
  }

  protected Collection<Node> nodesByCanonical(String canonical) {
    return Iterators
        .asCollection(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.CANONICAL_NAME, canonical));
  }

  protected List<Node> nodesByCanonicalAndRank(String canonical, Rank rank) {
    List<Node> matching = filterByRank(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.CANONICAL_NAME, canonical), rank);
    if (matching.size() > 10) {
      LOG.warn("There are {} matches for the {} {}. This might indicate we are not dealing with a proper checklist", matching.size(), rank, canonical);
    }
    return matching;
  }

  private List<Node> filterByRank(ResourceIterator<Node> nodes, Rank rank) {
    List<Node> matchingRanks = Lists.newArrayList();
    while (nodes.hasNext()) {
      Node n = nodes.next();
      if (rank == null || n.getProperty(NeoProperties.RANK, rank.ordinal()).equals(rank.ordinal())) {
        matchingRanks.add(n);
      }
    }
    return matchingRanks;
  }

  /**
   * @return the single matching node with the scientific name or null
   */
  protected Node nodeBySciname(String sciname) throws NotUniqueException {
    try {
      return Iterators
          .singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.SCIENTIFIC_NAME, sciname));
    } catch (NoSuchElementException e) {
      throw new NotUniqueException(sciname, "Scientific name not unique: " + sciname);
    }
  }

  protected NameUsageNode create(Origin origin, String sciname, Rank rank, TaxonomicStatus status, boolean isRoot) {
    return create(origin, sciname, rank, status, isRoot, null, null);
  }

  protected NameUsageNode create(Origin origin, String sciname, Rank rank, TaxonomicStatus status, boolean isRoot, @Nullable String taxonID, @Nullable String remark) {
    NameUsage u = new NameUsage();
    u.setScientificName(sciname);
    // or generate via parsed name below???
    u.setCanonicalName(sciname);
    u.setRank(rank);
    u.setOrigin(origin);
    u.setTaxonomicStatus(status);
    u.setTaxonID(taxonID);
    u.setRemarks(remark);
    return create(u, isRoot);
  }

  protected NameUsageNode create(NameUsage u, boolean isRoot) {
    Node n = dao.createTaxon();
    if (u.getTaxonomicStatus() != null && u.getTaxonomicStatus().isSynonym()) {
      n.addLabel(Labels.SYNONYM);
    }
    if (isRoot) {
      n.addLabel(Labels.ROOT);
    }

    // parse name and store it
    ParsedName pn = NameParsers.INSTANCE.parseQuietly(u.getScientificName(), u.getRank());
    dao.store(n.getId(), pn);

    // update canonical and store usage
    NameUsageNode nn = new NameUsageNode(n, u, true);
    dao.store(nn, true);

    return nn;
  }

  protected boolean matchesClassification(Node n, List<RankedName> classification) {
    Iterator<RankedName> clIter = classification.listIterator();
    Iterator<Node> nodeIter = Traversals.PARENTS.traverse(n).nodes().iterator();

    while (clIter.hasNext()) {
      if (!nodeIter.hasNext()) {
        return false;
      }
      RankedName rn1 = clIter.next();
      RankedName rn2 = dao.readRankedName(nodeIter.next());
      if (rn1.rank != rn2.rank || !rn1.name.equals(rn2.name)) {
        return false;
      }
    }
    return !nodeIter.hasNext();
  }

  /**
   * @return the last parent or the node itself if no parent exists
   */
  protected RankedName getDirectParent(Node n) {
    Node p = Iterables.lastOrNull(Traversals.PARENTS.traverse(n).nodes());
    return dao.readRankedName(p != null ? p : n);
  }

  protected Node getLinneanRankParent(Node n) {
    return Iterables.firstOrNull(Traversals.LINNEAN_PARENTS.traverse(n).nodes());
  }

  public UUID getDatasetKey() {
    return datasetKey;
  }
}
