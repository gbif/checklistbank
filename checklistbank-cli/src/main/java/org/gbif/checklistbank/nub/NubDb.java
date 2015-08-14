package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NodeProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubDb {

    private static final Logger LOG = LoggerFactory.getLogger(NubDb.class);
    private final AuthorComparator authComp = AuthorComparator.createWithAuthormap();
    private final int batchSize;
    private Transaction tx;
    protected final UsageDao dao;
    // activity counter to manage transaction commits
    private int counter = 0;
    // number of created nodes
    private int nodes = 0;

    private NubDb(UsageDao dao, int batchSize, boolean initialize) {
        this.batchSize = batchSize;
        this.dao = dao;
        tx = dao.beginTx();
        // create indices?
        if (initialize) {
            Schema schema = dao.getNeo().schema();
            schema.indexFor(Labels.TAXON).on(NodeProperties.CANONICAL_NAME).create();
            renewTx();
        }
    }

    public static NubDb create(UsageDao dao, int batchSize) {
        return new NubDb(dao, batchSize, true);
    }

    /**
     * Opens an existing db without creating a new db schema.
     */
    public static NubDb open(UsageDao dao, int batchSize) {
        return new NubDb(dao, batchSize, false);
    }

    public List<NubUsage> findNubUsages(String canonical) {
        List<NubUsage> usages = Lists.newArrayList();
        for (Node n : IteratorUtil.loop(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.CANONICAL_NAME, canonical))) {
            usages.add(node2usage(n));
        }
        return usages;
    }

    /**
     * @return the parent (or accepted) nub usage for a given node. Will be null for kingdom root nodes.
     */
    public NubUsage getParent(NubUsage child) {
        Node p;
        if (child.status.isSynonym()) {
            p = child.node.getSingleRelationship(RelType.SYNONYM_OF, Direction.OUTGOING).getOtherNode(child.node);
        } else {
            p = child.node.getSingleRelationship(RelType.PARENT_OF, Direction.INCOMING).getOtherNode(child.node);
        }
        return node2usage(p);
    }

    /**
     * Returns the matching accepted nub usage for that canonical name at the given rank.
     */
    public NubUsage findNubUsage(String canonical, Rank rank) {
        List<NubUsage> usages = Lists.newArrayList();
        for (Node n : IteratorUtil.loop(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.CANONICAL_NAME, canonical))) {
            NubUsage rn = node2usage(n);
            if (rank == rn.rank && !rn.status.isSynonym()) {
                usages.add(rn);
            }
        }
        if (usages.isEmpty()) {
            return null;
        } else if (usages.size() == 1) {
            return usages.get(0);
        } else {
            LOG.warn("{} homonyms encountered for {} {}", usages.size(), rank, canonical);
            throw new IllegalStateException("homonym " + canonical);
        }
    }

    /**
     * Tries to find an already existing nub usage for the given source usage.
     *
     * @return the existing usage or null if it does not exist yet.
     */
    public NubUsage findNubUsage(UUID currSource, SrcUsage u, Kingdom uKingdom) {
        List<NubUsage> checked = Lists.newArrayList();
        int canonMatches = 0;
        for (Node n : IteratorUtil.loop(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.CANONICAL_NAME, u.parsedName.canonicalName()))) {
            NubUsage rn = node2usage(n);
            if (matchesNub(u, uKingdom, rn)) {
                checked.add(rn);
                if (!rn.parsedName.hasAuthorship()) {
                    canonMatches++;
                }
            }
        }

        if (checked.size() == 0) {
            return null;
        } else if (checked.size() == 1) {
            return checked.get(0);
        } else if (checked.size() - canonMatches == 1){
            // prefer the single match with authorship!
            for (NubUsage nu : checked) {
                if (nu.parsedName.hasAuthorship()) {
                    return nu;
                }
            }
        } else {
            if (!u.status.isSynonym()) {
                Set<UUID> sources = new HashSet<UUID>();
                for (NubUsage nu : checked) {
                    sources.add(nu.datasetKey);
                }
                if (sources.contains(currSource) && sources.size()==1) {
                    LOG.debug("{} homonyms encountered for {}, but only synonyms from the same source", checked.size(), u.scientificName);
                    return null;
                }
            }
            LOG.warn("{} ambigous homonyms encountered for {}", checked.size(), u.scientificName);
            //TODO: implmement sth even more clever dealing with homonyms!!!
            throw new IgnoreSourceUsageException("homonym " + u.scientificName, u.key, u.scientificName);
        }
        return null;
    }

    private boolean matchesNub(SrcUsage u, Kingdom uKingdom, NubUsage match) {
        if (u.rank != match.rank) {
            return false;
        }
        // no homonmys above genus level!
        if (u.rank.isSuprageneric()) {
            return true;
        }
        Equality author = authComp.compare(u.parsedName, match.parsedName);
        Equality kingdom = compareClassification(uKingdom, match);
        switch (author) {
            case DIFFERENT:
                return false;
            case EQUAL:
                return kingdom != Equality.DIFFERENT;
            case UNKNOWN:
                return kingdom != Equality.DIFFERENT;
            //&& u.status.isSynonym() == match.status.isSynonym()
        }
        return false;
    }

    //TODO: improve classification comparison to more than just kingdom ???
    private Equality compareClassification(Kingdom uKingdom, NubUsage match) {
        if (uKingdom == null || match.kingdom == null) {
            return Equality.UNKNOWN;
        }
        return norm(uKingdom) == norm(match.kingdom) ? Equality.EQUAL : Equality.DIFFERENT;
    }

    public long countTaxa() {
        Result res = dao.getNeo().execute("start node=node(*) match node return count(node) as cnt");
        return (long) res.columnAs("cnt").next();
    }

    /**
     * distinct only 3 larger groups of kingdoms as others are often conflated.
     */
    private Kingdom norm(Kingdom k) {
        switch (k) {
            case ANIMALIA:
            case PROTOZOA:
                return Kingdom.ANIMALIA;
            case PLANTAE:
            case FUNGI:
            case CHROMISTA:
                return Kingdom.PLANTAE;
            default:
                return Kingdom.INCERTAE_SEDIS;
        }
    }

    private NubUsage node2usage(Node n) {
        if (n == null) return null;
        NubUsage nub = dao.readNub(n);
        nub.node = n;
        return nub;
    }

    public NubUsage addUsage(NubUsage parent, SrcUsage src, Origin origin, UUID sourceDatasetKey) {
        LOG.debug("create {} {} {} with parent {}", origin.name().toLowerCase(), src.status.name().toLowerCase(), src.parsedName.fullName(), parent == null ? "none" : parent.parsedName.fullName());

        NubUsage nub = new NubUsage(src);
        nub.datasetKey = sourceDatasetKey;
        nub.origin = origin;
        if (src.key != null) {
            nub.sourceIds.add(src.key);
        }

        return addUsage(parent, nub);
    }

    /**
     * @param parent classification parent or accepted name in case the nub usage has a synonym status
     */
    public NubUsage addUsage(NubUsage parent, NubUsage nub) {
        Preconditions.checkNotNull(parent);
        return add(parent, nub);
    }

    public NubUsage addRoot(NubUsage nub) {
        return add(null, nub);
    }

    /**
     * @param parent classification parent or accepted name in case the nub usage has a synonym status
     */
    private NubUsage add(@Nullable NubUsage parent, NubUsage nub) {
        nub.node = dao.createTaxon();
        nodes++;
        if (parent == null) {
            nub.node.addLabel(Labels.ROOT);
        } else {
            nub.kingdom = parent.kingdom;
            if (nub.status != null && nub.status.isSynonym()) {
                nub.node.addLabel(Labels.SYNONYM);
                nub.node.createRelationshipTo(parent.node, RelType.SYNONYM_OF);
            } else {
                parent.node.createRelationshipTo(nub.node, RelType.PARENT_OF);
            }
        }
        return store(nub);
    }

    /**
     * Removes a nub usage from the db.
     * Only synonyms are currently supported and it is verified that the usage does not have any further relations.
     */
    public void delete(NubUsage nub) {
        dao.delete(nub);
        nodes--;
    }

    /**
     * Creates a new parent relation from parent to child node and removes any previously existing parent relations of
     * the child node
     */
    public void updateParentRel(Node n, Node parent) {
        setSingleRelationship(parent, n, RelType.PARENT_OF);
    }

    public void setSingleRelationship(Node from, Node to, RelType reltype) {
        for (Relationship rel : from.getRelationships(reltype, Direction.OUTGOING)) {
            rel.delete();
        }
        from.createRelationshipTo(to, reltype);
        countAndRenewTx();
    }

    public NubUsage store(NubUsage nub) {
        dao.store(nub);
        countAndRenewTx();
        return nub;
    }

    private void countAndRenewTx() {
        if (counter++ > batchSize) {
            renewTx();
        }
    }

    protected void closeTx() {
        tx.success();
        tx.close();
    }

    protected void openTx() {
        tx = dao.beginTx();
        counter = 0;
    }

    protected void renewTx() {
        closeTx();
        openTx();
    }

    /**
     * @param n      the node to start the parental hierarchy search from
     * @param search the node to find in the hierarchy
     */
    public boolean existsInClassification(Node n, Node search) {
        if (n.equals(search)) {
            return true;
        }
        for (Node p : Traversals.PARENTS.traverse(n).nodes()) {
            if (p.equals(search)) {
                return true;
            }
        }
        return false;
    }
}
