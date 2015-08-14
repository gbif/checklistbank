package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.model.Constants;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.cli.common.MapDbObjectSerializer;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.postgres.TabMapperBase;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does a lookup by canonical name and then leniently filters by rank, kingdom and authorship.
 * There is no fuzzy matching involved, just simple string normalization to avoid whitespace and punctuation variants.
 *
 * TODO: normalize
 */
public class IdLookupImpl implements IdLookup {
    private static final Logger LOG = LoggerFactory.getLogger(IdLookupImpl.class);

    private final DB db;
    private final Map<String, List<LookupUsage>> usages;
    private final AuthorComparator authComp;
    private int counter = 0;
    private int deleted = 0;

    private IdLookupImpl() {
        db = DBMaker.tempFileDB()
                .fileMmapEnableIfSupported()
                .transactionDisable()
                .make();
        usages = db.hashMapCreate("usages")
                .keySerializer(Serializer.STRING_ASCII)
                .valueSerializer(new MapDbObjectSerializer<ArrayList>(ArrayList.class))
                .make();
        authComp = AuthorComparator.createWithAuthormap();
    }

    /**
     * Creates idlookup with explicit list of known ids.
     */
    public IdLookupImpl(Collection<LookupUsage> usages) {
        this();
        for (LookupUsage u : usages) {
            add(u);
        }
    }

    /**
     * Read old ids from existing, open DAO
     */
    public IdLookupImpl(UsageDao dao) {
        this();
        for (Node n : IteratorUtil.asIterable(dao.allTaxa())) {
            NubUsage u = dao.readNub(n);
            add( new LookupUsage(u.usageKey, u.parsedName.canonicalName(), u.parsedName.getAuthorship(), u.parsedName.getYear(), u.rank, u.kingdom, false) );
        }
    }
    /**
     * Loads known usages from checklistbank backbone.
     */
    public IdLookupImpl(ClbConfiguration clb) throws SQLException, IOException {
        this();
        UsageWriter writer = new UsageWriter();
        LOG.info("Reading existing nub usages from postgres...");
        try (Connection c = clb.connect()){
            final CopyManager cm = new CopyManager((BaseConnection) c);
            cm.copyOut("COPY ("
                    + "SELECT u.id, coalesce(n.canonical_name, n.scientific_name), n.authorship, n.year, u.rank, u.kingdom_fk, deleted is not null"
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
                    Kingdom.byNubUsageId(toInt(row[5])),
                    "t".equals(row[6])
            );
            add(u);
        }

        private Integer toInt(String x) {
            return x == null ? null : Integer.valueOf(x);
        }
    }

    @VisibleForTesting
    protected static String norm(String x) {
        x = StringUtils.normalizeSpace(x);
        if (StringUtils.isBlank(x)) {
            return null;
        }
        return foldToAscii(x).toLowerCase();
    }

    public AuthorComparator getAuthorComparator() {
        return authComp;
    }

    /**
     * Uses the solr.ASCIIFoldingFilter to convert a string to its ASCII equivalent. See solr documentation for full
     * details.
     * </br>
     * When doing the conversion, this method mirrors GBIF's registry-solr schema configuration for
     * <fieldType name="text_auto_ngram">. For example, it uses the KeywordTokenizer that treats the entire string as a
     * single token, regardless of its content. See the solr documentation for more details.
     * </br>
     * This method is needed when checking if the query string matches the dataset title. For example, if the query
     * string is "straÃŸe", it won't match the dataset title "Schulhof Gymnasium HÃ¼rth Bonnstrasse" unless "straÃŸe" gets
     * converted to its ASCII equivalent "strasse".
     *
     * @param x string to fold
     * @return string converted to ASCII equivalent
     * @see org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter
     * @see org.apache.lucene.analysis.core.KeywordTokenizer
     */
    private static String foldToAscii(String x) {
        try {
            StringReader reader = new StringReader(x);
            TokenStream stream = new KeywordTokenizer(reader);
            ASCIIFoldingFilter filter = new ASCIIFoldingFilter(stream);
            CharTermAttribute termAtt = filter.addAttribute(CharTermAttribute.class);
            filter.reset();
            filter.incrementToken();
            // converted x to ASCII equivalent and return it
            return termAtt.toString();
        } catch (IOException e) {
            // swallow
            LOG.warn("Failed to fold to ASCII: {}", x);
            return x;
        }
    }

    private void add(LookupUsage u) {
        String key = norm(u.getCanonical());
        if (usages.containsKey(key)) {
            // we need to create a new list cause mapdb considers them immutable!
            usages.put(key, ImmutableList.<LookupUsage>builder().addAll(usages.get(key)).add(u).build());
        } else {
            usages.put(key, ImmutableList.of(u));
        }
        counter++;
        if (u.isDeleted()) {
            deleted++;
        }
    }

    @Override
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

    @Override
    public LookupUsage match(String canonicalName, @Nullable String authorship, @Nullable String year, Rank rank, Kingdom kingdom) {
        List<LookupUsage> hits = usages.get(norm(canonicalName));
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
            int currCounter = 0;
            for (LookupUsage u : hits) {
                if (!u.isDeleted()) {
                    currCounter++;
                    curr = u;
                }
            }
            if (currCounter == 0) {
                LOG.debug("Ambiguous match with {} deleted hits for {} {} {}", hits.size(), kingdom, rank, canonicalName);
            } else if (currCounter == 1) {
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
    @Override
    public int size() {
        return counter;
    }

    /**
     * @return the number of usage keys known which belong to deleted usages.
     */
    @Override
    public int deletedIds() {
        return deleted;
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
