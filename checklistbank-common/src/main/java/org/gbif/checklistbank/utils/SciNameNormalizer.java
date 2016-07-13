package org.gbif.checklistbank.utils;

import java.util.regex.Pattern;

import com.google.common.base.Strings;

/**
 * A scientific name normalizer that replaces common misspellings and epithet gender changes.
 */
public class SciNameNormalizer {

  private static final Pattern suffix_a = Pattern.compile("(on|um|us|a)$");
  private static final Pattern i = Pattern.compile(" ([^ jyi]+)[jyi]+");
  private static final Pattern trh = Pattern.compile(" ([^ ]+[tr])h");
  private static final Pattern white = Pattern.compile("\\s{2,}");
  private static final Pattern empty = Pattern.compile("['_-]");
  private static final Pattern removeRepeatedLetter = Pattern.compile("(\\p{L})\\1+");
  private static final Pattern removeHybridSign2 = Pattern.compile("×");
  private static final Pattern removeHybridSign = Pattern.compile("(^|\\s)(?:×|[xX]([A-Z]))");

  public static String normalize(String s) {
    if (Strings.isNullOrEmpty(s)) return null;

    s = s.trim();

    s = removeHybridCross(s);

    s = normalizeLetters(s);

    s = empty.matcher(s).replaceAll("");
    s = white.matcher(s).replaceAll(" ");

    // Only for bi/trinomials, otherwise we mix up ranks.
    if (s.indexOf(' ') > 2) {
      s = stemEpithet(s);
      // normalize frequent variations of i in epithets only
      s = i.matcher(s).replaceAll(" $1i");
      // normalize frequent variations of characters sometimes followed by an 'h' in epithets only
      s = trh.matcher(s).replaceAll(" $1");
    }

    return s.trim();
  }

  /**
   * Does a stemming of a latin epithet and return the female version ending with 'a'.
   */
  public static String stemEpithet(String epithet) {
    if (Strings.isNullOrEmpty(epithet)) return null;
    return suffix_a.matcher(epithet).replaceFirst("a");
  }

  /**
   * Normalize letters and ligatures to their ASCII equivalent and remove repeated letters→leters.
   */
  public static String normalizeLetters(String s) {
    return removeRepeatedLetter.matcher(org.gbif.utils.text.StringUtils.foldToAscii(s)).replaceAll("$1");
  }

  /**
   * Remove a hybrid cross, or a likely hybrid cross.
   */
  public static String removeHybridCross(String s) {
    return removeHybridSign.matcher(s).replaceAll("$1$2");
  }
}
