package org.gbif.checklistbank.utils;

import java.text.Normalizer;
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

  private static Pattern MARKER = Pattern.compile("\\p{M}");

  // dont use guava or commons so we dont have to bundle it for the solr cloud plugin ...
  public static boolean hasContent(String s) {
    return s != null && !(s.trim().isEmpty());
  }

  public static String nullToEmpty(String s) {
    return (s == null) ? "" : s;
  }

  public static String normalize(String s) {
    if (!hasContent(s)) return null;

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

  /**
   * Removes accents & diacretics and converts ligatures into several chars
   *
   * WARNING: this is a copied version from gbif-common StringUtils to keep dependencies for solr plugins small
   * @param x string to fold into ASCII
   * @return string converted to ASCII equivalent, expanding common ligatures
   */
  private static String foldToAscii(String x) {
    if (x == null) {
      return null;
    }
    x = replaceSpecialCases(x);
    // use java unicode normalizer to remove accents
    x = Normalizer.normalize(x, Normalizer.Form.NFD);
    return MARKER.matcher(x).replaceAll("");
  }

  /**
   * The Normalizer misses a few cases and 2 char ligatures which we deal with here
   */
  private static String replaceSpecialCases(String x) {
    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < x.length(); i++) {
      char c = x.charAt(i);
      switch (c) {
        case 'ß':
          sb.append("ss");
          break;
        case 'Æ':
          sb.append("AE");
          break;
        case 'æ':
          sb.append("ae");
          break;
        case 'Ð':
          sb.append("D");
          break;
        case 'đ':
          sb.append("d");
          break;
        case 'ð':
          sb.append("d");
          break;
        case 'Ø':
          sb.append("O");
          break;
        case 'ø':
          sb.append("o");
          break;
        case 'Œ':
          sb.append("OE");
          break;
        case 'œ':
          sb.append("oe");
          break;
        case 'Ŧ':
          sb.append("T");
          break;
        case 'ŧ':
          sb.append("t");
          break;
        case 'Ł':
          sb.append("L");
          break;
        case 'ł':
          sb.append("l");
          break;
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }

}
