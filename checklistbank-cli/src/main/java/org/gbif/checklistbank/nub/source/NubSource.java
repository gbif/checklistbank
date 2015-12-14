package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.TreeIterables;
import org.gbif.checklistbank.nub.NubBuilder;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.postgres.TabMapperBase;
import org.gbif.nameparser.NameParser;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A backbone source dataset with some basic metadata that allows to iterate over its source usages.
 * A intermediate neo4j db is created reading from a postgres checklistbank db using the native postgres jdbc copy manager.
 * The init() method connects to an CLB instance and copies all the minimal information needed to build a
 * taxonomic tree into an embedded, persistent neo db. No extension data is copied, just core taxonomic information.
 * This abstract class reads a tab delimited text stream expected with the following columns:
 * <ul>
 * <li>usageKey</li>
 * <li>parentKey</li>
 * <li>basionymKey</li>
 * <li>rank (enum)</li>
 * <li>taxonomicStatus (enum)</li>
 * <li>nomenclaturalStatus (enum[])</li>
 * <li>scientificName</li>
 * </ul>
 * Implement the abstract initNeo method to supply such a tab delimited stream to the NeoUsageWriter instance.
 */

public abstract class NubSource implements CloseableIterable<SrcUsage> {
  private static final Logger LOG = LoggerFactory.getLogger(NubSource.class);

  private static final NeoConfiguration cfg = new NeoConfiguration();

  static {
    cfg.neoRepository = Files.createTempDir();
    cfg.mappedMemory = 256;
  }

  public UUID key;
  public String name;
  public Rank ignoreRanksAbove = Rank.FAMILY;
  public Date created;
  public boolean nomenclator = false;

  private UsageDao dao;

  /**
   * Loads data into the source and does any other initialization needed before usages() can be called.
   * Make sure to call this method once before the usage iterator is used!
   *
   * @param writeNeoProperties if true the scientific name and rank will also be added to the neo node properties
   */
  public void init(boolean writeNeoProperties, boolean nubRanksOnly) throws Exception {
    // load data into neo4j
    LOG.debug("Start loading source data from {} into neo", name);
    try (NeoUsageWriter writer = new NeoUsageWriter(UsageDao.persistentDao(cfg, key, false, null, true), writeNeoProperties, nubRanksOnly)) {
      initNeo(writer);
    }
  }

  public void setNeoRepository(File repository) {
    Preconditions.checkArgument(repository.isDirectory());
    cfg.neoRepository = repository;
  }

  abstract void initNeo(NeoUsageWriter writer) throws Exception;

  public class NeoUsageWriter extends TabMapperBase {
    private int counter = 0;
    private int skipped = 0;
    private Transaction tx;
    private IntIntMap ids = new IntIntHashMap();
    private IntObjectMap<Integer> nonNubRankUsages = new IntObjectHashMap<Integer>();
    private final UsageDao dao;
    private final boolean writeNeoProperties;
    private final boolean nubRanksOnly;
    private final NameParser parser;


    public NeoUsageWriter(UsageDao dao, boolean writeNeoProperties, boolean nubRanksOnly) {
      // the number of columns in our query to consume
      super(7);
      this.dao = dao;
      this.writeNeoProperties = writeNeoProperties;
      this.nubRanksOnly = nubRanksOnly;
      // we only need a parser in case we need to write neo properties
      parser = writeNeoProperties ? new NameParser() : null;
      tx = dao.beginTx();
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
      u.scientificName = row[6];

      if (nubRanksOnly) {
        if ((u.rank == null || !NubBuilder.NUB_RANKS.contains(u.rank))) {
          // do not create a node, just keep the id mapped to the next higher parent with an nub rank
          nonNubRankUsages.put(u.key, u.parentKey);
          // we might have created a node already, delete it if there is one
          if (ids.containsKey(u.key)) {
            Node n = dao.getNeo().getNodeById(ids.get(u.key));
            // delete all relations and relink parent rel to next nub rank
            while (u.parentKey != null && nonNubRankUsages.containsKey(u.parentKey)) {
              u.parentKey = nonNubRankUsages.get(u.parentKey);
            }
            Node nubParent = null;
            if (u.parentKey != null) {
              nubParent = getOrCreate(u.parentKey);
            }
            for (Relationship rel : n.getRelationships()) {
              if (rel.isType(RelType.PARENT_OF)) {
                if (nubParent != null) {
                  Node child = rel.getOtherNode(n);
                  nubParent.createRelationshipTo(child, RelType.PARENT_OF);
                }
              }
              rel.delete();
            }
            n.delete();
          }
          skipped++;
          return;

        } else {
          // make sure the parent and basionym are nub ranks
          while (u.parentKey != null && nonNubRankUsages.containsKey(u.parentKey)) {
            u.parentKey = nonNubRankUsages.get(u.parentKey);
          }
          if (u.originalNameKey != null && nonNubRankUsages.containsKey(u.originalNameKey)) {
            u.originalNameKey = null;
          }
        }
      }

      counter++;
      Node n = getOrCreate(u.key);
      dao.storeSourceUsage(n, u);
      // also add neo properties?
      if (writeNeoProperties) {
        n.setProperty(NeoProperties.SCIENTIFIC_NAME, u.scientificName);
        String canonical = parser.parseToCanonical(u.scientificName, u.rank);
        if (canonical != null) {
          n.setProperty(NeoProperties.CANONICAL_NAME, canonical);
        }
        if (u.rank != null) {
          n.setProperty(NeoProperties.RANK, u.rank.ordinal());
        }
      }

      // root?
      if (u.parentKey == null) {
        n.addLabel(Labels.ROOT);

      } else {
        int pid = u.parentKey;
        Node p = getOrCreate(pid);
        if (u.status.isSynonym()) {
          n.createRelationshipTo(p, RelType.SYNONYM_OF);
          n.addLabel(Labels.SYNONYM);
        } else {
          p.createRelationshipTo(n, RelType.PARENT_OF);
        }
      }
      // establish basionym a relation?
      if (u.originalNameKey != null) {
        Node o = getOrCreate(u.originalNameKey);
        o.createRelationshipTo(n, RelType.BASIONYM_OF);
        o.addLabel(Labels.BASIONYM);
      }
      if (counter % 10000 == 0) {
        renewTx();
      }
    }

    private Node getOrCreate(int key) {
      if (ids.containsKey(key)) {
        return dao.getNeo().getNodeById(ids.get(key));
      } else {
        Node n = dao.createTaxon();
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
      dao.close();
    }

    private void renewTx() {
      tx.success();
      tx.close();
      tx = dao.beginTx();
    }

    public int getCounter() {
      return counter;
    }

    public int getSkipped() {
      return skipped;
    }
  }

  public class SrcUsageIterator implements CloseableIterator<SrcUsage> {
    private final Transaction tx;
    private final Iterator<Node> nodes;

    public SrcUsageIterator(UsageDao dao) {
      tx = dao.beginTx();
      this.nodes = TreeIterables.allNodes(dao.getNeo(), null, null, true).iterator();
    }

    @Override
    public boolean hasNext() {
      return nodes.hasNext();
    }

    @Override
    public SrcUsage next() {
      return dao.readSourceUsage(nodes.next());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() {
      tx.close();
    }
  }

  /**
   * Returns a neo db backed iterator over all usages.
   * The iteration is in taxonomic order, starting with the highest root taxa and walks
   * the taxonomic tree in depth order first, including synonyms.
   */
  @Override
  public CloseableIterator<SrcUsage> iterator() {
    if (dao == null) {
      dao = open(true);
    }
    return new SrcUsageIterator(dao);
  }

  /**
   * @return a new read only dao
   */
  public UsageDao open(boolean readOnly) {
    return UsageDao.persistentDao(cfg, key, readOnly, null, false);
  }

  /**
   * Closes dao and deletes all intermediate persistence files.
   */
  @Override
  public void close() {
    if (dao != null) {
      dao.closeAndDelete();
    }
  }

}
