package org.gbif.checklistbank.nub.source;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.ExclusionEvaluator;
import org.gbif.checklistbank.neo.traverse.MultiRootNodeIterator;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.neo.traverse.TreeIterablesSorted;
import org.gbif.checklistbank.nub.NubBuilder;
import org.gbif.checklistbank.nub.ParentStack;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.postgres.TabMapperBase;
import org.gbif.common.parsers.utils.NameParserUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
 * <li>namePublishedIn</li>
 * </ul>
 * Implement the abstract initNeo method to supply such a tab delimited stream to the NeoUsageWriter instance.
 */

public abstract class NubSource implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(NubSource.class);

  protected final NeoConfiguration cfg;
  private final Stopwatch watch = Stopwatch.createUnstarted();
  private final Evaluator exclusionEvaluator;
  // default parser with 1s timeout
  private NameParser parser = NameParserUtils.PARSER;
  public UUID key;
  public String name;
  public Rank ignoreRanksAbove = Rank.FAMILY;
  public Date created;
  public boolean nomenclator = false;
  public boolean supragenericHomonymSource = false;
  public boolean includeOTUs = false;
  boolean ignoreSynonyms = false;
  public RankedName defaultParent;
  Map<NameType, NameType> nameTypeMapping;
  private UsageDao dao;
  private final boolean useTmpDao;

  /**
   * @param useTmpDao if true uses a temporary DAO that is not closed at the end of init.
   *                  If too many sources are created this can result in a large number of open files!
   *                  Do not use this for production.
   */
  public NubSource(UUID key, String name, @Nullable List<RankedName> exclusion, boolean useTmpDao, NeoConfiguration cfg) {
    this.cfg = cfg;
    this.key = Preconditions.checkNotNull(key);
    this.name = name;
    this.useTmpDao = useTmpDao;
    if (exclusion != null && !exclusion.isEmpty()) {
      exclusionEvaluator = new ExclusionEvaluator(exclusion);
      LOG.info("Exclude {} taxa from source {} {}", exclusion.size(), key, name);
    } else {
      exclusionEvaluator = null;
    }
  }

  /**
   * Loads data into the source and does any other initialization needed before usages() can be called.
   * Make sure to call this method once before the usage iterator is used!
   *
   * @param writeNeoProperties if true the scientific name and rank will also be added to the neo node properties
   * @param nubRanksOnly       if true skip non nub ranks
   */
  public void init(boolean writeNeoProperties, boolean nubRanksOnly) throws Exception {
    // load data into neo4j
    LOG.debug("Start loading source data from {} {} into neo", key, name);
    watch.reset().start();

    UsageDao dao;
    if (useTmpDao) {
      dao = UsageDao.temporaryDao(128);
      // reuse the dao for reading
      this.dao = dao;
    } else {
      dao = openOrCreate(true);
    }

    // NeoUsageWriter will autoclose persistent daos
    try (NeoUsageWriter writer = new NeoUsageWriter(dao, writeNeoProperties, nubRanksOnly)) {
      initNeo(writer);
      LOG.info("Loaded nub source with {} usages from {} {} into neo4j in {}ms. {} unparsable, skipping {}", writer.getCounter(), key, name, watch.elapsed(TimeUnit.MILLISECONDS), writer.getUnparsable(), writer.getSkipped());
    }

  }

  public RankedName getDefaultParent() {
    return defaultParent;
  }

  public void setDefaultParent(Rank rank, String canonicalName) {
    this.defaultParent = new RankedName(canonicalName, rank);
  }

  public void setParser(NameParser parser) {
    this.parser = parser;
  }

  protected abstract void initNeo(NeoUsageWriter writer) throws Exception;

  public class NeoUsageWriter extends TabMapperBase {
    private int counter = 0;
    private int unparsable = 0;
    private int skipped = 0;
    private Transaction tx;
    private Int2IntMap ids = new Int2IntOpenHashMap();
    private Int2ObjectMap<Integer> nonNubRankUsages = new Int2ObjectOpenHashMap<>();
    private final UsageDao dao;
    private final boolean writeNeoProperties;
    private final boolean nubRanksOnly;


    /**
     * @param writeNeoProperties if true the scientific name and rank will also be added to the neo node properties
     * @param nubRanksOnly       if true skip non nub ranks
     */
    public NeoUsageWriter(UsageDao dao, boolean writeNeoProperties, boolean nubRanksOnly) {
      // the number of columns in our query to consume
      super(8);
      this.dao = dao;
      this.writeNeoProperties = writeNeoProperties;
      this.nubRanksOnly = nubRanksOnly;
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
      u.publishedIn = row[7];

      if (ignoreSynonyms && u.status != null && u.status.isSynonym()) {
        LOG.debug("Skipping synonym {}: {}", u.key, u.scientificName);
        skipped++;
        return;
      }

      if (nubRanksOnly) {
        if ((u.rank == null || !NubBuilder.NUB_RANKS.contains(u.rank))) {
          // do not persist a node, just keep the id mapped to the next higher parent with an nub rank
          nonNubRankUsages.put((int) u.key, u.parentKey);
          // we might have created a node already, delete it if there is one
          if (ids.containsKey((int) u.key)) {
            Node n = dao.getNeo().getNodeById(ids.get(u.key));
            // delete all relations and relink parent rel to next nub rank
            while (u.parentKey != null && nonNubRankUsages.containsKey((int) u.parentKey)) {
              u.parentKey = nonNubRankUsages.get((int) u.parentKey);
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
          while (u.parentKey != null && nonNubRankUsages.containsKey((int) u.parentKey)) {
            u.parentKey = nonNubRankUsages.get((int) u.parentKey);
          }
          if (u.originalNameKey != null && nonNubRankUsages.containsKey((int) u.originalNameKey)) {
            u.originalNameKey = null;
          }
        }
      }

      u.parsedName = parser.parseQuietly(u.scientificName, u.rank);
      if (u.parsedName.isParsableType() && !u.parsedName.isParsed()) {
        LOG.debug("Failed to parse {} {}: {}", u.rank, u.key, u.scientificName);
        unparsable++;
      }

      // custom name type mapping for fine control mostly of OTU names e.g. GTDB
      if (nameTypeMapping != null && nameTypeMapping.containsKey(u.parsedName.getType())) {
        u.parsedName.setType(nameTypeMapping.get(u.parsedName.getType()));
      }

      if (!includeOTUs && u.parsedName.getType() == NameType.OTU) {
        LOG.debug("Skipping OTU {}: {}", u.key, u.scientificName);
        skipped++;
        return;
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
    protected NomenclaturalStatus[] toNomStatus(String x) {
      return null;
    }

    private Integer toInt(String x) {
      return Strings.isNullOrEmpty(x) ? null : Integer.valueOf(x);
    }

    @Override
    public void close() throws IOException {
      tx.success();
      tx.close();
      if (!useTmpDao) {
        dao.compact();
        dao.close();
      }
    }

    private void renewTx() {
      tx.success();
      tx.close();
      tx = dao.beginTx();
    }

    /**
     * @return number of all usages
     */
    public int getCounter() {
      return counter;
    }

    /**
     * @return number of unparsable usages
     */
    public int getUnparsable() {
      return unparsable;
    }

    /**
     * @return number of usages skipped and not inlcuded in the neo source
     */
    public int getSkipped() {
      return skipped;
    }
  }

  public class SrcUsageIterator implements CloseableIterator<SrcUsage> {
    private final Transaction tx;
    private final ResourceIterator<Node> nodes;

    public SrcUsageIterator(UsageDao dao) {
      tx = dao.beginTx();
      nodes = MultiRootNodeIterator.create(
          TreeIterablesSorted.findRoot(dao.getNeo()),
          exclusionEvaluator == null ? Traversals.TREE : Traversals.TREE.evaluator(exclusionEvaluator)
      ).iterator();
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
      nodes.close();
      tx.success();
      tx.close();
    }
  }

  /**
   * Returns a neo db backed iterator over all usages.
   * The iteration is in taxonomic order, starting with the highest root taxa and walks
   * the taxonomic tree in depth order first, including synonyms.
   *
   * WARN: you need to close this iterator to free up all neo4j resources.
   * Otherwise this will lead to a memory leak!
   * This is why we do not implement the convenient iterables interface in NubSOurce (anymore).
   */
  public CloseableIterator<SrcUsage> iterator() {
    if (dao == null) {
      dao = openOrCreate(false);
    }
    return new SrcUsageIterator(dao);
  }

  public UsageDao getDao() {
    return dao;
  }

  /**
   * @return a new or existing dao
   */
  private UsageDao openOrCreate(boolean eraseExisting) {
    if (useTmpDao) {
      throw new IllegalStateException("Temporary DAOs cannot be opened again");
    }
    return UsageDao.persistentDao(cfg, key, null, eraseExisting);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NubSource srcUsages = (NubSource) o;
    return Objects.equals(key, srcUsages.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key);
  }
}
