package org.gbif.checklistbank.authorship;

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
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.model.Equality;
import org.gbif.utils.ObjectUtils;
import org.gbif.utils.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility to compare scientific name authorships, i.e. the recombination and basionym author and publishing year.
 * Author strings are normalized to ASCII and then compared. As authors are often abbreviated in all kind of ways a shared common substring is accepted
 * as a positive equality.
 * If any of the names given has an empty author & year the results will always be Equality.UNKNOWN.
 * <p>
 * The class exposes two kind of compare methods. A strict one always requiring both year and author to match
 * and a more lax default comparison that only looks at years when the authors differ (as it is quite hard to compare authors)
 */
public class AuthorComparator {
  private static final Logger LOG = LoggerFactory.getLogger(AuthorComparator.class);

  private static final Pattern AND = Pattern.compile("( et | and |&|&amp;)", Pattern.CASE_INSENSITIVE);
  private static final Pattern IN = Pattern.compile(" in .+$", Pattern.CASE_INSENSITIVE);
  private static final Pattern EX = Pattern.compile("^.+ ex ", Pattern.CASE_INSENSITIVE);
  private static final Pattern FIL = Pattern.compile("([A-Z][a-z]*)[\\. ]\\s*f(:?il)?\\.?\\b");
  private static final Pattern SANCT = Pattern.compile(" *: *[A-Z][a-z]+\\.?$");
  private static final Pattern TRANSLITERATIONS = Pattern.compile("([auo])e", Pattern.CASE_INSENSITIVE);
  private static final Pattern AUTHOR = Pattern.compile("^((?:[a-z]\\s)*).*?([a-z]+)( filius)?$");
  private static final String AUTHOR_MAP_FILENAME = "/authorship/authormap.txt";
  private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}&&[^,]]+");
  private static final Pattern COMMA = Pattern.compile("\\s*,\\s*");
  private static final Splitter AUTHOR_SPLITTER = Splitter.on(",").omitEmptyStrings();
  private final Map<String, String> authorMap;
  private static final int MIN_AUTHOR_LENGTH_WITHOUT_LOOKUP = 4;

  private final int minCommonSubstring;

  private AuthorComparator(Map<String, String> authors) {
    Map<String, String> map = Maps.newHashMap();
    minCommonSubstring = 4;
    int counter = 0;
    for (Map.Entry<String, String> entry : authors.entrySet()) {
      String key = normalize(entry.getKey());
      String val = normalize(entry.getValue());
      if (key != null && val != null) {
        map.put(key, val);
        counter++;
      }
    }
    authorMap = ImmutableMap.copyOf(map);
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
      Equality yresult = new YearComparator(year1, year2).compare();
      if (yresult != Equality.UNKNOWN) {
        if (yresult == Equality.DIFFERENT || authors1 == null || authors2 == null) {
          result = yresult;
        } else {
          // year EQUAL, i.e. very close by
          // also make sure we have at least one capital char overlap between the 2 authorships
          Set<Character> upper1 = authors1.chars()
                  .filter(Character::isUpperCase)
                  .mapToObj(c -> (char) c)
                  .collect(Collectors.toSet());
          Set<Character> upper2 = authors2.chars()
                  .filter(Character::isUpperCase)
                  .mapToObj(c -> (char) c)
                  .collect(Collectors.toSet());
          upper1.retainAll(upper2);
          if (!upper1.isEmpty()) {
            result = yresult;
          }
        }
      }
    }
    return result;
  }

  /**
   * Does a comparison of recombination and basionym authorship using the author compare method once for the recombination authorship and once for the basionym.
   */
  public Equality compare(ParsedName n1, ParsedName n2) {
    Equality recomb = compare(n1.getAuthorship(), n1.getYear(), n2.getAuthorship(), n2.getYear());
    if (recomb != Equality.UNKNOWN) {
      // in case the recomb author differs or is the same we are done, no need for basionym authorship comparison
      return recomb;
    }
    Equality original = compare(n1.getBracketAuthorship(), n1.getBracketYear(), n2.getBracketAuthorship(), n2.getBracketYear());
    if (original == Equality.UNKNOWN) {
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
    return Equality.EQUAL == new YearComparator(year1, year2).compare();
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
    // fold to ascii
    x = org.gbif.utils.text.StringUtils.foldToAscii(x);

    // remove in publications
    x = IN.matcher(x).replaceFirst("");

    // remove sanctioning authors
    x = SANCT.matcher(x).replaceFirst("");

    // remove ex authors
    x = EX.matcher(x).replaceFirst("");

    // normalize filius
    x = FIL.matcher(x).replaceAll("$1 filius");

    // normalize and
    x = AND.matcher(x).replaceAll(", ");

    // remove transliterations of öüä to oua followed by e
    x = TRANSLITERATIONS.matcher(x).replaceAll("$1");

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
   *
   * @return entire authorship string with expanded authors if found
   */
  @VisibleForTesting
  protected String lookup(String normalizedAuthor) {
    if (normalizedAuthor != null && authorMap.containsKey(normalizedAuthor)) {
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
    if (normalizedAuthorTeam != null) {
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
   * Does an author comparison, normalizing the strings and try 3 comparisons:
   * 1) checks regular string equality
   * 2) checks for equality of the longest common substring
   * 3) do an author lookup and then check for common substring
   */
  private Equality compareAuthorteam(@Nullable String a1, @Nullable String a2, int minCommonSubstring, int maxAuthorLengthWithoutLookup) {
    // convert to all lower case, no punctuation but commas seperating authors and normed whitespace
    List<String> authorTeam1 = splitAndLookup(normalize(a1), maxAuthorLengthWithoutLookup);
    List<String> authorTeam2 = splitAndLookup(normalize(a2), maxAuthorLengthWithoutLookup);
    if (!authorTeam1.isEmpty() && !authorTeam2.isEmpty()) {
      Equality equality = compareNormalizedAuthorteam(authorTeam1, authorTeam2, minCommonSubstring);
      if (equality != Equality.EQUAL) {
        // try again by looking up entire string
        List<String> authorTeam1l = lookup(authorTeam1);
        List<String> authorTeam2l = lookup(authorTeam2);
        // only compare again if the list is actually different then before
        if (!authorTeam1.equals(authorTeam1l) || !authorTeam2.equals(authorTeam2l)) {
          equality = compareNormalizedAuthorteam(authorTeam1l, authorTeam2l, minCommonSubstring);
        }
      }
      return equality;
    }
    return Equality.UNKNOWN;
  }

  private static int lengthWithoutWhitespace(String x) {
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
        Author a1 = new Author(author1);
        for (String author2 : authorTeam2) {
          Author a2 = new Author(author2);
          if (Equality.EQUAL == a1.compare(a2, minCommonStart)) {
            return Equality.EQUAL;
          }
        }
      }
    }
    return Equality.DIFFERENT;
  }

  static class Author {
    final String fullname;
    final String initials;
    final String surname;
    final String suffix;

    Author(String a) {
      fullname = a;
      Matcher m = AUTHOR.matcher(a);
      if (m.find()) {
        initials = trim(m.group(1));
        surname = trim(m.group(2));
        suffix = trim(m.group(3));
      } else {
        LOG.warn("Cannot parse author: {}", a);
        initials = "";
        surname = trim(a);
        suffix = "";
      }
    }

    private String trim(String x) {
      return x == null ? null : StringUtils.trimToNull(x);
    }

    boolean hasInitials() {
      return initials != null && !initials.isEmpty();
    }

    /**
     * Gracefully compare initials of the first author only
     *
     * @return true if they differ
     */
    boolean firstInitialsDiffer(Author other) {
      if (hasInitials() && other.hasInitials()) {
        if (initials.equals(other.initials)) {
          return false;

        } else {
          // if one set of chars is a subset of the other we consider this a match
          List<Character> smaller = Lists.charactersOf(StringUtils.deleteWhitespace(initials));
          List<Character> larger = Lists.charactersOf(StringUtils.deleteWhitespace(other.initials));
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
        // no initials in at least one of them
        return false;
      }
    }

    /**
     * compares a single author potentially with initials
     */
    private Equality compare(final Author other, final int minCommonStart) {
      if (fullname.equals(other.fullname)) {
        // we can stop here, authors are equal, thats enough
        return Equality.EQUAL;

      } else {

        String common = StringUtils.getCommonPrefix(surname, other.surname);
        if (surname.equals(other.surname) || common.length() >= minCommonStart) {
          // do both names have a single initial which is different?
          // this is often the case when authors are relatives like brothers or son & father
          if (firstInitialsDiffer(other)) {
            return Equality.DIFFERENT;
          } else {
            return Equality.EQUAL;
          }

        } else if (!firstInitialsDiffer(other) && (surname.equals(common) && (other.surname.startsWith(common))
            || other.surname.equals(common) && (surname.startsWith(common)))
            ) {
          // short common surname, matching in full to one of them
          // and in addition existing and not conflicting initials
          return Equality.EQUAL;

        } else if (fullname.equals(common) && (other.surname.startsWith(common))
            || other.fullname.equals(common) && (surname.startsWith(common))
            ) {
          // the smallest common substring is the same as one of the inputs
          // if it also matches the start of the first longer surname then we are ok as the entire string is the best match we can have
          // likey a short abbreviation
          return Equality.EQUAL;

        } else if (lengthWithoutWhitespace(StringUtils.getCommonPrefix(fullname, other.fullname)) > minCommonStart) {
          // the author string incl initials but without whitespace shares at least minCommonStart+1 characters
          return Equality.EQUAL;

        }
      }
      return Equality.DIFFERENT;
    }

  }

}
