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
package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.vocabulary.Rank;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

public class DatasetMatchSummary {
  private final UUID datasetKey;
  private final AtomicInteger unparsable = new AtomicInteger();

  private final AtomicInteger usages = new AtomicInteger();
  private Map<Rank, AtomicInteger> usagesByRank = Maps.newHashMap();

  private final AtomicInteger matches = new AtomicInteger();
  private Map<Rank, AtomicInteger> matchesByRank = Maps.newHashMap();

  public DatasetMatchSummary(UUID datasetKey) {
    this.datasetKey = datasetKey;
  }

  public UUID getDatasetKey() {
    return datasetKey;
  }

  public int getUnparsable() {
    return unparsable.get();
  }

  public int getTotalUsages() {
    return usages.get();
  }

  public int getMatches() {
    return matches.get();
  }

  public void addUnparsable() {
    unparsable.incrementAndGet();
  }

  public void addNoMatch(@Nullable Rank rank) {
    rank = MoreObjects.firstNonNull(rank, Rank.UNRANKED);
    usages.incrementAndGet();
    if (usagesByRank.containsKey(rank)) {
      usagesByRank.get(rank).incrementAndGet();
    } else {
      usagesByRank.put(rank, new AtomicInteger(1));
    }
  }

  public void addMatch(@Nullable Rank rank) {
    rank = MoreObjects.firstNonNull(rank, Rank.UNRANKED);
    addNoMatch(rank);
    matches.incrementAndGet();
    if (matchesByRank.containsKey(rank)) {
      matchesByRank.get(rank).incrementAndGet();
    } else {
      matchesByRank.put(rank, new AtomicInteger(1));
    }
  }

  /**
   * @return percentage of all matched usages
   */
  public int percMatches() {
    return usages.get() == 0 ? 100 : matches.get() * 100 / usages.get();
  }

  /**
   * @return percentage of matched usages with a rank of genus, species or below
   */
  public int percBackboneRelevantNoMatches() {
    final AtomicInteger total = new AtomicInteger();
    final AtomicInteger matches = new AtomicInteger();

    usagesByRank.forEach((rank, cnt) -> {
      if (rank == Rank.GENUS || rank.isSpeciesOrBelow()) {
        total.getAndAdd(cnt.get());
        if (matchesByRank.containsKey(rank)) {
          matches.getAndAdd(matchesByRank.get(rank).get());
        }
      }
    });
    return total.get() == 0 ? 100 : matches.get() * 100 / total.get();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("DatasetMatch ")
        .append(datasetKey)
        .append(": total=")
        .append(usages.get())
        .append(", matches=")
        .append(matches.get())
        .append(", unparsable=")
        .append(unparsable.get())
        .append(", perc=")
        .append(percMatches())
        .append(", percLower=")
        .append(percBackboneRelevantNoMatches());
    // append by rank details
    sb.append("\n");
    final AtomicInteger zero = new AtomicInteger();
    usagesByRank.keySet().stream().sorted().forEach(rank -> {
      sb.append("  ")
          .append(rank)
          .append(": ")
          .append(matchesByRank.getOrDefault(rank, zero).get())
          .append("/")
          .append(usagesByRank.get(rank).get())
          .append("\n");
    });
    return sb.toString();
  }
}
