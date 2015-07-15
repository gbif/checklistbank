package org.gbif.checklistbank.cli;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;
import org.gbif.checklistbank.cli.normalizer.NormalizerTest;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NodeProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.yammer.metrics.MetricRegistry;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * base class to insert neo integration tests.
 */
public abstract class BaseTest {

  protected NormalizerConfiguration cfg;
  protected UsageDao dao;

  @Before
  public void initCfg() throws Exception {
    cfg = new NormalizerConfiguration();
    cfg.neo.neoRepository = Files.createTempDir();

    URL dwcasUrl = getClass().getResource("/dwcas");
    Path p = Paths.get(dwcasUrl.toURI());
    cfg.archiveRepository = p.toFile();
  }

  @After
  public void cleanup() throws Exception {
    if (dao != null) {
      dao.closeAndDelete();
    }
  }

  public void initDb() {
    dao = UsageDao.temporaryDao(25);
  }

  /**
   *
   * @param datasetKey
   */
  protected void openDb(UUID datasetKey) {
    dao = UsageDao.persistentDao(cfg.neo, datasetKey, new MetricRegistry("opendb"), false);
  }

  protected void compareStats(NormalizerStats stats) {
    // get proper counts and compare them to the handler generated one
    // we only compare some bits as not all are populated equally
    try (Transaction tx = beginTx()) {
      NormalizerStats statsExpected = getStats();
      System.out.println("Expected: " + statsExpected);
      System.out.println("Normalizer: " + stats);
      assertEquals("roots differ", statsExpected.getRoots(), stats.getRoots());
      assertEquals("synonyms differ", statsExpected.getSynonyms(), stats.getSynonyms());
      assertEquals("ranks differ", statsExpected.getCountByRank(), stats.getCountByRank());
      assertEquals("origins differ", statsExpected.getCountByOrigin(), stats.getCountByOrigin());
    }
  }

  public NormalizerStats insertNeo(UUID datasetKey) {
    Normalizer norm = NormalizerTest.buildNormalizer(cfg, datasetKey);
    norm.run();
    NormalizerStats stats = norm.getStats();

    openDb(datasetKey);
    compareStats(stats);
    dao.close();

    return stats;
  }

  public NormalizerStats getStats() {
    int roots = 0;
    int depth = -1;
    int synonyms = 0;
    int ignored = 0;
    Map<Origin, Integer> countByOrigin = Maps.newHashMap();
    Map<Rank, Integer> countByRank = Maps.newHashMap();

    for (Node n : GlobalGraphOperations.at(dao.getNeo()).getAllNodes()) {
      if (!n.hasLabel(Labels.TAXON)) {
        ignored++;
        continue;
      }

      NameUsage u = dao.readUsage(n, false);
      if (n.hasLabel(Labels.ROOT)) {
        roots++;
      }
      if (n.hasLabel(Labels.SYNONYM)) {
        synonyms++;
        if (!n.hasRelationship(RelType.SYNONYM_OF)) {
          throw new IllegalStateException("Node "+n.getId()+" with synonym label but without synonymOf relationship found");
        }
      } else if (u.isSynonym()) {
        throw new IllegalStateException("Node "+n.getId()+" without synonym label but usage has isSynonym=true");
      }

      if (!countByOrigin.containsKey(u.getOrigin())) {
        countByOrigin.put(u.getOrigin(), 1);
      } else {
        countByOrigin.put(u.getOrigin(), countByOrigin.get(u.getOrigin()) + 1);
      }
      if (u.getRank() != null) {
        if (!countByRank.containsKey(u.getRank())) {
          countByRank.put(u.getRank(), 1);
        } else {
          countByRank.put(u.getRank(), countByRank.get(u.getRank()) + 1);
        }
      }
    }
    return new NormalizerStats(roots, depth, synonyms, ignored, countByOrigin, countByRank, Lists.<String>newArrayList());
  }

  public Transaction beginTx() {
    return dao.beginTx();
  }

  public NameUsage getUsageByKey(int key) {
    Node n = dao.getNeo().getNodeById(key);
    return getUsageByNode(n);
  }

  /**
   * gets single usage or null, throws exception if more than 1 usage exist!
   */
  public NameUsage getUsageByTaxonId(String id) {
    Node n = IteratorUtil.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.TAXON_ID, id));
    return getUsageByNode(n);
  }

  public NameUsageMetrics getMetricsByTaxonId(String taxonID) {
    Node n = IteratorUtil.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.TAXON_ID, taxonID));
    return dao.readFacts(n.getId()).metrics;
  }

  public List<NameUsage> getUsagesByName(String name) {
    List<NameUsage> usages = Lists.newArrayList();
    for (Node n : IteratorUtil.loop(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.SCIENTIFIC_NAME, name))) {
      usages.add(getUsageByNode(n));
    }
    return usages;
  }

  /**
   * Gets single usage by its canonical name. Returns null if none found or throws exception if more than 1 usage with that name exists!
   */
  public NameUsage getUsageByCanonical(String name) {
    Node n = IteratorUtil.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.CANONICAL_NAME, name));
    return getUsageByNode(n);
  }

  /**
   * Gets single usage by its scientific name. Returns null if none found or throws exception if more than 1 usage with that name exists!
   */
  public NameUsage getUsageByName(String name) {
    Node n = IteratorUtil.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.SCIENTIFIC_NAME, name));
    return getUsageByNode(n);
  }

  public List<Node> getNodesByName(String name) {
    return IteratorUtil.asList(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.SCIENTIFIC_NAME, name));
  }

  public List<NameUsage> getAllUsages() {
    List<NameUsage> usages = Lists.newArrayList();
    for (Node n : IteratorUtil.loop(dao.getNeo().findNodes(Labels.TAXON))) {
      usages.add(getUsageByNode(n));
    }
    return usages;
  }

  private NameUsage getUsageByNode(Node n) {
    NameUsage u = null;
    if (n != null) {
      u = dao.readUsage(n, true);
      if (u != null) {
        u.setKey((int)n.getId());
      }
    }
    return u;
  }

  public void showAll() {
    try (Transaction tx = beginTx()) {
      show(dao.getNeo().findNodes(Labels.TAXON));
    }
  }

  /**
   * Debug method to show all root nodes in the neo db.
   */
  public void showRoot() {
    try (Transaction tx = beginTx()) {
      show(dao.getNeo().findNodes(Labels.ROOT));
    }
  }

  /**
   * Debug method to show all synonyms in the neo db.
   */
  public void showSynonyms() {
    try (Transaction tx = beginTx()) {
      show(dao.getNeo().findNodes(Labels.SYNONYM));
    }
  }

  private void show(ResourceIterator<Node> iter) {
    System.out.println("\n\n");

    while (iter.hasNext()) {
      Node n = iter.next();
      NameUsage u = getUsageByNode(n);
      System.out.println("### " + n.getId() + " " + u.getScientificName());
      System.out.println(u);
    }
    iter.close();
  }

  public void assertUsage(
    String sourceID,
    boolean synonym,
    String name,
    String basionym,
    String accepted,
    Rank rank,
    String ... parents
  ) {
    NameUsage u = getUsageByTaxonId(sourceID);
    assertEquals(synonym, u.isSynonym());
    assertEquals(rank, u.getRank());
    assertEquals(name, u.getScientificName());
    NameUsage nu = getUsageByKey(u.getKey());
    if (basionym != null) {
      assertNotNull(nu.getBasionymKey());
      NameUsage bas = getUsageByKey(nu.getBasionymKey());
      assertEquals(basionym, bas.getScientificName());
    } else {
      assertNull(nu.getBasionymKey());
    }
    Integer pid = u.getParentKey();
    for (String pn : parents) {
      if (pid == null) {
        fail("Missing expected parent " + pn);
      }
      NameUsage p = getUsageByKey(pid);
      assertEquals(pn, p.getScientificName());
      pid = p.getParentKey();
    }
  }
}
