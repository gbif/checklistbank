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

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.ints.IntSet;

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
