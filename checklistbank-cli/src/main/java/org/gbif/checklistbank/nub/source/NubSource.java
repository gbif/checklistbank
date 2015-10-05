package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.postgres.TabMapperBase;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.google.common.io.Files;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A backbone source dataset with some basic metadata that allows to iterate over its source usages.
 *
 * A intermediate neo4j db is created reading from a postgres checklistbank db using the native postgres jdbc copy manager.
 * The init() method connects to an CLB instance and copies all the minimal information needed to build a
 * taxonomic tree into an embedded, persistent neo db. No extension data is copied, just core taxonomic information.
 *
 * This abstract class reads a tab delimited text stream expected with the following columns:
 * <ul>
 * <li>usageKey</li>
 * <li>parentKey</li>
 * <li>basionymKey</li>
 * <li>rank (enum)</li>
 * <li>isSynonym (boolean)</li>
 * <li>taxonomicStatus (enum)</li>
 * <li>nomenclaturalStatus (enum[])</li>
 * <li>scientificName</li>
 * </ul>
 *
 * Implement the abstract initNeo method to supply such a tab delimited stream to the NeoUsageWriter instance.
 */

public abstract class NubSource implements CloseableIterable<SrcUsage> {
    private static final Logger LOG = LoggerFactory.getLogger(NubSource.class);

    private static final NeoConfiguration cfg = new NeoConfiguration();
    static {
        cfg.neoRepository = Files.createTempDir();
        cfg.cacheType = NeoConfiguration.CacheType.NONE;
        cfg.mappedMemory = 256;
    }

    public UUID key;
    public String name;
    public int priority = 0;
    public Rank ignoreRanksAbove = Rank.FAMILY;
    public Date created;
    public boolean nomenclator = false;

    private UsageDao dao;

    /**
     * Loads data into the source and does any other initialization needed before usages() can be called.
     * Make sure to call this method once before the usage iterator is used!
     */
    public void init() throws Exception {
        // load data into neo4j
        LOG.debug("Start loading source data from {} into neo", name);
        try (NeoUsageWriter writer = new NeoUsageWriter(UsageDao.persistentDao(cfg, key, false, null, true))) {
            initNeo(writer);
        }
    }

    abstract void initNeo(NeoUsageWriter writer) throws Exception;

    public class NeoUsageWriter extends TabMapperBase {
        private int counter = 0;
        private Transaction tx;
        private IntIntMap ids = new IntIntHashMap();
        private final UsageDao dao;
        private Node root;

        public NeoUsageWriter(UsageDao dao) {
            // the number of columns in our query to consume
            super(7);
            this.dao = dao;
            tx = dao.beginTx();
            root = dao.getNeo().createNode(Labels.ROOT);
        }

        @Override
        protected void addRow(String[] row) {
            SrcUsage u = new SrcUsage();
            u.key = toInt(row[0]);
            u.parentKey = toInt(row[1]);
            u.originalNameKey = toInt(row[2]);
            u.rank = row[3] == null ? null : Rank.valueOf(row[3]);
            u.status = row[4] == null ? null : TaxonomicStatus.valueOf(row[4]);
            if (u.status == null) {
                LOG.error("Source usage {} missing required taxonomic status", row[0]);
            }
            u.nomStatus = toNomStatus(row[5]);
            u.scientificName = row[6];
            counter++;
            Node n = getOrCreate(u.key);
            dao.storeSourceUsage(n, u);
            // root?
            if (u.parentKey == null) {
                root.createRelationshipTo(n, RelType.PARENT_OF);
            } else {
                int pid = u.parentKey;
                Node p = getOrCreate(pid);
                if (u.status.isSynonym()) {
                    n.createRelationshipTo(p, RelType.SYNONYM_OF);
                } else {
                    p.createRelationshipTo(n, RelType.PARENT_OF);
                }
            }
            // establish basionym a relation?
            if (u.originalNameKey != null) {
                int oid = u.originalNameKey;
                Node o = getOrCreate(oid);
                o.createRelationshipTo(n, RelType.BASIONYM_OF);
            }
            if (counter % 1000 == 0) {
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
    }

    public class SrcUsageIterator implements CloseableIterator<SrcUsage> {
        private final Transaction tx;
        private final ResourceIterator<Node> nodes;
        private final Node root;

        public SrcUsageIterator(UsageDao dao) {
            tx = dao.beginTx();
            root = IteratorUtil.first(dao.getNeo().findNodes(Labels.ROOT));
            this.nodes = Traversals.DESCENDANTS.traverse(root).nodes().iterator();
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
            dao = UsageDao.persistentDao(cfg, key, true, null, false);
        }
        return new SrcUsageIterator(dao);
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
