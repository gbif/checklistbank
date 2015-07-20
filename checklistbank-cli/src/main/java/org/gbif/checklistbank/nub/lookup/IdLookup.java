package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.model.Constants;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.common.MapDbObjectSerializer;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.postgres.TabMapperBase;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import com.google.common.collect.ImmutableList;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdLookup implements Iterable<LookupUsage> {
    private static final Logger LOG = LoggerFactory.getLogger(IdLookup.class);
    private final DB db;
    private final Map<String, List<LookupUsage>> usages;
    private final AuthorComparator authComp;
    private int counter = 0;
    private int deleted = 0;
    private IntSet noCanonical = new IntHashSet();

    private IdLookup() {
        db = DBMaker.tempFileDB()
                .fileMmapEnableIfSupported()
                .transactionDisable()
                .make();
        usages = db.hashMapCreate("usages")
                .keySerializer(Serializer.STRING_ASCII)
                .valueSerializer(new MapDbObjectSerializer<ArrayList>(ArrayList.class))
                .make();
        //TODO: load known author abbreviations
        authComp = new AuthorComparator();
    }

    /**
     * Creates idlookup with explicit list of known ids.
     */
    public IdLookup(Collection<LookupUsage> usages) {
        this();
        for (LookupUsage u : usages) {
            add(u);
        }
    }

    /**
     * Loads known usages from checklistbank backbone.
     */
    public IdLookup(ClbConfiguration clb) throws SQLException, IOException {
        this();
        UsageWriter writer = new UsageWriter();
        LOG.info("Reading existing nub usages from postgres...");
        try (Connection c = clb.connect()){
            final CopyManager cm = new CopyManager((BaseConnection) c);
            cm.copyOut("COPY ("
                    + "SELECT u.id, n.canonical_name, n.authorship, n.year, u.rank, u.kingdom_fk, false"
                    + " FROM name_usage u join name n ON name_fk=n.id"
                    + " WHERE dataset_key = '" + Constants.NUB_DATASET_KEY + "')"
                    + " TO STDOUT WITH NULL ''", writer);
            LOG.info("Loaded existing nub with {} usages into id lookup", usages.size());
        } finally {
            writer.close();
        }
    }

    /**
     * int key
     * String canonical
     * String authorship
     * String year
     * Rank rank
     * Kingdom kingdom
     * boolean deleted
     */
    private class UsageWriter extends TabMapperBase {
        public UsageWriter() {
            // the number of columns in our query to consume
            super(7);
        }

        @Override
        protected void addRow(String[] row) {
            LookupUsage u = new LookupUsage(
                    toInt(row[0]),
                    row[1],
                    row[2],
                    row[3],
                    Rank.valueOf(row[4]),
                    Kingdom.byNubUsageId(Integer.valueOf(row[5])),
                    "t".equals(row[6])
            );
            add(u);
        }

        private Integer toInt(String x) {
            return x == null ? null : Integer.valueOf(x);
        }
    }

    private void add(LookupUsage u) {
        if (u.getCanonical() == null) {
            LOG.warn("Ignore previous usage {} without canonical name", u.getKey());
            noCanonical.add(u.getKey());

        } else {
            if (usages.containsKey(u.getCanonical())) {
                // we need to create a new list cause mapdb considers them immutable!
                usages.put(u.getCanonical(), ImmutableList.<LookupUsage>builder().addAll(usages.get(u.getCanonical())).add(u).build());
            } else {
                usages.put(u.getCanonical(), ImmutableList.of(u));
            }
            counter++;
            if (u.isDeleted()) {
                deleted++;
            }
        }
    }

    public LookupUsage match(String canonicalName, Rank rank, Kingdom kingdom) {
        return match(canonicalName, null, null, rank, kingdom);
    }

    private boolean match(Rank r1, Rank r2) {
        if (r1 == Rank.INFRASPECIFIC_NAME) {
            return r2.isInfraspecific();
        } else if (r1 == Rank.INFRASUBSPECIFIC_NAME) {
            return r2.isInfraspecific() && r2 != Rank.SUBSPECIES;

        } else if (r2 == Rank.INFRASPECIFIC_NAME) {
            return r1.isInfraspecific();
        } else if (r2 == Rank.INFRASUBSPECIFIC_NAME) {
            return r1.isInfraspecific() && r1 != Rank.SUBSPECIES;
        }

        return r1 == r2;
    }

    private boolean match(Kingdom k1, Kingdom k2) {
        if (k1 == Kingdom.INCERTAE_SEDIS || k2 == Kingdom.INCERTAE_SEDIS) {
            return true;
        }
        return k1 == k2;
    }

    public LookupUsage match(String canonicalName, @Nullable String authorship, @Nullable String year, Rank rank, Kingdom kingdom) {
        List<LookupUsage> hits = usages.get(canonicalName);
        if (hits == null) return null;
        final boolean compareAuthorship = authorship != null || year != null;
        // filter by rank & kingdom
        Iterator<LookupUsage> iter = hits.iterator();
        while (iter.hasNext()) {
            LookupUsage u = iter.next();
            // allow uncertain kingdoms and ranks to match
            if ( rank != null && !match(rank, u.getRank())  ||  kingdom != null && !match(kingdom, u.getKingdom())) {
                iter.remove();
            } else if (compareAuthorship) {
                // authorship comparison was requested!
                Equality eq = authComp.compare(authorship, year, u.getAuthorship(), u.getYear());
                if (eq == Equality.DIFFERENT) {
                    iter.remove();
                }
            }
        }
        // if no authorship was requested and we got 1 result, a hit!
        if (hits.size() == 1) {
            return hits.get(0);
        } else if (hits.size() > 1){
            // if we ever had too many bad usages they might block forever a stable id.
            // If only one current id is matched use that!
            //TODO: review if this decision is correct!!!
            LookupUsage curr = null;
            int courrCounter = 0;
            for (LookupUsage u : hits) {
                if (!u.isDeleted()) {
                    courrCounter++;
                    curr = u;
                }
            }
            if (courrCounter == 0) {
                LOG.debug("Ambiguous match with {} deleted hits for {} {} {}", hits.size(), kingdom, rank, canonicalName);
            } else if (courrCounter == 1) {
                LOG.debug("{} matches, but only 1 current usage {} for {} {} {}", hits.size(), curr.getKey(), kingdom, rank, canonicalName);
                return curr;
            } else {
                LOG.debug("Ambiguous match with multiple current usages in {} hits for {} {} {}", hits.size(), kingdom, rank, canonicalName);
            }
        }
        return null;
    }

    /**
     * @return the number of known usage keys incl deleted ones
     */
    public int size() {
        return counter;
    }

    /**
     * @return the number of usage keys known which belong to deleted usages.
     */
    public int deletedIds() {
        return deleted + noCanonical.size();
    }

    public IntSet getNoCanonical() {
        return noCanonical;
    }

    @Override
    public Iterator<LookupUsage> iterator() {
        return new LookupIterator();
    }

    private class LookupIterator implements Iterator<LookupUsage> {
        private final Iterator<List<LookupUsage>> canonIter;
        private Iterator<LookupUsage> iter = null;

        public LookupIterator() {
            canonIter = usages.values().iterator();
        }

        @Override
        public boolean hasNext() {
            return (iter != null && iter.hasNext()) || canonIter.hasNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("You cannot remove from an id lookup");
        }

        @Override
        public LookupUsage next() {
            if (iter == null || !iter.hasNext()) {
                iter = canonIter.next().iterator();
            }
            return iter.next();
        }
    }
}
