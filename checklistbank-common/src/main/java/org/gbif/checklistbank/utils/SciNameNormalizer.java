package org.gbif.checklistbank.utils;

import java.util.regex.Pattern;

import static org.gbif.utils.text.StringUtils.foldToAscii;


/**
 * A scientific name normalizer that replaces common misspellings and epithet gender changes.
 */
public class SciNameNormalizer {

  private static final Pattern suffix_a = Pattern.compile("(?:on|um|us|a)$");
  private static final Pattern suffix_i = Pattern.compile("ei$");
  private static final Pattern i = Pattern.compile("(?<!\\b)[jyi]+");
  private static final Pattern trh = Pattern.compile("([tr])h", Pattern.CASE_INSENSITIVE);
  private static final Pattern white = Pattern.compile("\\s{2,}");
  private static final Pattern empty = Pattern.compile("['_-]");
  private static final Pattern removeRepeatedLetter = Pattern.compile("(\\p{L})\\1+");
  private static final Pattern removeHybridSignGenus   = Pattern.compile("^\\s*[×xX]\\s*([A-Z])");
  private static final Pattern removeHybridSignEpithet = Pattern.compile("(?:^|\\s)(?:×\\s*|[xX]\\s+)([^A-Z])");

  // dont use guava or commons so we dont have to bundle it for the solr cloud plugin ...
  public static boolean hasContent(String s) {
    return s != null && !(s.trim().isEmpty());
  }

  public static String nullToEmpty(String s) {
    return (s == null) ? "" : s;
  }

  /**
   * Normalizes and entire scientific name, keeping monomials or the first genus part rather unchanged,
   * applying the more drastic normalizatin to the remainder of the name only.
   */
  public static String normalize(String s) {
    return normalize(s, false);
  }

  /**
   * Normalizes an entire name string including monomials and genus parts of a name.
   */
  public static String normalizeAll(String s) {
    return normalize(s, true);
  }

  private static String normalize(String s, boolean normMonomials) {
    if (!hasContent(s)) return "";

    s = s.trim();

   // Remove a hybrid cross, or a likely hybrid cross.
   s = removeHybridSignGenus.matcher(s).replaceAll("$1");
   s = removeHybridSignEpithet.matcher(s).replaceAll(" $1");

    // Normalize letters and ligatures to their ASCII equivalent
    s = foldToAscii(s);

    // normalize whitespace
    s = empty.matcher(s).replaceAll("");
    s = white.matcher(s).replaceAll(" ");

    // Only for bi/trinomials, otherwise we mix up ranks.
    if (normMonomials) {
      s = normStrongly(s);

    } else if (s.indexOf(' ') > 2) {
      String[] parts = s.split(" ", 2);
      s = parts[0] + " " + normStrongly(parts[1]);
    }

    return s.trim();
  }

  private static String normStrongly(String s) {
    // remove repeated letters→leters in binomials
    s = removeRepeatedLetter.matcher(s).replaceAll("$1");

    s = stemEpithet(s);
    // normalize frequent variations of i
    s = i.matcher(s).replaceAll("i");
    s = suffix_i.matcher(s).replaceAll("i");
    // normalize frequent variations of t/r sometimes followed by an 'h'
    return trh.matcher(s).replaceAll("$1");
  }

  /**
   * Does a stemming of a latin epithet and return the female version ending with 'a'.
   */
  public static String stemEpithet(String epithet) {
    if (!hasContent(epithet)) return "";
    return suffix_a.matcher(epithet).replaceFirst("a");
  }

}
