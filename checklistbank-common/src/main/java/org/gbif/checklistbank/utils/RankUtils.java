package org.gbif.checklistbank.utils;

import org.gbif.api.vocabulary.Rank;

import java.util.List;

import com.google.common.collect.Lists;

/**
 *
 */
public class RankUtils {
  private static List<Rank> LINNEAN_RANKS_REVERSE = Lists.reverse(Rank.LINNEAN_RANKS);

  public static Rank nextLowerLinneanRank(Rank rank) {
    for (Rank r : Rank.LINNEAN_RANKS) {
      if (r.ordinal() > rank.ordinal()) {
        return r;
      }
    }
    return null;
  }

  public static Rank nextHigherLinneanRank(Rank rank) {
    for (Rank r : LINNEAN_RANKS_REVERSE) {
      if (r.ordinal() < rank.ordinal()) {
        return r;
      }
    }
    return null;
  }
}
