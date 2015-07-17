package org.gbif.checklistbank.authorship;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.model.Equality;

import java.text.Normalizer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility to compare scientific name authorships, i.e. the (recombination) author and the publishing year.
 * Original name (bracket) authorship is not used in the comparison.
 *
 * Author strings are normalized to ASCII and then compared. As authors are often abbreviated in all kind of ways a shared common substring is accepted
 * as a positive equality.
 *
 * If any of the names given has an empty author & year the results will always be Equality.UNKNOWN.
 */
public class AuthorComparator {
    private static final Pattern AND = Pattern.compile("( et | and |&|&amp;)", Pattern.CASE_INSENSITIVE);
    private static final Pattern YEAR = Pattern.compile("(^|[^0-9])(\\d{4})([^0-9]|$)");

    private Map<String, String> KNOWN_AUTHORS;

    /**
     * @return ascii only, lower cased string without punctuation. Empty string instead of null
     */
    protected static String normalize(String x) {
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

    public Equality compare(String author1, String year1, String author2, String year2) {
        // compare recombination authors first
        Equality result = compareAuthor(author1, author2);
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
     * Extract authorship from the name itself as best as we can to at least do some common string comparison
     */
    private void parseAuthorship(ParsedName pn) {
        // try to use full sciname minus the epithets
        String lastEpithet = coalesce(pn.getInfraSpecificEpithet(), pn.getSpecificEpithet(), pn.getGenusOrAbove());
        int idx = pn.getScientificName().lastIndexOf(lastEpithet);
        if (idx >= 0) {
            pn.setAuthorship(pn.getScientificName().substring(idx + lastEpithet.length()));
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

    private Equality compareAuthor(String a1, String a2) {
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
