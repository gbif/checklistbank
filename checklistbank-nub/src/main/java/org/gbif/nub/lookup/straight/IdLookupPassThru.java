package org.gbif.nub.lookup.straight;

import it.unimi.dsi.fastutil.ints.IntSet;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.authorship.AuthorComparator;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;

/**
 * dev/null implementation of an IdLookup doing nothing.
 */
public class IdLookupPassThru implements IdLookup {

  public IdLookupPassThru() {
  }

  @Override
  public LookupUsage match(String canonicalName, Rank rank, Kingdom kingdom) {
    return null;
  }

  @Override
  public LookupUsage match(String canonicalName, @Nullable String authorship, @Nullable String year, Rank rank, TaxonomicStatus status, Kingdom kingdom, IntSet... ignoreIDs) {
    return null;
  }

  @Override
  public LookupUsage exactCurrentMatch(ParsedName name, Kingdom kingdom, IntSet... ignoreIDs) {
    return null;
  }

  @Override
  public List<LookupUsage> match(String canonicalName) {
    return Lists.newArrayList();
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public int deletedIds() {
    return 0;
  }

  @Override
  public Iterator<LookupUsage> iterator() {
    return Lists.<LookupUsage>newArrayList().iterator();
  }

  @Nullable
  @Override
  public AuthorComparator getAuthorComparator() {
    return null;
  }

  @Override
  public void close() throws Exception {

  }
}
