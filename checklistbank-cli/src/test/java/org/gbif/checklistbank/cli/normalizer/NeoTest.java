package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
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
public abstract class NeoTest {

  protected NeoConfiguration cfg;
  protected GraphDatabaseService db;
  protected NeoMapper mapper = NeoMapper.instance();
  protected boolean cleanup;

  public NeoTest() {
    this.cleanup = true;
  }

  public NeoTest(boolean cleanup) {
    this.cleanup = cleanup;
  }

  @Before
  public void initNeoCfg() throws Exception {
    cfg = new NeoConfiguration();
    File tmp = FileUtils.createTempDir();
    cfg.neoRepository = tmp;
  }

  @After
  public void cleanup() throws Exception {
    if (db != null) {
      db.shutdown();
    }
    if (cleanup) {
      org.apache.commons.io.FileUtils.cleanDirectory(cfg.neoRepository);
      cfg.neoRepository.delete();
    }
  }

  public void initDb(UUID datasetKey) {
    db = cfg.newEmbeddedDb(datasetKey, true);
  }

  protected void initDb(UUID datasetKey, NormalizerStats stats) {
    initDb(datasetKey);
    compareStats(stats);
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

  public NormalizerStats getStats() {
    int roots = 0;
    int depth = -1;
    int synonyms = 0;
    int ignored = 0;
    Map<Origin, Integer> countByOrigin = Maps.newHashMap();
    Map<Rank, Integer> countByRank = Maps.newHashMap();

    for (Node n : GlobalGraphOperations.at(db).getAllNodes()) {
      if (n.hasLabel(Labels.ROOT)) {
        roots++;
      }
      if (n.hasLabel(Labels.SYNONYM)) {
        synonyms++;
        if (!n.hasRelationship(RelType.SYNONYM_OF)) {
          throw new IllegalStateException("Node "+n.getId()+" with synonym label but without synonymOf relationship found");
        }
      }
      if (!n.hasLabel(Labels.TAXON)) {
        ignored++;
      }
      Origin o = mapper.readOrigin(n);
      if (!countByOrigin.containsKey(o)) {
        countByOrigin.put(o, 1);
      } else {
        countByOrigin.put(o, countByOrigin.get(o) + 1);
      }
      Rank r = mapper.readRank(n);
      if (r != null) {
        if (!countByRank.containsKey(r)) {
          countByRank.put(r, 1);
        } else {
          countByRank.put(r, countByRank.get(r) + 1);
        }
      }
    }
    return new NormalizerStats(roots, depth, synonyms, ignored, countByOrigin, countByRank, Lists.<String>newArrayList());
  }

  public Transaction beginTx() {
    return db.beginTx();
  }

  public NameUsageContainer getUsageByKey(int key) {
    Node n = db.getNodeById(key);
    return getUsageByNode(n);
  }

  /**
   * gets single usage or null, throws exception if more than 1 usage exist!
   */
  public NameUsageContainer getUsageByTaxonId(String id) {
    Node n = IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(Labels.TAXON, TaxonProperties.TAXON_ID, id));
    return getUsageByNode(n);
  }

  public NameUsageMetrics getMetricsByKey(long nodeId) {
    Node n = db.getNodeById(nodeId);
    return mapper.read(n, new NameUsageMetrics());
  }

  public NameUsageMetrics getMetricsByTaxonId(String taxonID) {
    Node n = IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(Labels.TAXON, TaxonProperties.TAXON_ID, taxonID));
    return mapper.read(n, new NameUsageMetrics());
  }

  public List<NameUsageContainer> getUsagesByName(String name) {
    List<NameUsageContainer> usages = Lists.newArrayList();
    for (Node n : db.findNodesByLabelAndProperty(Labels.TAXON, TaxonProperties.SCIENTIFIC_NAME, name)) {
      usages.add(getUsageByNode(n));
    }
    return usages;
  }

  /**
   * gets single usage or null, throws exception if more than 1 usage exist!
   */
  public NameUsageContainer getUsageByName(String name) {
    Node n = IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(Labels.TAXON, TaxonProperties.CANONICAL_NAME, name));
    return getUsageByNode(n);
  }

  public List<Node> getNodesByName(String name) {
    return IteratorUtil.asList(db.findNodesByLabelAndProperty(Labels.TAXON, TaxonProperties.SCIENTIFIC_NAME, name));
  }

  public List<NameUsageContainer> getAllUsages() {
    List<NameUsageContainer> usages = Lists.newArrayList();
    for (Node n : GlobalGraphOperations.at(db).getAllNodesWithLabel(Labels.TAXON)) {
      usages.add(getUsageByNode(n));
    }
    return usages;
  }

  private NameUsageContainer getUsageByNode(Node n) {
    return mapper.read(n);
  }

  public void showAll() {
    try (Transaction tx = beginTx()) {
      show(GlobalGraphOperations.at(db).getAllNodesWithLabel(Labels.TAXON));
    }
  }

  /**
   * Debug method to show all root nodes in the neo db.
   */
  public void showRoot() {
    try (Transaction tx = beginTx()) {
      show(GlobalGraphOperations.at(db).getAllNodesWithLabel(Labels.ROOT));
    }
  }

  /**
   * Debug method to show all synonyms in the neo db.
   */
  public void showSynonyms() {
    try (Transaction tx = beginTx()) {
      show(GlobalGraphOperations.at(db).getAllNodesWithLabel(Labels.SYNONYM));
    }
  }

  private void show(Iterable<Node> iterable) {
    System.out.println("\n\n");
    for (Node n : iterable) {
      NameUsage u = getUsageByNode(n);
      System.out.println("### " + n.getId() + " " + mapper.readScientificName(n));
      System.out.println(u);
    }
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
