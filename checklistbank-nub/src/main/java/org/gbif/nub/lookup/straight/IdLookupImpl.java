package org.gbif.nub.lookup.straight;

import org.gbif.api.model.Constants;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.postgres.TabMapperBase;
import org.gbif.checklistbank.utils.SciNameNormalizer;
import org.gbif.nub.mapdb.MapDbObjectSerializer;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.apache.commons.lang3.StringUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does a lookup by canonical name and then leniently filters by rank, kingdom and authorship.
 * There is no fuzzy matching involved, just simple string normalization to avoid whitespace and punctuation variants.
 * TODO: normalize
 */
public class IdLookupImpl implements IdLookup {
  private static final Logger LOG = LoggerFactory.getLogger(IdLookupImpl.class);

  private final DB db;
  private final Map<String, List<LookupUsage>> usages;
  private final AuthorComparator authComp;
  private int keyMax = 0;
  private int counter = 0;
  private int deleted = 0;

  /**
   * Creates or opens a persistent lookup store.
   */
  public static IdLookupImpl persistent(File db) {
    return new IdLookupImpl(DBMaker.fileDB(db)
        .fileMmapEnableIfSupported()
        .make());
  }

  /**
   * Creates or opens a persistent lookup store.
   */
  public static IdLookupImpl temp() {
    return new IdLookupImpl(DBMaker.tempFileDB()
        .fileMmapEnableIfSupported()
        .make());
  }

  private IdLookupImpl(DB db) {
    this.db = db;
    usages = db.hashMap("usages")
        .keySerializer(Serializer.STRING_ASCII)
        .valueSerializer(new MapDbObjectSerializer(ArrayList.class, new LookupKryoFactory()))
        .createOrOpen();
    authComp = AuthorComparator.createWithAuthormap();
  }

  /**
   * Loads idlookup with explicit list of known ids.
   */
  public IdLookupImpl load(Iterable<LookupUsage> usages) {
    int counter = 0;
    for (LookupUsage u : usages) {
      add(u);
      counter++;
    }
    LOG.info("Use {} existing nub with max key {} into id lookup", counter, keyMax);
    return this;
  }

  /**
   * Loads known usages from checklistbank backbone.
   */
  public IdLookupImpl load(ClbConfiguration clb, boolean includeDeleted) throws SQLException, IOException {
    try (Connection c = clb.connect()) {
      final CopyManager cm = new CopyManager((BaseConnection) c);
      final String delClause = includeDeleted ? "" : " AND deleted is null";

      // first read bulk of regular usages - we add pro parte usage later
      LOG.info("Reading existing nub usages {}from postgres ...", includeDeleted ? "incl. deleted " : "");
      try (Writer writer = new UsageWriter()) {
        cm.copyOut("COPY ("
            + "SELECT u.id, coalesce(NULLIF(trim(n.canonical_name), ''), n.scientific_name), n.authorship, n.year, u.rank, u.kingdom_fk, deleted is not null"
            + " FROM name_usage u join name n ON name_fk=n.id"
            + " WHERE dataset_key = '" + Constants.NUB_DATASET_KEY + "'" + delClause + " AND pp_synonym_fk is null)"
            + " TO STDOUT WITH NULL ''", writer);
        LOG.info("Added {} nub usages into id lookup", usages.size());
      }
      final int uCount = usages.size();

      // now load pro parte keys separately saving us from doing complex aggregations
      LOG.info("Reading existing pro parte nub usages {}from postgres ...", includeDeleted ? "incl. deleted " : "");
      try (Writer writer = new ProParteUsageWriter()) {
        cm.copyOut("COPY ("
            + "SELECT u.id, u.parent_fk, u.pp_synonym_fk, coalesce(NULLIF(trim(n.canonical_name), ''), n.scientific_name), n.authorship, n.year, u.rank, u.kingdom_fk, deleted is not null"
            + " FROM name_usage u join name n ON name_fk=n.id"
            + " WHERE dataset_key = '" + Constants.NUB_DATASET_KEY + "'" + delClause + " AND pp_synonym_fk is not null)"
            + " ORDER BY pp_synonym_fk"
            + " TO STDOUT WITH NULL ''", writer);
        LOG.info("Added {} pro parte usages into id lookup", usages.size()-uCount);
      }
      LOG.info("Loaded existing nub with {} usages and max key {} into id lookup", usages.size(), keyMax);
    }
    return this;
  }

  @Override
  public void close() throws Exception {
    db.close();
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
          toKingdom(row[5]),
          "t".equals(row[6])
      );
      add(u);
    }
  }

  /**
   * The writer expects the incoming rows to be sorted by the proParteKey!
   *
   * int key
   * int parentKey
   * int proParteKey
   * String canonical
   * String authorship
   * String year
   * Rank rank
   * Kingdom kingdom
   * boolean deleted
   */
  private class ProParteUsageWriter extends TabMapperBase {
    private LookupUsage u;
    private Integer lastProParteKey;

    public ProParteUsageWriter() {
      // the number of columns in our query to consume
      super(9);
    }

    @Override
    protected void addRow(String[] row) {
      Integer key = toInt(row[0]);
      Integer parentKey = toInt(row[1]);
      Integer proParteKey = toInt(row[2]);
      // only create a new usage if the pro parte key changes
      if (lastProParteKey == null || !lastProParteKey.equals(proParteKey)) {
        add(u);
        u = new LookupUsage(
            key,
            new Int2IntOpenHashMap(),
            row[3],
            row[4],
            row[5],
            Rank.valueOf(row[6]),
            toKingdom(row[7]),
            "t".equals(row[8])
        );
      }
      // add parent key -> usage key into map
      u.getProParteKeys().put(parentKey, key);
    }

    @Override
    public void close() throws IOException {
      // we need to add the last usage still
      add(u);
      super.close();
    }
  }

  /**
   * Translates the kingdom_fk into a kingdom enum value.
   * To avoid NPEs it translates null kingdoms into incertae sedis,
   * see http://dev.gbif.org/issues/browse/POR-3202
   * @return matching kingdom or incertae sedis in case of null (which should *never* happen!)
   */
  private static Kingdom toKingdom(String x) {
    Integer usageKey = toInt(x);
    return usageKey == null ? Kingdom.INCERTAE_SEDIS : Kingdom.byNubUsageKey(usageKey);
  }

  private static Integer toInt(String x) {
    return x == null ? null : Integer.valueOf(x);
  }

  @VisibleForTesting
  protected static String norm(String x) {
    x = SciNameNormalizer.normalize(x);
    return StringUtils.isBlank(x) ? null : x.toLowerCase();
  }

  /**
   * @return the largest usage key existing in the backbone
   */
  public int getKeyMax() {
    return keyMax;
  }

  public AuthorComparator getAuthorComparator() {
    return authComp;
  }

  private void add(LookupUsage u) {
    String key = norm(u.getCanonical());
    if (key == null) {
      LOG.warn("Missing canonical name for {} usage {}", u.getKingdom(), u.getKey());
      return;
    }

    if (usages.containsKey(key)) {
      // we need to persistent a new list cause mapdb considers them immutable!
      usages.put(key, ImmutableList.<LookupUsage>builder().addAll(usages.get(key)).add(u).build());
    } else {
      usages.put(key, ImmutableList.of(u));
    }
    counter++;
    if (u.isDeleted()) {
      deleted++;
    }
    keyMax = u.getKey() > keyMax ? u.getKey() : keyMax;
  }

  @Override
  public LookupUsage match(String canonicalName, Rank rank, Kingdom kingdom) {
    return match(canonicalName, null, null, rank, kingdom);
  }

  @Override
  public List<LookupUsage> match(String canonicalName) {
    List<LookupUsage> hits = usages.get(norm(canonicalName));
    if (hits != null) {
      return hits;
    }
    return Lists.newArrayList();
  }

  private boolean match(Rank r1, Rank r2) {
    if (r1 == Rank.UNRANKED || r2 == Rank.UNRANKED) return true;

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
  public LookupUsage match(final String canonicalName, @Nullable String authorship, @Nullable String year, Rank rank, Kingdom kingdom) {
    final String canonicalNameNormed = norm(canonicalName);
    if (canonicalNameNormed == null) return null;

    List<LookupUsage> hits = usages.get(canonicalNameNormed);
    if (hits == null) return null;

    final boolean compareAuthorship = authorship != null || year != null;
    // filter by rank, kingdom & authorship
    Iterator<LookupUsage> iter = hits.iterator();
    while (iter.hasNext()) {
      LookupUsage u = iter.next();
      // allow uncertain kingdoms and ranks to match
      if (rank != null && !match(rank, u.getRank()) || kingdom != null && !match(kingdom, u.getKingdom())) {
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

    } else if (hits.size() > 1) {
      // try a very exact match first to see if we only get 1 hit
      LookupUsage exact = exactMatch(canonicalName, authorship, year, hits);
      if (exact != null) {
        LOG.debug("{} matches, but only 1 exact match {} for {} {} {} {} {}", hits.size(), exact.getKey(), kingdom, rank, canonicalName, authorship, year);
        return exact;
      }

      // Still several matches
      // If we ever had too many bad usages they might block forever a stable id.
      // If only one current id is matched use that!
      LookupUsage curr = null;
      int currCounter = 0;
      for (LookupUsage u : hits) {
        if (!u.isDeleted()) {
          currCounter++;
          curr = u;
        }
      }
      if (currCounter == 1) {
        LOG.debug("{} matches, but only 1 current usage {} for {} {} {} {} {}", hits.size(), curr.getKey(), kingdom, rank, canonicalName, authorship, year);
        return curr;

      } else if (rank != Rank.UNRANKED && kingdom != Kingdom.INCERTAE_SEDIS) {
        // if requested rank & kingdom was clear, use usage with lowest key
        for (LookupUsage u : hits) {
          if (curr == null || curr.getKey() > u.getKey()) {
            curr = u;
          }
        }
        LOG.debug("Use lowest usage key {} for ambiguous match with {} hits for {} {} {} {} {}", curr.getKey(), hits.size(), kingdom, rank, canonicalName, authorship, year);
        return curr;
      }
    }
    return null;
  }

  /**
   * Checks candidates for a single unambigous exact match
   */
  private LookupUsage exactMatch(String canonicalName, String authorship, String year, List<LookupUsage> candidates) {
    LookupUsage match = null;
    for (LookupUsage u : candidates) {
      if (Objects.equals(canonicalName, u.getCanonical())
        && Objects.equals(authorship, u.getAuthorship())
        && Objects.equals(year, u.getYear()))
      {
        // did we have a match already?
        if (match != null) {
          return null;
        }
        // no, keep it
        match = u;
      }

    }
    return match;
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

  @Override
  public Spliterator<LookupUsage> spliterator() {
    return Spliterators.spliteratorUnknownSize(iterator(), 0);
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
