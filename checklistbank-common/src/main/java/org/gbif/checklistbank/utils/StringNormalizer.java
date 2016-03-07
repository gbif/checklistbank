package org.gbif.checklistbank.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 *
 */
public class StringNormalizer {
  private static Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
  private static Pattern MARKER = Pattern.compile("\\p{M}");

  /**
   * @param x string to fold
   * @return string converted to ASCII equivalent, expanding common ligatures
   */
  public static String foldToAscii(String x) {
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
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }

}
