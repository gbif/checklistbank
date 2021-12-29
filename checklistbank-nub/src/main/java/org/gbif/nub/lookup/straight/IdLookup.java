/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nub.lookup.straight;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.authorship.AuthorComparator;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * An (nub) usage matching service that honors the authorship, year and kingdom on top of the canonical name.
 */
public interface IdLookup extends Iterable<LookupUsage>, AutoCloseable {

  /**
   * Lookup a usage by the canonical name, rank and kingdom alone.
   *
   * @return the matching usage or null
   */
  default LookupUsage match(String canonicalName, Rank rank, Kingdom kingdom) {
    return match(canonicalName, null, null, rank, TaxonomicStatus.ACCEPTED, kingdom);
  }

  /**
   * Lookup a usage by all possible parameters but the parentKey which was added very recently.
   *
   * @return the matching usage or null
   */
  LookupUsage match(String canonicalName, @Nullable String authorship, @Nullable String year, Rank rank, TaxonomicStatus status, Kingdom kingdom, IntSet... ignoreIDs);

  LookupUsage exactCurrentMatch(ParsedName name, Kingdom kingdom, IntSet... ignoreIDs);

  /**
   * List all usages with the given canonical name regardless of rank, kingdom or authorship
   */
  @Deprecated
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
