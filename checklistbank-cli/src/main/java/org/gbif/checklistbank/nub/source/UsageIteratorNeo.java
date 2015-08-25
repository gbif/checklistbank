package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.CloseableIterator;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.postgres.TabMapperBase;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.yammer.metrics.MetricRegistry;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A neo db backed iterable that can be used to iterate over all usages in the source multiple times.
 * The iteration is in taxonomic order, starting with the highest root taxa and walks
 * the taxonomic tree in depth order first, including synonyms.
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
 * Implement the abstract method to init a neo db using the included NeoUsageWriter class.
 */
public abstract class UsageIteratorNeo implements Iterable<SrcUsage>, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(UsageIteratorNeo.class);

    protected final UUID datasetKey;
    protected final String datasetTitle;
    protected Node root;
    private boolean init = false;
    private UsageDao dao;

    /**
     * @param memory in megabytes to be used for a pure in memory storage. Negative or zero values create a persistent dao
     * @param datasetKey
     * @param datasetTitle
     */
    public UsageIteratorNeo(int memory, UUID datasetKey, String datasetTitle) {
        this.datasetKey = datasetKey;
        this.datasetTitle = datasetTitle;
        if (memory > 0) {
            dao = UsageDao.temporaryDao(memory);
        } else {
            NeoConfiguration cfg = new NeoConfiguration();
            cfg.neoRepository = Files.createTempDir();
            dao = UsageDao.persistentDao(cfg, datasetKey, new MetricRegistry("sourcedb"), true);
        }
    }

    abstract void initNeo(NeoUsageWriter writer) throws Exception;

    /**
     * Closes dao and deletes all intermediate persistence files.
     */
    @Override
    public void close() throws IOException {
        dao.closeAndDelete();
    }

    public class SrcUsageIterator implements CloseableIterator<SrcUsage> {
        private final Iterator<Node> nodes;
        private final Transaction tx;


        public SrcUsageIterator(ResourceIterable<Node> nodes) {
            tx = dao.beginTx();
            this.nodes = nodes.iterator();
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
        public void close() throws Exception {
            tx.close();
        }
    }

    @Override
    public CloseableIterator<SrcUsage> iterator() {
        if (!init) {
            try (NeoUsageWriter writer = new NeoUsageWriter(dao)) {
                LOG.info("Start loading source data from {} into neo", datasetTitle);
                initNeo(writer);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
        return new SrcUsageIterator(Traversals.DESCENDANTS.traverse(root).nodes());
    }

    public class NeoUsageWriter extends TabMapperBase {
        private int counter = 0;
        private Transaction tx;
        private IntIntMap ids = new IntIntHashMap();
        private final UsageDao dao;

        public NeoUsageWriter(UsageDao dao) {
            // the number of columns in our query to consume
            super(7);
            this.dao = dao;
            tx = dao.beginTx();
            root = dao.getNeo().createNode();
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

}
