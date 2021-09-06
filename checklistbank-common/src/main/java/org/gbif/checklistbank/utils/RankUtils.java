package org.gbif.checklistbank.utils;

import com.google.common.collect.Lists;
import org.gbif.api.vocabulary.Rank;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  /**
   * Ignore super- or sub- prefixes of a rank and returns the main Linnean rank if it can be found, the rank as was given otherwise.
   */
  public static Rank linneanBaseRank(Rank rank) {
    if (rank != null) {
      String name = rank.name();
      Pattern PREFIX = Pattern.compile("^(?:super|sub|infra)(.+)$", Pattern.CASE_INSENSITIVE);
      Matcher m = PREFIX.matcher(name);
      if (m.find()) {
        return Rank.valueOf(m.group(1).toUpperCase());
      }
    }
    return rank;
  }

  /**
   * @return true if the ranks given do not contradict each other
   */
  public static boolean match(Rank r1, Rank r2) {
    if (r1 == null || r1 == Rank.UNRANKED ||
        r2 == null || r2 == Rank.UNRANKED) return true;

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
}
