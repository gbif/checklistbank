package org.gbif.checklistbank.utils;

import java.util.regex.Pattern;

import com.google.common.base.Strings;


/**
 * A scientific name normalizer that replaces common misspellings and epither gender changes.
 */
public class SciNameNormalizer {

    private static final Pattern suffix_a = Pattern.compile("(on|um|us|a)$");
    private static final Pattern suffix_i = Pattern.compile("(ei|ae|iae|ii)$");
    private static final Pattern suffix_oi = Pattern.compile("ioi$");
    private static final Pattern suffix_if = Pattern.compile("aef");
    private static final Pattern suffix_iana = Pattern.compile("(?<=[^i])ana$");
    private static final Pattern suffix_ensis = Pattern.compile("ens(e|is)$");
    private static final Pattern i = Pattern.compile("[jyi]+");
    private static final Pattern white = Pattern.compile("\\s{2,}");
    private static final Pattern empty = Pattern.compile("['_-]");

    public static String normalize(String s) {
        if (Strings.isNullOrEmpty(s)) return null;

        //s = suffix_iana.matcher(s).replaceFirst("iana");
        //s = suffix_if.matcher(s).replaceFirst("if");
        //s = suffix_oi.matcher(s).replaceFirst("oi");
        //s = suffix_i.matcher(s).replaceFirst("i");
        s = suffix_a.matcher(s).replaceFirst("a");
        s = i.matcher(s).replaceAll("i");
        s = empty.matcher(s).replaceAll("");
        s = white.matcher(s).replaceAll(" ");

        return s.trim();
    }

  /**
   * Does a simple gender neutral stemming of a latin epithet.
   */
  public static String stemEpithet(String epithet) {
    if (Strings.isNullOrEmpty(epithet)) return null;
    return suffix_a.matcher(epithet).replaceFirst("");
  }
}