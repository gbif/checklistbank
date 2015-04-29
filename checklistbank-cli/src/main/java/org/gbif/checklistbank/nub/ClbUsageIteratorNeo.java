package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.common.CloseableIterator;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.utils.file.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import net.openhft.koloboke.collect.map.IntIntMap;
import net.openhft.koloboke.collect.map.hash.HashIntIntMaps;
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
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A neo db backed iterable that can be used to iterate over all usages in the source multiple times.
 * The iteration is in taxonomic order, starting with the highest root taxa and walks
 * the taxonomic tree in depth order first, including synonyms.
 *
 * At creation time the instance connects to an CLB instance and copies all the minimal information needed to build a
 * taxonomic tree into an embedded neo db.
 */
public class ClbUsageIteratorNeo implements Iterable<SrcUsage>, Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(ClbUsageIteratorNeo.class);

  private final GraphDatabaseService db;
  private final NubSource source;
  private final File neoDir;
  private NeoMapper mapper = NeoMapper.instance();
  private Node root;

  public ClbUsageIteratorNeo(ClbConfiguration clb, NubSource source) throws SQLException, IOException {
    this.source = source;
    neoDir = FileUtils.createTempDir();
    GraphDatabaseFactory factory = new GraphDatabaseFactory();
    GraphDatabaseBuilder builder = factory.newEmbeddedDatabaseBuilder(neoDir.getAbsolutePath())
      .setConfig(GraphDatabaseSettings.keep_logical_logs, "false")
      .setConfig(GraphDatabaseSettings.cache_type, "soft")
      .setConfig(GraphDatabaseSettings.pagecache_memory, "1G");
    db = builder.newGraphDatabase();

    final Connection c = clb.connect();
    final CopyManager cm = new CopyManager((BaseConnection) c);
    loadFromClb(cm);
    c.close();
  }

  private void loadFromClb(CopyManager cm) throws IOException, SQLException {
    LOG.info("Start loading source data from {}", source.name);
    SrcUsageWriter writer = new SrcUsageWriter(db);
    cm.copyOut("COPY ("
               + "SELECT u.id, u.parent_fk, u.basionym_fk, u.rank, u.status, u.nom_status, "
               +        "n.canonical_name, n.type, n.genus_or_above, n.specific_epithet, n.authorship, n.year_int "
               + "FROM name_usage u join name n ON name_fk=n.id "
               + "WHERE dataset_key = '" + source.key + "') "
               + "TO STDOUT WITH NULL ''", writer);
    writer.close();
    LOG.info("Loaded nub source data {} with {} usages into neo4j at {}", source.name, writer.counter, neoDir.getAbsolutePath());
  }

  /**
   * Cleans up all tmp files
   */
  @Override
  public void close() throws IOException {
    db.shutdown();
    org.apache.commons.io.FileUtils.deleteQuietly(neoDir);
  }

  public class SrcUsageWriter extends TabMapperBase {
    private final GraphDatabaseService db;
    private int counter = 0;
    private Transaction tx;
    private IntIntMap ids = HashIntIntMaps.newMutableMap();

    public SrcUsageWriter(GraphDatabaseService db) {
      // the number of columns in our query to consume
      super(12);
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
      u.status = row[4] == null ? null : TaxonomicStatus.valueOf(row[4]);
      u.nomStatus = toNomStatus(row[5]);
      u.canonical = row[6];
      u.nameType = NameType.valueOf(row[7]); // mandatory field!;
      u.genus = row[8];
      u.epithet= row[9];
      u.author= row[10]; // is this the entire authorship or just the recomb one???
      u.year = toInt(row[11]);

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
    TraversalDescription parentsTraversal = db.traversalDescription()
      .relationships(RelType.PARENT_OF, Direction.OUTGOING)
      .depthFirst()
      .evaluator(Evaluators.excludeStartPosition());
    return new SrcUsageIterator(parentsTraversal.traverse(root).nodes());
  }
}
