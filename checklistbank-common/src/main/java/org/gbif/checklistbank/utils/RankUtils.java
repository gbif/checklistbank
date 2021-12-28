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
package org.gbif.checklistbank.utils;

import org.gbif.api.vocabulary.Rank;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

/**
 *
 */
public class RankUtils {
  private static final Pattern PREFIX = Pattern.compile("^(SUPER|SUB(?:TER)?|INFRA|GIGA|MAGN|GRAND|MIR|NAN|HYPO|MIN|PARV|MEGA|EPI)");
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
      switch (rank) {
        case INFRAGENERIC_NAME:
          return Rank.GENUS;
        case SPECIES_AGGREGATE:
          return Rank.SPECIES;
        case INFRASPECIFIC_NAME:
        case SUBSPECIES:
        case INFRASUBSPECIFIC_NAME:
        case VARIETY:
        case FORM:
        case SUBFORM:
          return Rank.INFRASPECIFIC_NAME;
        default:
          String name = rank.name();
          Matcher m = PREFIX.matcher(name);
          if (m.find()) {
            return Rank.valueOf(m.replaceFirst("").toUpperCase());
          }
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
