package org.gbif.nub.lookup.straight;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.authorship.AuthorComparator;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * An (nub) usage matching service that honors the authorship, year and kingdom on top of the canonical name.
 */
public interface IdLookup extends Iterable<LookupUsage>, AutoCloseable {

  /**
   * Lookup a usage by the canonical name, rank and kingdom alone.
   *
   * @return the matching usage or null
   */
  LookupUsage match(String canonicalName, Rank rank, Kingdom kingdom);

  /**
   * Lookup a usage by all possible parameters but the parentKey which was added very recently.
   *
   * @return the matching usage or null
   */
  LookupUsage match(String canonicalName, @Nullable String authorship, @Nullable String year, Rank rank, Kingdom kingdom);

  /**
   * List all usages with the given canonical name regardless of rank, kingdom or authorship
   */
  List<LookupUsage> match(String canonicalName);

  /**
   * @return the number of known usage keys incl deleted ones
   */
  int size();

  /**
   * @return the number of usage keys known which belong to deleted usages.
   */
  int deletedIds();

  @Override
  Iterator<LookupUsage> iterator();

  /**
   * @return the author comparator used
   */
  @Nullable
  AuthorComparator getAuthorComparator();
}
