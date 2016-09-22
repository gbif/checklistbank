package org.gbif.checklistbank.utils;

import org.gbif.utils.text.StringUtils;

import java.util.regex.Pattern;


/**
 * A scientific name normalizer that replaces common misspellings and epithet gender changes.
 */
public class SciNameNormalizer {

  private static final Pattern suffix_a = Pattern.compile("(?:on|um|us|a)$");
  private static final Pattern suffix_i = Pattern.compile("ei$");
  private static final Pattern i = Pattern.compile(" ([^ jyi]+)[jyi]+");
  private static final Pattern trh = Pattern.compile(" ([^ ]+[tr])h");
  private static final Pattern white = Pattern.compile("\\s{2,}");
  private static final Pattern empty = Pattern.compile("['_-]");
  private static final Pattern removeRepeatedLetter = Pattern.compile("(\\p{L})\\1+");
  private static final Pattern removeHybridSignGenus   = Pattern.compile("^\\s*[×xX]\\s*([A-Z])");
  private static final Pattern removeHybridSignEpithet = Pattern.compile("(?:^|\\s)(?:×\\s*|[xX]\\s+)([^A-Z])");

  // dont use guava or commons so we dont have to bundle it for the solr cloud plugin ...
  public static boolean hasContent(String s) {
    return s != null && !(s.trim().isEmpty());
  }

  public static String normalize(String s) {
    if (!hasContent(s)) return null;

    s = s.trim();

   // Remove a hybrid cross, or a likely hybrid cross.
   s = removeHybridSignGenus.matcher(s).replaceAll("$1");
   s = removeHybridSignEpithet.matcher(s).replaceAll(" $1");

    // Normalize letters and ligatures to their ASCII equivalent
    s = StringUtils.foldToAscii(s);

    // normalize whitespace
    s = empty.matcher(s).replaceAll("");
    s = white.matcher(s).replaceAll(" ");

    // Only for bi/trinomials, otherwise we mix up ranks.
    if (s.indexOf(' ') > 2) {
      // remove repeated letters→leters in binomials
      s= removeRepeatedLetter.matcher(s).replaceAll("$1");

      s = stemEpithet(s);
      // normalize frequent variations of i in epithets only
      s = i.matcher(s).replaceAll(" $1i");
      s = suffix_i.matcher(s).replaceAll("i");
      // normalize frequent variations of characters sometimes followed by an 'h' in epithets only
      s = trh.matcher(s).replaceAll(" $1");
    }

    return s.trim();
  }

  /**
   * Does a stemming of a latin epithet and return the female version ending with 'a'.
   */
  public static String stemEpithet(String epithet) {
    if (!hasContent(epithet)) return null;
    return suffix_a.matcher(epithet).replaceFirst("a");
  }

}
