package org.gbif.nub.lookup.straight;

import org.gbif.api.model.Constants;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.postgres.TabMapperBase;
import org.gbif.checklistbank.utils.StringNormalizer;
import org.gbif.nub.mapdb.MapDbObjectSerializer;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
        .transactionDisable()
        .make());
  }

  /**
   * Creates or opens a persistent lookup store.
   */
  public static IdLookupImpl temp() {
    return new IdLookupImpl(DBMaker.tempFileDB()
        .fileMmapEnableIfSupported()
        .transactionDisable()
        .make());
  }

  private IdLookupImpl(DB db) {
    this.db = db;
    usages = db.hashMapCreate("usages")
        .keySerializer(Serializer.STRING_ASCII)
        .valueSerializer(new MapDbObjectSerializer<ArrayList>(ArrayList.class, new LookupKryoFactory()))
        .make();
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
  public IdLookupImpl load(ClbConfiguration clb) throws SQLException, IOException {
    UsageWriter writer = new UsageWriter();
    LOG.info("Reading existing nub usages from postgres...");
    try (Connection c = clb.connect()) {
      final CopyManager cm = new CopyManager((BaseConnection) c);
      cm.copyOut("COPY ("
          + "SELECT u.id, coalesce(NULLIF(trim(n.canonical_name), ''), n.scientific_name), n.authorship, n.year, u.rank, u.kingdom_fk, deleted is not null"
          + " FROM name_usage u join name n ON name_fk=n.id"
          + " WHERE dataset_key = '" + Constants.NUB_DATASET_KEY + "')"
          + " TO STDOUT WITH NULL ''", writer);
      LOG.info("Loaded existing nub with {} usages and max key {} into id lookup", usages.size(), keyMax);
    } finally {
      writer.close();
    }
    return this;
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
    return StringNormalizer.foldToAscii(x).toLowerCase();
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
  public LookupUsage match(String canonicalName, @Nullable String authorship, @Nullable String year, Rank rank, Kingdom kingdom) {
    List<LookupUsage> hits = usages.get(norm(canonicalName));
    if (hits == null) return null;
    final boolean compareAuthorship = authorship != null || year != null;
    // filter by rank & kingdom
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
