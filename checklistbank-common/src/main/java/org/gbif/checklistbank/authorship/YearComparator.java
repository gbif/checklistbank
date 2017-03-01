package org.gbif.checklistbank.authorship;

import org.apache.commons.lang3.StringUtils;
import org.gbif.checklistbank.model.Equality;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YearComparator {
  private static final Pattern YEAR = Pattern.compile("(^|[^0-9])([0-9?]{4})([^0-9]|$)");

  private String y1;
  private String y2;

  public YearComparator(String y1, String y2) {
    this.y1 = normalizeYear(y1);
    this.y2 = normalizeYear(y2);
  }

  private String normalizeYear(String y) {
    if (y == null) return null;
    Matcher m = YEAR.matcher(StringUtils.deleteWhitespace(y));
    if (m.find()) {
      return m.group(2);
    }
    return AuthorComparator.normalize(y);
  }

  /**
   * @return true if ? placeholders are found and have been replaced
   */
  private boolean replacePlaceholders() {
    int qm1 = y1.indexOf('?');
    int qm2 = y2.indexOf('?');
    if (qm1 >= 0 || qm2 >= 0) {
      StringBuilder s1 = new StringBuilder(y1);
      StringBuilder s2 = new StringBuilder(y2);
      if (qm1 >= 0) {
        s1.setCharAt(qm1, '_');
        if (s2.length() > qm1) {
          s2.setCharAt(qm1, '_');
        }
      }
      if (qm2 >= 0) {
        if (s1.length() > qm2) {
          s1.setCharAt(qm2, '_');
        }
        s2.setCharAt(qm2, '_');
      }
      y1 = s1.toString();
      y2 = s2.toString();
      return true;
    }
    return false;
  }

  /**
   * Compares year strings.
   * If parsable as integers allows for 1 year difference
   * due to frequent confusion between imprint dates and actual dates of publication
   */
  public Equality compare() {
    if (y1 != null && y2 != null) {
      // equal strings are equal
      if (y1.equals(y2)) {
        return Equality.EQUAL;
      }
      // try to parse into ints and allow one year difference
      try {
        int yi1 = Integer.parseInt(y1);
        int yi2 = Integer.parseInt(y2);
        if (Math.abs(yi1 - yi2) <= 1) {
          return Equality.EQUAL;
        }
      } catch (NumberFormatException e) {
        // allow ? in year comparisons
        if (replacePlaceholders() && y1.equals(y2)) {
          return Equality.EQUAL;
        }
      }
      return Equality.DIFFERENT;
    }
    return Equality.UNKNOWN;
  }
}
