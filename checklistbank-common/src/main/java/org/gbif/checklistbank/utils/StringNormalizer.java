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
        case 0xc6:
          sb.append("Ae");
          break;
        case 0xe6:
          sb.append("ae");
          break;
        case 0xd0:
          sb.append("D");
          break;
        case 0x111:
          sb.append("d");
          break;
        case 0xd8:
          sb.append("O");
          break;
        case 0xf8:
          sb.append("o");
          break;
        case 0x152:
          sb.append("Oe");
          break;
        case 0x153:
          sb.append("oe");
          break;
        case 0x166:
          sb.append("T");
          break;
        case 0x167:
          sb.append("t");
          break;
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }
}
