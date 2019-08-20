package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.LookupUsage;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nub id generator trying to reuse previously existing ids, even if they had been deleted.
 * It will only ever issue the same id once.
 */
public class IdGenerator {
  private static final Logger LOG = LoggerFactory.getLogger(IdGenerator.class);
  private IdLookup lookup;
  private int nextId;
  private IntSet resurrected = new IntOpenHashSet();
  private IntSet reissued = new IntOpenHashSet();
  private List<LookupUsage> created = Lists.newArrayList();
  private final Joiner nameJoiner = Joiner.on(" ").skipNulls();

  /**
   *
   * @param lookup
   * @param idStart
   */
  public IdGenerator(IdLookup lookup, int idStart) {
    this.lookup = lookup;
    Preconditions.checkArgument(idStart < Constants.NUB_MAXIMUM_KEY, "Lowest current backbone id exceeds maximum nub id limit");
    nextId = idStart;
  }

  public int issue(String canonicalName, String authorship, String year, Rank rank, TaxonomicStatus status, Kingdom kingdom) {
    return issue(canonicalName, authorship, year, rank, status, kingdom, null);
  }

  public int issue(String canonicalName, String authorship, String year, Rank rank, TaxonomicStatus status, Kingdom kingdom, Integer parentKey) {
    LookupUsage u = lookup.match(canonicalName, authorship, year, rank, status, kingdom);
    int id;
    if (u == null) {
      id = create(canonicalName, authorship, year, rank, status, kingdom);

    } else {
      final int matchKey = keyOrProParte(u, parentKey);
      if (reissued.contains(matchKey) || resurrected.contains(matchKey)) {
        id = create(canonicalName, authorship, year, rank, status, kingdom);
        LOG.warn("{} {} {} was already issued as {}. Generating new id {} instead", kingdom, rank, canonicalName, matchKey, id);

      } else {
        id = matchKey;
        if (u.isDeleted()) {
          resurrected.add(id);
          LOG.debug("Resurrected id {} for {} {}", id, rank, name(canonicalName, authorship, year));
        } else {
          reissued.add(id);
          LOG.debug("Reissued id {} for {} {}", id, rank, name(canonicalName, authorship, year));
        }
      }
    }
    // make sure we dont exceed the maximum nub id limit which we use to identify nub usages elsewhere
    if (id > Constants.NUB_MAXIMUM_KEY) {
      throw new IllegalStateException("Exceeded maximum nub id limit " + Constants.NUB_MAXIMUM_KEY);
    }
    return id;
  }

  // select best match from pro parte keys if possible, otherwise return match key
  private int keyOrProParte(LookupUsage u, Integer parentKey) {
    if (u.getProParteKeys() != null && parentKey != null && u.getProParteKeys().containsKey(parentKey)) {
      return Math.abs(u.getProParteKeys().get(parentKey));
    }
    return u.getKey();
  }

  private int create(String canonicalName, String authorship, String year, Rank rank, TaxonomicStatus status, Kingdom kingdom) {
    int id = nextId++;
    LOG.debug("New id {} generated for {} {}", id, rank, name(canonicalName, authorship, year));
    created.add(new LookupUsage(id, canonicalName, authorship, year, rank, status, kingdom, false));
    return id;
  }

  /**
   * Forces a reissues of a usage key. Useful if the key is managed outside but the IdGenerator should still keep track of it.
   */
  public int reissue(int id) {
    reissued.add(id);
    LOG.debug("Reissued id {} externally", id);
    return id;
  }

  private String name(String canonicalName, String authorship, String year){
    StringBuilder sb = new StringBuilder();
    sb.append(canonicalName);
    if (authorship != null){
      sb.append(", ");
      sb.append(authorship);
    }
    if (year != null){
      sb.append(", ");
      sb.append(year);
    }
    return sb.toString();
  }

  public void writeReports(File reportingDir) throws IOException {
    // add current date folder
    reportingDir = new File(reportingDir, new Date(System.currentTimeMillis()).toString());
    LOG.info("Writing nub reports to {}", reportingDir.getAbsolutePath());
    if (reportingDir.exists()) {
      FileUtils.deleteDirectory(reportingDir);
    }
    FileUtils.forceMkdir(reportingDir);

    // prepare lists for sorting
    List<LookupUsage> del = Lists.newArrayList();
    List<LookupUsage> res = Lists.newArrayList();
    // also include pro parte usages that are hidden in the main usages proParteKeys property
    streamAll().forEach(u -> {
        if (u.isDeleted()) {
          if (resurrected.contains(u.getKey())) {
            res.add(u);
          }
        } else if (!reissued.contains(u.getKey())) {
          del.add(u);
        }
      }
    );

    // write report files
    print(del, new File(reportingDir, "deleted.txt"));
    print(res, new File(reportingDir, "resurrected.txt"));
    print(created, new File(reportingDir, "created.txt"));
  }

  private Stream<LookupUsage> streamAll(){
    final int characteristics = Spliterator.NONNULL | Spliterator.SIZED | Spliterator.DISTINCT;
    return StreamSupport.stream(Spliterators.spliterator(lookup.iterator(), lookup.size(), characteristics), false)
        .flatMap(u -> u.getProParteKeys() == null ? Stream.of(u) : Stream.concat(Stream.of(u), proParteUsages(u)));
  }

  private static Stream<LookupUsage> proParteUsages(LookupUsage orig) {
    return StreamSupport.stream(orig.getProParteKeys().values().spliterator(), false).map(val -> {
      LookupUsage ppu = new LookupUsage();
      ppu.setDeleted(val < 0);
      ppu.setKey(Math.abs(val));
      ppu.setKingdom(orig.getKingdom());
      ppu.setRank(orig.getRank());
      ppu.setCanonical(orig.getCanonical());
      ppu.setAuthorship(orig.getAuthorship());
      ppu.setYear(orig.getYear());
      return ppu;
    });
  }

  private void print(List<LookupUsage> usages, File f) throws IOException {
    try (Writer writer = org.gbif.utils.file.FileUtils.startNewUtf8File(f)){
      // sort and write
      Collections.sort(usages);
      for (LookupUsage u : usages) {
        writer.write(Integer.toString(u.getKey()));
        writer.write('\t');
        writer.write(u.getRank().name());
        writer.write('\t');
        writer.write(nameJoiner.join(u.getCanonical(), u.getAuthorship(), u.getYear()));
        writer.write('\n');
      }
    }
  }
}
