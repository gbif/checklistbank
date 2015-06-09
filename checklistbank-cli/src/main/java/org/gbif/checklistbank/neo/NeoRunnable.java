package org.gbif.checklistbank.neo;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.neo.traverse.Traversals;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.yammer.metrics.MetricRegistry;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public abstract class NeoRunnable implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(NeoRunnable.class);

  private final NeoConfiguration neoCfg;
  protected final UUID datasetKey;
  protected final int batchSize;
  protected GraphDatabaseService db;
  protected NeoMapper mapper = NeoMapper.instance();

  public NeoRunnable(UUID datasetKey, NeoConfiguration cfg, MetricRegistry registry) {
    batchSize = cfg.batchSize;
    this.datasetKey = datasetKey;
    this.neoCfg = cfg;
  }

  protected GraphDatabaseService setupDb(boolean clean) {
    db = neoCfg.newEmbeddedDb(datasetKey, clean);
    return db;
  }

  protected void tearDownDb() {
    db.shutdown();
  }

  /**
   * @return the single matching node with the taxonID or null
   */
  protected Node nodeByTaxonId(String taxonID) {
    return IteratorUtil.singleOrNull(db.findNodes(Labels.TAXON, TaxonProperties.TAXON_ID, taxonID));
  }

  /**
   * @return the single matching node with the canonical name or null
   */
  protected Node nodeByCanonical(String canonical) throws NotUniqueException {
    try {
      return IteratorUtil
        .singleOrNull(db.findNodes(Labels.TAXON, TaxonProperties.CANONICAL_NAME, canonical));
    } catch (NoSuchElementException e) {
      throw new NotUniqueException("Canonical name not unique: " + canonical, canonical);
    }
  }

  protected Collection<Node> nodesByCanonical(String canonical) {
    return IteratorUtil
      .asCollection(db.findNodes(Labels.TAXON, TaxonProperties.CANONICAL_NAME, canonical));
  }

  /**
   * @return the single matching node with the scientific name or null
   */
  protected Node nodeBySciname(String sciname) throws NotUniqueException {
    try {
      return IteratorUtil
        .singleOrNull(db.findNodes(Labels.TAXON, TaxonProperties.SCIENTIFIC_NAME, sciname));
    } catch (NoSuchElementException e) {
      throw new NotUniqueException("Scientific name not unique: " + sciname, sciname);
    }
  }

  protected Node create(Origin origin, String sciname, Rank rank, TaxonomicStatus status) {
    NameUsage u = new NameUsage();
    u.setScientificName(sciname);
    //TODO: parse name???
    u.setCanonicalName(sciname);
    u.setRank(rank);
    u.setOrigin(origin);
    u.setTaxonomicStatus(status);
    Node n = db.createNode(Labels.TAXON);
    if (status != null && status.isSynonym()) {
      n.addLabel(Labels.SYNONYM);
    }
    mapper.store(n, u, false);
    return n;
  }

  protected boolean matchesClassification(Node n, List<RankedName> classification) {
    Iterator<RankedName> clIter = classification.listIterator();
    Iterator<Node> nodeIter = Traversals.PARENTS.traverse(n).nodes().iterator();

    while (clIter.hasNext()) {
      if (!nodeIter.hasNext()) {
        return false;
      }
      RankedName rn1 = clIter.next();
      RankedName rn2 = mapper.readRankedName(nodeIter.next());
      if (rn1.rank != rn2.rank || !rn1.name.equals(rn2.name)) {
        return false;
      }
    }
    return !nodeIter.hasNext();
  }

  /**
   * @return the last parent or the node itself if no parent exists
   */
  protected RankedName getHighestParent(Node n) {
    Node p = IteratorUtil.lastOrNull(Traversals.PARENTS.traverse(n).nodes());
    if (p != null) {
      return mapper.readRankedName(p);
    }
    return mapper.readRankedName(n);
  }

}
