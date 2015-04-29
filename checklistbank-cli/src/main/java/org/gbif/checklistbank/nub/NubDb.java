package org.gbif.checklistbank.nub;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.RelType;

import java.io.Closeable;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.TraversalDescription;

public class NubDb implements Closeable {
  private final int batchSize;
  protected GraphDatabaseService db;
  private Transaction tx;
  private NeoMapper mapper = NeoMapper.instance();
  private TraversalDescription parentsTraversal;
  // activity counter to manage transaction commits
  private int counter = 0;

  public NubDb(GraphDatabaseService db, int batchSize) {
    this.batchSize = batchSize;
    this.db = db;
    tx = db.beginTx();
  }

  protected Node addUsage(Node parent, SrcUsage src) {
    Node n = db.createNode(Labels.TAXON);
    NameUsage u = new NameUsage();
    u.setKey(src.key);
    u.setCanonicalName(src.canonical);
    u.setAuthorship(src.author);
    u.setScientificName(src.canonical);
    u.setOrigin(Origin.SOURCE);
    u.setRank(src.rank);
    u.setTaxonomicStatus(src.status);
    if (src.nomStatus != null) {
      for (NomenclaturalStatus ns : src.nomStatus) {
        u.getNomenclaturalStatus().add(ns);
      }
    }

    if (parent == null) {
      n.addLabel(Labels.ROOT);
    } else {
      if (src.status != null && src.status.isSynonym()) {
        n.addLabel(Labels.SYNONYM);
        u.setSynonym(true);
        n.createRelationshipTo(parent, RelType.SYNONYM_OF);
      } else {
        parent.createRelationshipTo(n, RelType.PARENT_OF);
      }
    }

    //TODO: map more data, basionym, fullname, author, ...
    mapper.store(n, u, false);

    countAndRenewTx();
    return n;
  }

  protected static Integer toInt(String x) {
    return x == null ? null : Integer.valueOf(x);
  }

  private void countAndRenewTx() {
    if (counter++ > batchSize) {
      renewTx();
    }
  }

  protected void renewTx() {
    tx.success();
    tx.close();
    tx = db.beginTx();
    counter = 0;
  }

  @Override
  public void close() {
    tx.success();
    tx.close();
    db.shutdown();
  }
}
