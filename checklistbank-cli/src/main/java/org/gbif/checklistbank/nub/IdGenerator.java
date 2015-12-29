package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.nub.lookup.IdLookup;
import org.gbif.checklistbank.nub.lookup.LookupUsage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.util.Collections;
import java.util.List;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
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
  private final int idStart;
  private int nextId;
  private IntSet resurrected = new IntHashSet();
  private IntSet reissued = new IntHashSet();
  private List<LookupUsage> created = Lists.newArrayList();
  private final Joiner nameJoiner = Joiner.on(" ").skipNulls();

  /**
   *
   * @param lookup
   * @param idStart
   */
  public IdGenerator(IdLookup lookup, int idStart) {
    this.lookup = lookup;
    Preconditions.checkArgument(idStart < Constants.NUB_MAXIMUM_KEY);
    this.idStart = idStart;
    nextId = idStart;
  }

  public int issue(String canonicalName, String authorship, String year, Rank rank, Kingdom kingdom, boolean forceNew) {
    LookupUsage u = forceNew ? null : lookup.match(canonicalName, authorship, year, rank, kingdom);
    int id = -1;
    if (u == null) {
      id = nextId++;
      LOG.debug("New id {} generated for {} {} {}", id, canonicalName, authorship, year);
      created.add(new LookupUsage(id, canonicalName, authorship, year, rank, kingdom, false));
    } else if (reissued.contains(u.getKey()) || resurrected.contains(u.getKey())) {
      id = nextId++;
      LOG.warn("{} {} {} was already issued as {}. Generating new id {} instead", kingdom, rank, canonicalName, u.getKey(), id);
      LOG.debug("New id {} generated for {} {} {}", id, canonicalName, authorship, year);
      created.add(new LookupUsage(id, canonicalName, authorship, year, rank, kingdom, false));
    } else {
      id = u.getKey();
      if (u.isDeleted()) {
        resurrected.add(id);
        LOG.debug("Resurrected id {} for {} {} {}", id, canonicalName, authorship, year);
      } else {
        reissued.add(id);
        LOG.debug("Reissued id {} for {} {} {}", id, canonicalName, authorship, year);
      }
    }
    // make sure we dont exceed the maximum nub id limit which we use to identify nub usages elsewhere
    if (id > Constants.NUB_MAXIMUM_KEY) {
      throw new IllegalStateException("Exceeded maximum nub id limit " + Constants.NUB_MAXIMUM_KEY);
    }
    return id;
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
    for (LookupUsage u : lookup) {
      if (u.isDeleted()) {
        if (resurrected.contains(u.getKey())) {
          res.add(u);
        }
      } else {
        if (!reissued.contains(u.getKey())) {
          del.add(u);
        }
      }
    }

    // write report files
    print(del, new File(reportingDir, "deleted.txt"));
    print(res, new File(reportingDir, "resurrected.txt"));
    print(created, new File(reportingDir, "created.txt"));
  }

  private void print(List<LookupUsage> usages, File f) throws IOException {
    try (FileWriter writer = new FileWriter(f)){
      // sort and write
      Collections.sort(usages);
      for (LookupUsage u : usages) {
        writer.write(u.getKey());
        writer.write('\t');
        writer.write(u.getRank().name());
        writer.write('\t');
        writer.write(nameJoiner.join(u.getCanonical(), u.getAuthorship(), u.getYear()));
        writer.write('\n');
      }
    }
  }
}
