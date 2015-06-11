package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Origin;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;

public class NubDbTest {

  private GraphDatabaseService db;
  private Transaction tx;
  private NubDb nub;

  @After
  public void shutdown() {
    if (tx != null) {
      tx.close();
    }
    if (db != null) {
      db.shutdown();
    }
  }

  @Before
  public void init() {
    NubConfiguration cfg = new NubConfiguration();
    db = cfg.neo.newEmbeddedDb(Constants.NUB_DATASET_KEY, true);
    nub = new NubDb(db, 10);
    tx = db.beginTx();
  }

  @Test
  public void testCountTaxa() throws Exception {
    assertEquals(0l, nub.countTaxa());


    NubUsage u = new NubUsage();
    u.parsedName = new ParsedName();
    u.origin = Origin.SOURCE;
    nub.addRoot(u);
    nub.addRoot(u);

    assertEquals(2l, nub.countTaxa());
  }
}