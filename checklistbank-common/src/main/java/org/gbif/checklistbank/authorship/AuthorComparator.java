package org.gbif.checklistbank.authorship;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.model.Equality;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to compare scientific name authorships, i.e. the (recombination) author and the publishing year.
 * Original name (bracket) authorship is not used in the comparison.
 * Author strings are normalized to ASCII and then compared. As authors are often abbreviated in all kind of ways a shared common substring is accepted
 * as a positive equality.
 * If any of the names given has an empty author & year the results will always be Equality.UNKNOWN.
 */
public class AuthorComparator {
  private static final Logger LOG = LoggerFactory.getLogger(AuthorComparator.class);

  private static final Pattern AND = Pattern.compile("( et | and |&|&amp;)", Pattern.CASE_INSENSITIVE);
  private static final Pattern IN = Pattern.compile(" in .+$", Pattern.CASE_INSENSITIVE);
  private static final Pattern EX = Pattern.compile("^.+ ex ", Pattern.CASE_INSENSITIVE);
  private static final Pattern INITIALS = Pattern.compile("\\b[A-Z]\\b");
  private static final Pattern INITIAL_NAME = Pattern.compile("^([a-z])\\s+[a-z]{3,}$");
  private static final Pattern YEAR = Pattern.compile("(^|[^0-9])(\\d{4})([^0-9]|$)");
  private static final String AUTHOR_MAP_FILENAME = "/authorship/authormap.txt";
  private final Map<String, String> authorMap = Maps.newHashMap();

  private final int minCommonSubstring;

  private AuthorComparator(Map<String, String> authors) {
    this.minCommonSubstring = 4;
    for (Map.Entry<String, String> entry : authors.entrySet()) {
      String key = normalize(entry.getKey());
      String val = normalize(entry.getValue());
      if (key != null && val != null) {
        authorMap.put(key, val);
      }
    }
    LOG.info("Created author comparator with {} abbreviation entries", authorMap.size());
  }

  public static AuthorComparator createWithoutAuthormap() {
    return new AuthorComparator(Maps.<String, String>newHashMap());
  }

  public static AuthorComparator createWithAuthormap() {
    try {
      AuthorComparator ac = new AuthorComparator(
          FileUtils.streamToMap(Resources.asByteSource(AuthorComparator.class.getResource(AUTHOR_MAP_FILENAME)).openStream())
      );
      return ac;
    } catch (IOException e) {
      throw new RuntimeException("Failed to load author map from classpath", e);
    }
  }

  public static AuthorComparator createWithAuthormap(Map<String, String> authorMap) {
    return new AuthorComparator(authorMap);
  }

  /**
   * @return ascii only, lower cased string without punctuation. Empty string instead of null
   */
  protected String normalize(String x) {
    if (StringUtils.isBlank(x)) {
      return null;
    }
    // normalize and
    x = AND.matcher(x).replaceAll(" ");
    // manually normalize characters not dealt with by the java Normalizer
    x = StringUtils.replaceChars(x, "ø", "o");

    // remove in publications
    x = IN.matcher(x).replaceAll("");

    // remove ex authors
    x = EX.matcher(x).replaceAll("");

    // fold to ascii
    x = org.gbif.utils.text.StringUtils.foldToAscii(x);

    //remove punctuation
    x = x.replaceAll("\\p{Punct}+", " ");

    // try to remove initials if the remaining string is still large
    if (x.length() > 10) {
      String withoutInitials = INITIALS.matcher(x).replaceAll(" ");
      if (withoutInitials.length() > 10 && withoutInitials.trim().contains(" ")) {
        x = withoutInitials;
      }
    }
    x = StringUtils.normalizeSpace(x);
    if (StringUtils.isBlank(x)) {
      return null;
    }
    x = x.toLowerCase();
    if (authorMap.containsKey(x)) {
      return authorMap.get(x);
    }
    return x;
  }

  public Equality compare(String author1, String year1, String author2, String year2) {
    // compare recombination authors first
    Equality result = compareAuthor(author1, author2, minCommonSubstring);
    if (result != Equality.EQUAL) {
      // if authors are not the same we allow a positive year comparison to override it as author comparison is very difficult
      Equality yresult = compareYear(year1, year2);
      if (yresult != Equality.UNKNOWN) {
        result = yresult;
      }
    }
    return result;
  }

  public Equality compare(ParsedName n1, ParsedName n2) {
    if (!n1.isAuthorsParsed()) {
      parseAuthorship(n1);
    }
    if (!n2.isAuthorsParsed()) {
      parseAuthorship(n2);
    }
    return compare(n1.getAuthorship(), n1.getYear(), n2.getAuthorship(), n2.getYear());
  }

  /**
   * Compares two sets of author & year for equality.
   * This is more strict than the normal compare method and requires both authors and year to match.
   * Author matching is still done fuzzily
   *
   * @return true if both sets match
   */
  public boolean equals(String author1, @Nullable String year1, String author2, @Nullable String year2) {
    // strictly compare authors first
    author1 = normalize(author1);
    author2 = normalize(author2);
    if (author1 == null || !author1.equals(author2)) {
      return false;
    }
    // now also compare the year
    if (year1 == null && year2 == null) {
      return true;
    }
    return Equality.EQUAL == compareYear(year1, year2);
  }

  public boolean equals(String author1, @Nullable String year1, String author2, @Nullable String year2, int minCommonSubstring) {
    // strictly compare authors first
    Equality authorEq = compareAuthor(author1, author2, minCommonSubstring);
    if (authorEq != Equality.EQUAL) {
      return false;
    }
    // now also compare the year
    if (year1 == null && year2 == null) {
      return true;
    }
    return Equality.EQUAL == compareYear(year1, year2);
  }

  /**
   * Extract authorship from the name itself as best as we can to at least do some common string comparison
   */
  private void parseAuthorship(ParsedName pn) {
    // try to use full sciname minus the epithets
    String lastEpithet = coalesce(pn.getInfraSpecificEpithet(), pn.getSpecificEpithet(), pn.getGenusOrAbove());
    if (lastEpithet != null && pn.getScientificName() != null) {
      int idx = pn.getScientificName().lastIndexOf(lastEpithet);
      if (idx >= 0) {
        pn.setAuthorship(pn.getScientificName().substring(idx + lastEpithet.length()));
      }
    }
    // copy full name to year, will be extracted/normalized in year comparison
    pn.setYear(pn.getScientificName());
  }

  //TODO: allow ? and brackets in year comparisons ...
  private Equality compareYear(String y1, String y2) {
    y1 = normalizeYear(y1);
    y2 = normalizeYear(y2);
    if (y1 != null && y2 != null) {
      return y1.equals(y2) ? Equality.EQUAL : Equality.DIFFERENT;
    }
    return Equality.UNKNOWN;
  }

  private String normalizeYear(String y) {
    if (y == null) return null;
    Matcher m = YEAR.matcher(y);
    if (m.find()) {
      return m.group(2);
    }
    return normalize(y);
  }

  private Equality compareAuthor(String a1, String a2, int minCommonSubstring) {
    a1 = normalize(a1);
    a2 = normalize(a2);
    if (a1 != null && a2 != null) {
      if (a1.equalsIgnoreCase(a2)) {
        // we can stop here, authors are equal, thats enough
        return Equality.EQUAL;
      } else {
        String lcs = LongestCommonSubstring.lcs(a1, a2);
        if (lcs.length() >= minCommonSubstring) {
          // do both names have a single initial which is different?
          // this is often the case when authors are relatives like brothers or son & father
          if (singleInitialsDiffer(a1, a2)) {
            return Equality.DIFFERENT;
          } else {
            return Equality.EQUAL;
          }

        } else if (a1.equals(lcs) && a2.startsWith(lcs) || a2.equals(lcs) && a1.startsWith(lcs)) {
          // the smallest common substring is the same as one of the inputs and also the start of the other one.
          // Good enough, likey a short abbreviation
          return Equality.EQUAL;
        }
        return Equality.DIFFERENT;
      }
    }
    return Equality.UNKNOWN;
  }

  private boolean singleInitialsDiffer(String a1, String a2) {
    Matcher m1 = INITIAL_NAME.matcher(a1);
    Matcher m2 = INITIAL_NAME.matcher(a2);
    if (m1.find() && m2.find()) {
      if (!m1.group(1).equals(m2.group(1))) {
        return true;
      }
    }
    return false;
  }

  private Equality compareAuthor2(String a1, String a2) {
    a1 = normalize(a1);
    a2 = normalize(a2);
    if (a1 != null && a2 != null) {
      if (a1.equalsIgnoreCase(a2)) {
        // we can stop here, authors are equal, thats enough
        return Equality.EQUAL;
      } else {
        String lcs = LongestCommonSubstring.lcs(a1, a2);
        if (lcs.length() > 3) {
          return Equality.EQUAL;
        } else if (a1.equals(lcs) || a2.equals(lcs)) {
          // the smallest common substring is the same as one of the inputs. Good enough
          return Equality.EQUAL;
        }
        return Equality.DIFFERENT;
      }
    }
    return Equality.UNKNOWN;
  }

  private static <T> T coalesce(T... items) {
    for (T i : items) if (i != null) return i;
    return null;
  }
}
