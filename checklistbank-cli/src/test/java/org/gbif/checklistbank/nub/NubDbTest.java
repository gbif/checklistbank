package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Origin;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;

public class NubDbTest {

  private NubDb nub;

  @Before
  public void init() {
    UsageDao dao = UsageDao.temporaryDao(10);
    nub = NubDb.create(dao, 10);
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