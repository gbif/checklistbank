package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.CloseableIterator;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.utils.file.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.google.common.base.Throwables;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A neo db backed iterable that can be used to iterate over all usages in the source multiple times.
 * The iteration is in taxonomic order, starting with the highest root taxa and walks
 * the taxonomic tree in depth order first, including synonyms.
 *
 * This abstract class reads a tab delimited text stream expected with the following columns:
 * <ul>
 *   <li>usageKey</li>
 *   <li>parentKey</li>
 *   <li>basionymKey</li>
 *   <li>rank (enum)</li>
 *   <li>isSynonym (boolean)</li>
 *   <li>taxonomicStatus (enum)</li>
 *   <li>nomenclaturalStatus (enum[])</li>
 *   <li>scientificName</li>
 * </ul>
 *
 * Implement the abstract method to init a neo db using the included NeoUsageWriter class.
 */
public abstract class UsageIteratorNeo implements Iterable<SrcUsage>, Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(UsageIteratorNeo.class);

  protected final GraphDatabaseService db;
  protected final NubSource source;
  protected final File neoDir;
  protected final NeoMapper mapper = NeoMapper.instance();
  protected Node root;
  private boolean init = false;

  public UsageIteratorNeo(NubSource source) throws Exception {
    this.source = source;
    neoDir = FileUtils.createTempDir();
    GraphDatabaseFactory factory = new GraphDatabaseFactory();
    GraphDatabaseBuilder builder = factory.newEmbeddedDatabaseBuilder(neoDir.getAbsolutePath())
      .setConfig(GraphDatabaseSettings.keep_logical_logs, "false")
      .setConfig(GraphDatabaseSettings.cache_type, "soft")
      .setConfig(GraphDatabaseSettings.pagecache_memory, "1G");
    db = builder.newGraphDatabase();
  }

  abstract void initNeo(NeoUsageWriter writer) throws Exception;

  /**
   * Cleans up all tmp files
   */
  @Override
  public void close() throws IOException {
    db.shutdown();
    org.apache.commons.io.FileUtils.deleteQuietly(neoDir);
  }

  public class SrcUsageIterator implements CloseableIterator<SrcUsage>{
    private final Iterator<Node> nodes;
    private final Transaction tx;


    public SrcUsageIterator(ResourceIterable<Node> nodes) {
      tx = db.beginTx();
      this.nodes = nodes.iterator();
    }

    @Override
    public boolean hasNext() {
      return nodes.hasNext();
    }

    @Override
    public SrcUsage next() {
      SrcUsage u = new SrcUsage();
      mapper.read(nodes.next(), u);
      return u;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() throws Exception {
      tx.close();
    }
  }

  @Override
  public CloseableIterator<SrcUsage> iterator() {
    if (!init) {
      try (NeoUsageWriter writer = new NeoUsageWriter(db)) {
        LOG.info("Start loading source data from {} into neo", source.name);
        initNeo(writer);
      } catch (Exception e) {
        Throwables.propagate(e);
      }
    }
    TraversalDescription parentsTraversal = db.traversalDescription()
      .relationships(RelType.PARENT_OF, Direction.OUTGOING)
      .depthFirst()
      .evaluator(Evaluators.excludeStartPosition());
    return new SrcUsageIterator(parentsTraversal.traverse(root).nodes());
  }

  public class NeoUsageWriter extends TabMapperBase {
    private final GraphDatabaseService db;
    private int counter = 0;
    private Transaction tx;
    private IntIntMap ids = new IntIntHashMap();

    public NeoUsageWriter(GraphDatabaseService db) {
      // the number of columns in our query to consume
      super(8);
      this.db = db;
      tx = db.beginTx();
      root = db.createNode();
    }

    @Override
    protected void addRow(String[] row) {
      SrcUsage u = new SrcUsage();
      u.key = toInt(row[0]);
      u.parentKey = toInt(row[1]);
      u.originalNameKey = toInt(row[2]);
      u.rank = row[3] == null ? null : Rank.valueOf(row[3]);
      boolean synonym = "t".equals(row[4]);
      u.status = row[5] == null ? null : TaxonomicStatus.valueOf(row[5]);
      if (u.status == null) {
        u.status = synonym ? TaxonomicStatus.SYNONYM : TaxonomicStatus.ACCEPTED;
      } else if (u.status.isSynonym() && !synonym  ||  !u.status.isSynonym() && synonym) {
        LOG.warn("Source usage flagged as {} has contradictory status {}", synonym, row[5]);
      }
      u.nomStatus = toNomStatus(row[6]);
      u.scientificName = row[7];

      counter++;
      Node n = getOrCreate(u.key);
      mapper.store(n, u, false);
      // root?
      if (u.parentKey == null) {
        root.createRelationshipTo(n, RelType.PARENT_OF);
      } else {
        int pid = u.parentKey;
        Node p = getOrCreate(pid);
        p.createRelationshipTo(n, RelType.PARENT_OF);
      }

      if (counter % 1000 == 0) {
        renewTx();
      }
    }

    private Node getOrCreate(int key) {
      if (ids.containsKey(key)) {
        return db.getNodeById(ids.get(key));
      } else {
        Node n = db.createNode(Labels.TAXON);
        ids.put(key, (int) n.getId());
        return n;
      }
    }

    // TODO: implement {NOM, NOM} parsing
    private NomenclaturalStatus[] toNomStatus(String x) {
      return null;
    }

    private Integer toInt(String x) {
      return x == null ? null : Integer.valueOf(x);
    }

    @Override
    public void close() throws IOException {
      tx.success();
      tx.close();
    }

    private void renewTx() {
      tx.success();
      tx.close();
      tx = db.beginTx();
    }

    public int getCounter() {
      return counter;
    }
  }

}
