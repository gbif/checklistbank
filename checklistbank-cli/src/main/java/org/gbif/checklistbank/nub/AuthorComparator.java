package org.gbif.checklistbank.nub;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.nub.model.Equality;

import java.text.Normalizer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class AuthorComparator {
  private static final Pattern AND = Pattern.compile("( et | and |&|&amp;)", Pattern.CASE_INSENSITIVE);
  /**
   * @return ascii only, lower cased string without punctuation. Empty string instead of null
   */
  protected static String normalize (String x) {
    if (StringUtils.isBlank(x)) {
      return null;
    }
    // normalize and
    x = AND.matcher(x).replaceAll(" ");
    // manually normalize characters not dealt with by the java Normalizer
    x = StringUtils.replaceChars(x, "ø", "o");

    // use java unicode normalizer to remove accents and punctuation
    x = Normalizer.normalize(x, Normalizer.Form.NFD);
    x = x.replaceAll("[^\\p{ASCII}]", "");
    x = x.replaceAll("\\p{M}", "");
    x = x.replaceAll("\\p{Punct}+", " ");

    x = StringUtils.normalizeSpace(x);
    if (StringUtils.isBlank(x)) {
      return null;
    }
    return x.toLowerCase();
  }

  public Equality equals(ParsedName n1, ParsedName n2) {
    Equality result = Equality.UNKNOWN;
    // compare authors first
    String a1 = normalize(n1.getAuthorship() == null ? n1.getBracketAuthorship() : n1.getAuthorship());
    String a2 = normalize(n2.getAuthorship() == null ? n2.getBracketAuthorship() : n2.getAuthorship());
    if (a1 != null && a2 != null) {
      if (a1.equals(a2)) {
        // we can stop here, authors are equal, thats enough
        return Equality.EQUAL;
      } else {
        // if authors are not the same we allow a positive year comparison to override it as author comparison is very difficult
        result = Equality.DIFFERENT;
      }
    }

    // check year
    String y1 = normalize(n1.getYear() == null ? n1.getBracketYear() : n1.getYear());
    String y2 = normalize(n2.getYear() == null ? n2.getBracketYear() : n2.getYear());
    if (y1 != null && y2 != null) {
      //TODO: allow ? and brackets in year comparisons ...
      return y1.equals(y2) ? Equality.EQUAL : Equality.DIFFERENT;
    }

    return result;
  }
}
