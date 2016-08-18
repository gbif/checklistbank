package org.gbif.checklistbank.authorship;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.model.Equality;
import org.gbif.utils.ObjectUtils;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to compare scientific name authorships, i.e. the recombination and basionym author and publishing year.
 * Author strings are normalized to ASCII and then compared. As authors are often abbreviated in all kind of ways a shared common substring is accepted
 * as a positive equality.
 * If any of the names given has an empty author & year the results will always be Equality.UNKNOWN.
 *
 * The class exposes two kind of compare methods. A strict one always requiring both year and author to match
 * and a more lax default comparison that only looks at years when the authors differ (as it is quite hard to compare authors)
 */
public class AuthorComparator {
  private static final Logger LOG = LoggerFactory.getLogger(AuthorComparator.class);

  private static final Pattern AND = Pattern.compile("( et | and |&|&amp;)", Pattern.CASE_INSENSITIVE);
  private static final Pattern IN = Pattern.compile(" in .+$", Pattern.CASE_INSENSITIVE);
  private static final Pattern EX = Pattern.compile("^.+ ex ", Pattern.CASE_INSENSITIVE);
  private static final Pattern FIL = Pattern.compile("([A-Z][a-z]*)[\\. ]\\s*f(:?il)?\\.?\\b");
  private static final Pattern TRANSLITERATIONS = Pattern.compile("([auo])e", Pattern.CASE_INSENSITIVE);
  private static final Pattern SURNAME = Pattern.compile("([a-z]+)(:? filius)?$");
  private static final Pattern FIRST_INITIALS = Pattern.compile("^([a-z]\\s)+");
  private static final Pattern YEAR = Pattern.compile("(^|[^0-9])(\\d{4})([^0-9]|$)");
  private static final String AUTHOR_MAP_FILENAME = "/authorship/authormap.txt";
  private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}&&[^,]]+");
  private static final Pattern COMMA = Pattern.compile("\\s*,\\s*");
  private static final Splitter AUTHOR_SPLITTER = Splitter.on(",").omitEmptyStrings();
  private final Map<String, String> authorMap;
  private static final int MIN_AUTHOR_LENGTH_WITHOUT_LOOKUP = 4;

  private final int minCommonSubstring;

  private AuthorComparator(Map<String, String> authors) {
    Map<String, String> map = Maps.newHashMap();
    this.minCommonSubstring = 4;
    int counter=0;
    for (Map.Entry<String, String> entry : authors.entrySet()) {
      String key = normalize(entry.getKey());
      String val = normalize(entry.getValue());
      if (key != null && val != null) {
        map.put(key, val);
        counter++;
      }
    }
    this.authorMap = ImmutableMap.copyOf(map);
    LOG.info("Created author comparator with {} abbreviation entries", counter);
  }

  public static AuthorComparator createWithoutAuthormap() {
    return new AuthorComparator(Maps.<String, String>newHashMap());
  }

  public static AuthorComparator createWithAuthormap() {
    try {
      AuthorComparator ac = new AuthorComparator(
          FileUtils.streamToMap(Resources.asByteSource(AuthorComparator.class.getResource(AUTHOR_MAP_FILENAME)).openStream(),
              Maps.<String, String>newHashMap(), 0, 2, true)
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
   * Compares the author and year of two names by first evaluating equivalence of the authors.
   * Only if they appear to differ also a year comparison is done which can still yield an overall EQUAL in case years match.
   */
  public Equality compare(@Nullable String authors1, @Nullable String year1, @Nullable String authors2, @Nullable String year2) {
    // compare recombination authors first
    Equality result = compareAuthorteam(authors1, authors2, minCommonSubstring, MIN_AUTHOR_LENGTH_WITHOUT_LOOKUP);
    if (result != Equality.EQUAL) {
      // if authors are not the same we allow a positive year comparison to override it as author comparison is very difficult
      Equality yresult = compareYear(year1, year2);
      if (yresult != Equality.UNKNOWN) {
        result = yresult;
      }
    }
    return result;
  }

  /**
   * Does a comparison of recombination and basionym authorship using the author compare method once for the recombination authorship and once for the basionym.
   */
  public Equality compare(ParsedName n1, ParsedName n2) {
    if (!n1.isAuthorsParsed()) {
      // copy parsed name to not alter the original
      n1 = clone(n1);
      parseAuthorship(n1);
    }
    if (!n2.isAuthorsParsed()) {
      // copy parsed name to not alter the original
      n2 = clone(n2);
      parseAuthorship(n2);
    }

    Equality recomb = compare(n1.getAuthorship(), n1.getYear(), n2.getAuthorship(), n2.getYear());
    if (recomb == Equality.DIFFERENT) {
      // in case the recomb author differs we are done, no need for basionym authorship comparison
      return recomb;
    }
    Equality original = compare(n1.getBracketAuthorship(), n1.getBracketYear(), n2.getBracketAuthorship(), n2.getBracketYear());
    if (recomb == Equality.UNKNOWN && original == Equality.UNKNOWN) {
      // a common error is missing brackets, so if all is unknown we compare authorship across brackets and return a possible match
      Equality across = Equality.UNKNOWN;
      if (Strings.isNullOrEmpty(n1.getAuthorship()) && Strings.isNullOrEmpty(n1.getYear())) {
        across = compare(n1.getBracketAuthorship(), n1.getBracketYear(), n2.getAuthorship(), n2.getYear());
      } else if (Strings.isNullOrEmpty(n1.getBracketAuthorship()) && Strings.isNullOrEmpty(n1.getBracketYear())) {
        across = compare(n1.getAuthorship(), n1.getYear(), n2.getBracketAuthorship(), n2.getBracketYear());
      }
      return across == Equality.EQUAL ? Equality.EQUAL : Equality.UNKNOWN;
    }
    return recomb.and(original);
  }

  /**
   * Compares two sets of author & year for equality.
   * This is more strict than the normal compare method and requires both authors and year to match.
   * Author matching is still done fuzzily
   *
   * @return true if both sets match
   */
  public boolean compareStrict(String author1, @Nullable String year1, String author2, @Nullable String year2) {
    // strictly compare authors first
    Equality result = compareAuthorteam(author1, author2, minCommonSubstring, Integer.MAX_VALUE);
    if (result != Equality.EQUAL) {
      return false;
    }
    // now also compare the year
    if (year1 == null && year2 == null) {
      return true;
    }
    return Equality.EQUAL == compareYear(year1, year2);
  }

  /**
   * @return ascii only, lower cased string without punctuation. Empty string instead of null.
   * Umlaut transliterations reduced to single letter
   */
  @VisibleForTesting
  protected static String normalize(String x) {
    if (StringUtils.isBlank(x)) {
      return null;
    }
    // remove in publications
    x = IN.matcher(x).replaceFirst("");

    // remove ex authors
    x = EX.matcher(x).replaceFirst("");

    // normalize filius
    x = FIL.matcher(x).replaceAll("$1 filius");

    // normalize and
    x = AND.matcher(x).replaceAll(", ");

    // remove ex authors
    x = TRANSLITERATIONS.matcher(x).replaceAll("$1");

    // fold to ascii
    x = org.gbif.utils.text.StringUtils.foldToAscii(x);

    // replace all punctuation but commas
    x = PUNCTUATION.matcher(x).replaceAll(" ");

    // normalize commas
    x = COMMA.matcher(x).replaceAll(",");

    x = StringUtils.normalizeSpace(x);

    if (StringUtils.isBlank(x)) {
      return null;
    }

    return x.toLowerCase();
  }

  /**
   * Looks up individual authors from an authorship string
   * @return entire authorship string with expanded authors if found
   */
  @VisibleForTesting
  protected String lookup(String normalizedAuthor) {
    if (normalizedAuthor!=null && authorMap.containsKey(normalizedAuthor)) {
      return authorMap.get(normalizedAuthor);
    } else {
      return normalizedAuthor;
    }
  }

  private List<String> lookup(List<String> authorTeam) {
    List<String> authors = Lists.newArrayList();
    for (String author : authorTeam) {
      authors.add(lookup(author));
    }
    return authors;
  }

  private List<String> splitAndLookup(String normalizedAuthorTeam, int minAuthorLengthWithoutLookup) {
    List<String> authors = Lists.newArrayList();
    if (normalizedAuthorTeam!=null){
      for (String author : AUTHOR_SPLITTER.split(normalizedAuthorTeam)) {
        if (minAuthorLengthWithoutLookup > 0 && author.length() < minAuthorLengthWithoutLookup) {
          authors.add(lookup(author));
        } else {
          authors.add(author);
        }
      }
    }
    return authors;
  }

  private ParsedName clone(ParsedName pn) {
    ParsedName pn2 = new ParsedName();
    try {
      BeanUtils.copyProperties(pn2, pn);
    } catch (IllegalAccessException e) {
      Throwables.propagate(e);
    } catch (InvocationTargetException e) {
      Throwables.propagate(e);
    }
    return pn2;
  }

  /**
   * Extract authorship from the name itself as best as we can to at least do some common string comparison
   */
  private void parseAuthorship(ParsedName pn) {
    // try to use full sciname minus the epithets
    String lastEpithet = ObjectUtils.coalesce(pn.getInfraSpecificEpithet(), pn.getSpecificEpithet(), pn.getGenusOrAbove());
    if (lastEpithet != null && pn.getScientificName() != null) {
      int idx = pn.getScientificName().lastIndexOf(lastEpithet);
      if (idx >= 0) {
        pn.setAuthorship(pn.getScientificName().substring(idx + lastEpithet.length()));
      }
    }
    // copy full name to year, will be extracted/normalized in year comparison
    pn.setYear(pn.getScientificName());
    pn.setAuthorsParsed(true);
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

  /**
   * Does an author comparison, normalizing the strings and try 3 comparisons:
   * 1) checks regular string equality
   * 2) checks for equality of the longest common substring
   * 3) do an author lookup and then check for common substring
   *
   * @param a1
   * @param a2
   * @param minCommonSubstring
   * @return
   */
  private Equality compareAuthorteam(@Nullable String a1, @Nullable String a2, int minCommonSubstring, int maxAuthorLengthWithoutLookup) {
    // convert to all lower case, no punctuation but commas seperating authors and normed whitespace
    List<String> authorTeam1 = splitAndLookup(normalize(a1), maxAuthorLengthWithoutLookup);
    List<String> authorTeam2 = splitAndLookup(normalize(a2), maxAuthorLengthWithoutLookup);
    if (!authorTeam1.isEmpty() && !authorTeam2.isEmpty()) {
      Equality equality = compareNormalizedAuthorteam(authorTeam1, authorTeam2, minCommonSubstring);
      if (equality != Equality.EQUAL) {
        List<String> authorTeam1l = lookup(authorTeam1);
        List<String> authorTeam2l = lookup(authorTeam2);
        if (!authorTeam1.equals(authorTeam1l) || !authorTeam2.equals(authorTeam2l)) {
          equality = compareNormalizedAuthorteam(authorTeam1l, authorTeam2l, minCommonSubstring);
        }
      }
      return equality;
    }
    return Equality.UNKNOWN;
  }

  private int lengthWithoutWhitespace(String x) {
    return StringUtils.deleteWhitespace(x).length();
  }

  /**
   * compares entire author team strings
   */
  private Equality compareNormalizedAuthorteam(final List<String> authorTeam1, final List<String> authorTeam2, final int minCommonStart) {
    // quick check avoiding subsequent heavier processing
    if (authorTeam1.equals(authorTeam2)) {
      // we can stop here, authors are equal, thats enough
      return Equality.EQUAL;

    } else {
      // compare all authors to each other - a single match is good enough!
      for (String author1 : authorTeam1) {
        for (String author2 : authorTeam2) {
          if (Equality.EQUAL == compareNormalizedAuthor(author1, author2, minCommonStart)) {
            return Equality.EQUAL;
          }
        }
      }
    }
    return Equality.DIFFERENT;
  }

  @VisibleForTesting
  protected String extractSurname(String name) {
    Matcher m = SURNAME.matcher(name);
    if (m.find()){
      return m.group(1);
    }
    return name;
  }

  /**
   * compares a single author potentially with initials
   */
  private Equality compareNormalizedAuthor(final String a1, final String a2, final int minCommonStart) {
    if (a1.equals(a2)) {
      // we can stop here, authors are equal, thats enough
      return Equality.EQUAL;

    } else {

      final String surname1 = extractSurname(a1);
      final String surname2 = extractSurname(a2);

      String common = StringUtils.getCommonPrefix(surname1, surname2);
      if (common != null && common.length() >= minCommonStart) {
        // do both names have a single initial which is different?
        // this is often the case when authors are relatives like brothers or son & father
        if (firstInitialsDiffer(a1, a2)) {
          return Equality.DIFFERENT;
        } else {
          return Equality.EQUAL;
        }

      } else if (a1.equals(common) && (surname2.startsWith(common))
          || a2.equals(common) && (surname1.startsWith(common))
          ) {
        // the smallest common substring is the same as one of the inputs
        // if it also matches the start of the first longer surname then we are ok as the entire string is the best match we can have
        // likey a short abbreviation
        return Equality.EQUAL;

      } else if (lengthWithoutWhitespace(StringUtils.getCommonPrefix(a1, a2)) > minCommonStart) {
        // the author string incl initials but without whitespace shares at least minCommonStart+1 characters
        return Equality.EQUAL;

      }
    }
    return Equality.DIFFERENT;
  }

  /**
   * Gracefully compare initials of the first author only
   * @return true if they differ
   */
  @VisibleForTesting
  protected boolean firstInitialsDiffer(String a1, String a2) {
    Matcher m1 = FIRST_INITIALS.matcher(a1);
    Matcher m2 = FIRST_INITIALS.matcher(a2);
    if (m1.find() && m2.find()) {
      String i1 = m1.group(0);
      String i2 = m2.group(0);
      if (i1.equals(i2)) {
        return false;

      } else {
        // if one set of chars is a subset of the other we consider this a match
        List<Character> smaller = Lists.charactersOf(StringUtils.deleteWhitespace(i1));
        List<Character> larger  = Lists.charactersOf(StringUtils.deleteWhitespace(i2));
        if (smaller.size() > larger.size()) {
          // swap, the Sets difference method needs the right inputs
          List<Character> tmp = smaller;
          smaller = larger;
          larger = tmp;
        }
        // remove all of the chars from the larger list and see if any remain
        if (CollectionUtils.isSubCollection(smaller, larger)) {
          // one is a subset of the other
          return false;
        }
      }
      // they seem to differ
      return true;

    } else {
      // no initials
      return false;
    }
  }
}
