package org.gbif.checklistbank.nub.authorship;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.nub.model.Equality;

import java.text.Normalizer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class AuthorComparator {
  private static final Pattern AND = Pattern.compile("( et | and |&|&amp;)", Pattern.CASE_INSENSITIVE);
  private static final Pattern YEAR = Pattern.compile("(^|[^0-9])(\\d{4})([^0-9]|$)");

  private Map<String, String> KNOWN_AUTHORS;

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
    String a1 = getAuthor(n1);
    String a2 = getAuthor(n2);
    if (a1 != null && a2 != null) {
      if (a1.equals(a2)) {
        // we can stop here, authors are equal, thats enough
        return Equality.EQUAL;
      } else {
        String lcs = LongestCommonSubstring.lcs(a1, a2);
        if (lcs.length() > 3) {
          return Equality.EQUAL;
        } else if(a1.equals(lcs) || a2.equals(lcs)) {
          // the smallest common substring is the same as one of the inputs. Good enough
          return Equality.EQUAL;
        }
        // if authors are not the same we allow a positive year comparison to override it as author comparison is very difficult
        result = Equality.DIFFERENT;
      }
    }

    // check year
    String y1 = getYear(n1);
    String y2 = getYear(n2);
    if (y1 != null && y2 != null) {
      return y1.equals(y2) ? Equality.EQUAL : Equality.DIFFERENT;
    }

    return result;
  }

  private String getAuthor(ParsedName pn) {
    // TODO: deal with teams, sort them alphabetically???
    if (pn.isAuthorsParsed()) {
      return normalize(pn.getAuthorship() == null ? pn.getBracketAuthorship() : pn.getAuthorship());
    } else {
      // try to return full sciname minus the epithets
      String lastEpithet = coalesce(pn.getInfraSpecificEpithet(), pn.getSpecificEpithet(), pn.getGenusOrAbove());
      int idx = pn.getScientificName().lastIndexOf(lastEpithet);
      if (idx >= 0) {
        return normalize(pn.getScientificName().substring(idx + lastEpithet.length()));
      }
      return null;
    }
  }

  private static <T> T coalesce(T ...items) {
    for(T i : items) if(i != null) return i;
    return null;
  }

  private String getYear(ParsedName pn) {
    //TODO: allow ? and brackets in year comparisons ...
    if (pn.isAuthorsParsed()) {
      return normalize(coalesce(pn.getYear(), pn.getBracketYear()));
    } else {
      // try to readUsage first year
      Matcher m = YEAR.matcher(pn.getScientificName());
      if (m.find()) {
        String y = m.group(2);
        return y;
      }
    }
    return null;
  }

}
