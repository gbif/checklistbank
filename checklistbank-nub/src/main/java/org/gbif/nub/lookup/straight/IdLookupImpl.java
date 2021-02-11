package org.gbif.nub.lookup.straight;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.*;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.postgres.TabMapperBase;
import org.gbif.checklistbank.utils.KingdomUtils;
import org.gbif.checklistbank.utils.NameFormatter;
import org.gbif.checklistbank.utils.RankUtils;
import org.gbif.checklistbank.utils.SciNameNormalizer;
import org.gbif.nub.mapdb.MapDbObjectSerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
            + "SELECT u.id, coalesce(NULLIF(trim(n.canonical_name), ''), n.scientific_name), n.authorship, n.year, u.rank, u.status, u.kingdom_fk, deleted is not null"
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
            + "SELECT u.id, u.parent_fk, u.pp_synonym_fk, coalesce(NULLIF(trim(n.canonical_name), ''), n.scientific_name), n.authorship, n.year, u.rank, u.status, u.kingdom_fk, deleted is not null"
            + " FROM name_usage u join name n ON name_fk=n.id"
            + " WHERE dataset_key = '" + Constants.NUB_DATASET_KEY + "'" + delClause + " AND pp_synonym_fk is not null"
            + " ORDER BY pp_synonym_fk)"
            + " TO STDOUT WITH NULL ''", writer);
        LOG.info("Added {} pro parte usages into id lookup", usages.size() - uCount);
      }

      LOG.info("Loaded existing nub with {} usages ({} deleted) and max key {} into id lookup", usages.size(), deleted, keyMax);
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
   * TaxonomicStatus status
   * Kingdom kingdom
   * boolean deleted
   */
  private class UsageWriter extends TabMapperBase {
    public UsageWriter() {
      // the number of columns in our query to consume
      super(8);
    }

    @Override
    protected void addRow(String[] row) {
      LookupUsage u = new LookupUsage(
          toInt(row[0]),
          row[1],
          row[2],
          row[3],
          Rank.valueOf(row[4]),
          TaxonomicStatus.valueOf(row[5]),
          toKingdom(row[6]),
          "t".equals(row[7])
      );
      add(u);
    }
  }

  /**
   * The writer expects the incoming rows to be sorted by the proParteKey!
   * <p>
   * int key
   * int parentKey
   * int proParteKey
   * String canonical
   * String authorship
   * String year
   * Rank rank
   * TaxonomicStatus status
   * Kingdom kingdom
   * boolean deleted
   */
  private class ProParteUsageWriter extends TabMapperBase {
    private LookupUsage u;
    private Integer lastProParteKey;

    public ProParteUsageWriter() {
      // the number of columns in our query to consume
      super(10);
    }

    @Override
    protected void addRow(String[] row) {
      Integer key = toInt(row[0]);
      Integer parentKey = toInt(row[1]);
      Integer proParteKey = toInt(row[2]);
      boolean deleted = "t".equals(row[9]);
      // only create a new usage if the pro parte key changes
      if (lastProParteKey == null || !lastProParteKey.equals(proParteKey)) {
        // add last if existing
        if (u != null) {
          add(u);
        }
        // start new usage
        lastProParteKey = proParteKey;
        u = new LookupUsage(
            key,
            new Int2IntOpenHashMap(),
            row[3],
            row[4],
            row[5],
            Rank.valueOf(row[6]),
            TaxonomicStatus.valueOf(row[7]),
            toKingdom(row[8]),
            deleted
        );
      }
      // negate key if its a deleted usage
      key = deleted ? -1 * key : key;
      // add parent key -> usage key into map
      u.getProParteKeys().put(parentKey, key);
    }

    @Override
    public void close() throws IOException {
      // we need to add the last usage still
      if (u != null) {
        add(u);
      }
      super.close();
    }
  }

  /**
   * Translates the kingdom_fk into a kingdom enum value.
   * To avoid NPEs it translates null kingdoms into incertae sedis,
   * see http://dev.gbif.org/issues/browse/POR-3202
   *
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
    keyMax = Math.max(keyMax, u.getMaxKey());
  }

  @Override
  public LookupUsage match(String canonicalName, Rank rank, Kingdom kingdom) {
    return match(canonicalName, null, null, rank, TaxonomicStatus.ACCEPTED, kingdom);
  }

  @Override
  public List<LookupUsage> match(String canonicalName) {
    List<LookupUsage> hits = usages.get(norm(canonicalName));
    if (hits != null) {
      return hits;
    }
    return Lists.newArrayList();
  }

  private static boolean ignore(int id, IntSet[] ignoreIDs){
    if (ignoreIDs != null) {
      for (IntSet set : ignoreIDs) {
        if (set.contains(id)) return true;
      }
    }
    return false;
  }

  @Override
  public LookupUsage match(final String canonicalName, @Nullable String authorship, @Nullable String year, Rank rank, @Nullable TaxonomicStatus status, Kingdom kingdom, IntSet... ignoreIDs) {
    final String canonicalNameNormed = norm(canonicalName);
    if (canonicalNameNormed == null) return null;

    List<LookupUsage> hits = usages.get(canonicalNameNormed);
    if (hits == null) return null;

    final boolean lookupWithAuthorship = authorship != null || year != null;
    List<LookupUsage> withAuthormatch = new ArrayList<>();
    List<LookupUsage> canonicalMatch = new ArrayList<>();

    // filter by rank, kingdom & authorship
    Iterator<LookupUsage> iter = hits.iterator();
    while (iter.hasNext()) {
      LookupUsage u = iter.next();
      if (ignore(u.getKey(), ignoreIDs)) {
        iter.remove();
      } else {
        boolean matchWithAuthorship = u.getAuthorship() != null || u.getYear() != null;
        // allow uncertain kingdoms and ranks to match
        if (rank != null && !RankUtils.match(rank, u.getRank()) || kingdom != null && !KingdomUtils.match(kingdom, u.getKingdom())) {
          iter.remove();
        } else {
          // compare authorship. Only keep unknown matches if no authorship was requested
          Equality eq = authComp.compare(authorship, year, u.getAuthorship(), u.getYear());
          if (eq == Equality.DIFFERENT) {
            iter.remove();
          } else {
            if (!matchWithAuthorship) {
              canonicalMatch.add(u);
            } else if (eq == Equality.EQUAL){
              withAuthormatch.add(u);
            }
          }
        }
      }
    }

    if (!withAuthormatch.isEmpty()) {
      hits = withAuthormatch;
    } else if (!lookupWithAuthorship && !canonicalMatch.isEmpty()) {
      hits = canonicalMatch;
    }

    if (hits.size() == 1) {
      return hits.get(0);

    } else if (hits.size() > 1) {
      // try a very exact match first to see if we only get 1 hit
      LookupUsage exact = exactMatch(canonicalName, authorship, year, hits);
      if (exact != null) {
        LOG.debug("{} matches, but only 1 exact match {} for {} {} {} {} {}", hits.size(), exact.getKey(), kingdom, rank, canonicalName, authorship, year);
        return exact;
      }

      // Several matches. If we ever had too many bad usages they might block forever a stable id.

      // If only one current id is matched use that!
      List<LookupUsage> current = hits.stream()
              .filter(u -> !u.isDeleted())
              .collect(Collectors.toList());
      if (current.size() == 1) {
        LOG.debug("{} matches, but only 1 current usage {} for {} {} {} {} {}", hits.size(), current.get(0).getKey(), kingdom, rank, canonicalName, authorship, year);
        return current.get(0);
      }

      // if requested rank & kingdom was clear, snap better to results utilizing the status and prefering current over deleted usages
      LookupUsage match = null;
      if (rank != Rank.UNRANKED && kingdom != Kingdom.INCERTAE_SEDIS) {
        // use only current matches if possible
        if (status != null) {
          match = matchByStatus(status, current);
          if (match == null) {
            match = matchByStatus(status, hits);
          }
        }
      }

      // Still no clear match - pick lowest key
      if (match == null) {
        match = selectLowestKey(hits);
        LOG.debug("Use lowest usage key {} for ambiguous match with {} hits for {} {} {} {} {}", match.getKey(), hits.size(), kingdom, rank, canonicalName, authorship, year);
      }
      return match;
    }
    LOG.debug("No match for {} {} {} {} {}", kingdom, rank, canonicalName, authorship, year);
    return null;
  }

  /**
   * Try to match exactly kingdom, rank, canonical name, authorship and year on current, non deleted names only.
   * @return match or null
   */
  @Override
  public LookupUsage exactCurrentMatch(final ParsedName pn, Kingdom kingdom, IntSet... ignoreIDs) {
    final String canonicalName = norm(NameFormatter.canonicalOrScientificName(pn));
    if (canonicalName == null) return null;

    List<LookupUsage> hits = usages.get(canonicalName);
    if (hits == null) return null;

    // filter by rank, kingdom, authorship and only allow current, non deleted matches
    hits.removeIf(u -> ignore(u.getKey(), ignoreIDs) ||
            u.isDeleted() ||
            pn.getRank() != null && !RankUtils.match(pn.getRank(), u.getRank()) ||
            kingdom != null && !KingdomUtils.match(kingdom, u.getKingdom()) ||
            !Objects.equals(canonicalName, u.getCanonical()) ||
            !Objects.equals(pn.getAuthorship(), u.getAuthorship()) ||
            !Objects.equals(pn.getYear(), u.getYear())
    );

    if (hits.size() == 1) {
      return hits.get(0);

    } else if (hits.size() > 1) {
      LOG.debug("{} exact matches for {} {} {} {} {}", hits.size(), kingdom, pn.getRank(), canonicalName, pn.getAuthorship(), pn.getYear());
    }
    return null;
  }

  private static LookupUsage selectLowestKey(List<LookupUsage> matches) {
    LookupUsage match = null;
    for (LookupUsage u : matches) {
      if (match == null || match.getKey() > u.getKey()) {
        match = u;
      }
    }
    return match;
  }
  
  /**
   * For multiple candidates, filter them by status:
   *     a) If one matches use that
   *     b) If multiple match, use lowest id of those
   *     c) If none matches and candidate to be matched has status of accepted, use lowest existing id of all current matches
   *     d) If none matches and candidate to be matched has a status that is anything but accepted, issue new id
   * @param status status to filter by
   * @return matching usage or null
   */
  private LookupUsage matchByStatus(TaxonomicStatus status, List<LookupUsage> candidates) {
    List<LookupUsage> matches = candidates.stream()
        .filter(u -> status.equals(u.getStatus()))
        .collect(Collectors.toList());
    if (!matches.isEmpty()) {
      return selectLowestKey(matches);
    }
    // no direct status matches. Allow any other for accepted
    if (TaxonomicStatus.ACCEPTED == status) {
      return selectLowestKey(candidates);
    }
    // no exact status matches. Try to merge all synonym/accepted stati
    matches = candidates.stream()
        .filter(u -> status.isAccepted() == u.getStatus().isAccepted())
        .collect(Collectors.toList());
    return selectLowestKey(matches);
  }
  
  /**
   * Checks candidates for a single unambigous exact match
   */
  private LookupUsage exactMatch(String canonicalName, String authorship, String year, List<LookupUsage> candidates) {
    LookupUsage match = null;
    for (LookupUsage u : candidates) {
      if (Objects.equals(canonicalName, u.getCanonical())
          && Objects.equals(authorship, u.getAuthorship())
          && Objects.equals(year, u.getYear())) {
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
    private LookupUsage pp = null;
    private IntIterator ppIter = null;

    public LookupIterator() {
      canonIter = usages.values().iterator();
    }

    @Override
    public boolean hasNext() {
      return (ppIter != null && ppIter.hasNext()) || (iter != null && iter.hasNext()) || canonIter.hasNext();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("You cannot remove from an id lookup");
    }

    @Override
    public LookupUsage next() {
      if (ppIter != null && ppIter.hasNext()) {
        return proParteUsage(ppIter.next());
      }
      if (iter == null || !iter.hasNext()) {
        iter = canonIter.next().iterator();
      }
      LookupUsage u = iter.next();
      if (u.getProParteKeys() != null && !u.getProParteKeys().isEmpty()) {
        pp = u;
        ppIter = u.getProParteKeys().values().iterator();
        return proParteUsage(ppIter.next());
      }
      return u;
    }

    private LookupUsage proParteUsage(int val) {
      LookupUsage ppu = new LookupUsage();
      ppu.setDeleted(val < 0);
      ppu.setKey(Math.abs(val));
      ppu.setKingdom(pp.getKingdom());
      ppu.setRank(pp.getRank());
      ppu.setCanonical(pp.getCanonical());
      ppu.setAuthorship(pp.getAuthorship());
      ppu.setYear(pp.getYear());
      return ppu;
    }
  }
}
