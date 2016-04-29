package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.model.Equality;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;
import org.gbif.nub.lookup.similarity.ModifiedDamerauLevenshtein;
import org.gbif.nub.lookup.similarity.StringSimilarity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubMatchingServiceImpl implements NameUsageMatchingService {

  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingServiceImpl.class);
  private static final int MIN_CONFIDENCE = 80;
  private static final int MIN_CONFIDENCE_FOR_HIGHER_MATCHES = 90;
  private static ConfidenceOrder CONFIDENCE_ORDER = new ConfidenceOrder();
  private final NubIndex nubIndex;
  private final HigherTaxaComparator htComp;
  private final NameParser parser;
  // name string to usageId
  private Map<String, NameUsageMatch> hackMap = Maps.newHashMap();
  private final StringSimilarity sim = new ModifiedDamerauLevenshtein(3);

  private static final List<Rank> HIGHER_QUERY_RANK = ImmutableList.of(Rank.FAMILY, Rank.ORDER, Rank.CLASS, Rank.PHYLUM,
    Rank.KINGDOM);
  public static final Map<TaxonomicStatus, Integer> STATUS_SCORE =
    ImmutableMap.of(TaxonomicStatus.ACCEPTED, 1, TaxonomicStatus.DOUBTFUL, -5, TaxonomicStatus.SYNONYM, 0);
  // match order by usageKey lowest to highest to preserve old ids
  private static final Ordering<NameUsageMatch> USAGE_KEY_ORDER = Ordering.natural().nullsLast().onResultOf(new Function<NameUsageMatch, Integer>() {
    @Nullable
    @Override
    public Integer apply(@Nullable NameUsageMatch m) {
      return m.getUsageKey();
    }
  });
  private static final Pattern FIRST_WORD = Pattern.compile("^(.+?)\\b");
  private static final List<Rank> HIGHER_RANKS;
  static {
    List<Rank> ranks = Lists.newArrayList(Rank.LINNEAN_RANKS);
    ranks.remove(Rank.SPECIES);
    HIGHER_RANKS = ImmutableList.copyOf(ranks);
  }

  private final AuthorComparator authComp;


  /**
   * @param nubIndex
   * @param htComp
   * @param parser
   */
  @Inject
  public NubMatchingServiceImpl(NubIndex nubIndex, HigherTaxaComparator htComp, NameParser parser) {
    this.nubIndex = nubIndex;
    this.htComp = htComp;
    this.parser = parser;
    authComp = AuthorComparator.createWithAuthormap();
    initHackMap();
  }

  private void initHackMap(){
    LOG.debug("Add entries to hackmap ...");
    try {
      hackMap.put("radiolaria", nubIndex.matchByUsageId(7));
    } catch (Exception e) {
      LOG.debug("Hackmap entry not existing, skip", e.getMessage());
    }
  }

  private static boolean isMatch(NameUsageMatch match) {
    return NameUsageMatch.MatchType.NONE != match.getMatchType();
  }

  private static NameUsageMatch higherMatch(NameUsageMatch match, NameUsageMatch firstMatch) {
    match.setMatchType(NameUsageMatch.MatchType.HIGHERRANK);
    match.setAlternatives(firstMatch.getAlternatives());
    return match;
  }

  // Wrapper method doing the time tracking and logging only.
  @Override
  public NameUsageMatch match(String scientificName, @Nullable Rank rank, @Nullable LinneanClassification classification, boolean strict, boolean verbose) {
    StopWatch watch = new StopWatch();
    watch.start();

    NameUsageMatch match = matchInternal(scientificName, rank, classification, strict, verbose);

    LOG.debug("{} Match of scientific name >{}< to {} [{}] in {}", match.getMatchType(), scientificName, match.getUsageKey(), match.getScientificName(), watch.toString());
    return match;
  }

  /**
   * Real method doing the work
   */
  private NameUsageMatch matchInternal(String scientificName, @Nullable Rank rank, @Nullable LinneanClassification classification, boolean strict, boolean verbose) {

    ParsedName pn = null;
    if (classification == null) {
      classification = new NameUsageMatch();
    } else {
      cleanClassification(classification);
    }
    try {
      // use name parser to make the name a canonical one
      // we build the name with flags manually as we wanna exclude indet. names such as "Abies spec." and rather match them to Abies only
      pn = parser.parse(scientificName, rank);
      interpretGenus(pn, classification.getGenus());
      scientificName = pn.buildName(false, false, false, false, false, false, true, true, false, false, false, false, false, false);

    } catch (UnparsableException e) {
      // hybrid names, virus names & blacklisted ones - dont provide any parsed name
      LOG.debug("Unparsable [{}] name [{}]", e.type, scientificName);
    }

    NameUsageMatch match1 = match(pn, scientificName, rank, classification, strict, MIN_CONFIDENCE, verbose);
    // for strict matching do not try higher ranks
    if (isMatch(match1) || strict) {
      return match1;
    }


    // try to MATCH TO HIGHER RANKS if we can
    // include species or genus only matches ?
    NameUsageMatch match;
    if (pn != null && pn.getGenusOrAbove() != null) {
      if (pn.getSpecificEpithet() != null) {
        if (pn.getInfraSpecificEpithet() != null) {
          // try with species
          String species = pn.canonicalSpeciesName();
          match = match(null, species, Rank.SPECIES, classification, false, MIN_CONFIDENCE_FOR_HIGHER_MATCHES, false);
          if (isMatch(match)) {
            return higherMatch(match, match1);
          }
        }

        // try with genus
        // we're not sure if this is really a genus, so dont set the rank
        // we get non species names sometimes like "Chaetognatha eyecount" that refer to a phylum called
        // "Chaetognatha"
        match = match(null, pn.getGenusOrAbove(), null, classification, true, MIN_CONFIDENCE_FOR_HIGHER_MATCHES, false);
        if (isMatch(match)) {
          return higherMatch(match, match1);
        }
      }

    } else if (!Strings.isNullOrEmpty(classification.getGenus())) {
      // no parsed name, try genus if given
      match = match(null, classification.getGenus(), Rank.GENUS, classification, true, MIN_CONFIDENCE_FOR_HIGHER_MATCHES, false);
      if (isMatch(match)) {
        return higherMatch(match, match1);
      }
    }

    // last resort - try higher ranks above genus
    for (Rank qr : HIGHER_QUERY_RANK) {
      String name = ClassificationUtils.getHigherRank(classification, qr);
      if (!StringUtils.isEmpty(name)) {
        match = match(null, name, qr, classification, true, MIN_CONFIDENCE_FOR_HIGHER_MATCHES, false);
        if (isMatch(match)) {
          return higherMatch(match, match1);
        }
      }
    }

    // if finally we cant find anything, return empty match object - but not null!
    LOG.debug("No match for name {}", scientificName);
    return noMatch(100, match1.getNote(), verbose ? match1.getAlternatives() : null);
  }

  private void cleanClassification(LinneanClassification cl) {
    for (Rank r : HIGHER_RANKS) {
      if (cl.getHigherRank(r) != null) {
        Matcher m = FIRST_WORD.matcher(cl.getHigherRank(r));
        if (m.find()) {
          ClassificationUtils.setHigherRank(cl, r, m.group(1));
        }
      }
    }
  }

  /**
   * Expands abbreviated genus names with the full genus if provided in the separate classification.
   * @param pn
   * @param genus
   */
  @VisibleForTesting
  protected static void interpretGenus(ParsedName pn, String genus) {
        // test if name has an abbreviated genus
    if (pn != null && !Strings.isNullOrEmpty(genus) && pn.getGenusOrAbove() != null && genus.length() > 1) {
      if (pn.getGenusOrAbove().length() == 2
        && pn.getGenusOrAbove().charAt(1) == '.'
        && pn.getGenusOrAbove().charAt(0) == genus.charAt(0)
        || pn.getGenusOrAbove().length() == 1 && pn.getGenusOrAbove().charAt(0) == genus.charAt(0)) {
        pn.setGenusOrAbove(genus);
      }
    }
  }

  private NameUsageMatch bestMatch(ParsedName pn, String canonicalName, Rank rank, LinneanClassification lc) {

  }
  private NameUsageMatch bestMatchStrict(ParsedName pn, String canonicalName, Rank rank, LinneanClassification lc) {

  }
  /**
   * Use our custom similarity algorithm and compare the higher classifications to select the best match
   * @return the best match, might contain no usageKey
   */
  @VisibleForTesting
  protected NameUsageMatch match(ParsedName pn, String canonicalName, Rank rank, LinneanClassification lc, boolean strict, int minConfidence, boolean verbose){
    if (Strings.isNullOrEmpty(canonicalName)) {
      return noMatch(100, "No name given", null);
    }

    // first try our manual hackmap
    if (hackMap.containsKey(canonicalName.toLowerCase())) {
      return hackMap.get(canonicalName.toLowerCase());
    }

    // do the matching
    NameUsageMatch best = strict ? bestMatchStrict(pn, canonicalName, rank, lc) : bestMatch(pn, canonicalName, rank, lc);



    // do a lucene matching
    List<NameUsageMatch> matches = nubIndex.matchByName(canonicalName, !strict, 50);
    for (NameUsageMatch m : matches) {
      // 0 - +100
      final int nameSimilarity = nameSimilarity(canonicalName, m);
      // -20 - +40
      final int authorSimilarity = authorSimilarity(pn, m, strict);
      // -50 - +50
      final int classificationSimilarity = classificationSimilarity(lc, m, strict);
      // -10 - +5
      final int rankSimilarity = rankSimilarity(rank, m.getRank());
      // -5 - +1
      final int statusScore = STATUS_SCORE.get(m.getStatus());
      // preliminary total score, -5 - 20 distance to next best match coming below!
      m.setConfidence(nameSimilarity + authorSimilarity + classificationSimilarity + rankSimilarity + statusScore);

      if (verbose) {
        addNote(m, "Similarity: name="+nameSimilarity);
        addNote(m, "authorship="+authorSimilarity);
        addNote(m, "classification="+classificationSimilarity);
        addNote(m, "rank="+rankSimilarity);
        addNote(m, "status="+statusScore);
      }
    }

    // order by confidence
    Collections.sort(matches, CONFIDENCE_ORDER);
    if (matches.size() > 0) {
      // add -5 - 20 confidence based on distance to next best match
      NameUsageMatch best = matches.get(0);
      int bestConfidence = best.getConfidence();
      int nextMatchDistance;

      if (matches.size() == 1) {
        // boost results with a single match to pick from
        nextMatchDistance = 10;
        if (verbose) {
          addNote(best, "singleMatch=" + nextMatchDistance);
        }

      } else {
        // we have more than one match to chose from
        int secondBestConfidence = matches.get(1).getConfidence();
        if (bestConfidence == secondBestConfidence) {
          // equally good matches, bummer!
          // this sometimes happens when there are "homonyms" in the nub as synonyms only
          List<NameUsageMatch> equalMatches = extractEqualMatches(matches);
          boolean sameClassification = true;
          for (NameUsageMatch m : equalMatches) {
            if (!equalClassification(best, m)) {
              sameClassification = false;
              break;
            }
          }
          if (sameClassification) {
            // if they both have the same classification pick the one with the lowest, hence oldest id!
            best = equalMatches.get(0);
            addNote(best, equalMatches.size() + " synonym homonyms");
          } else {
            return noMatch(99, "Multiple equal matches for " + canonicalName, verbose ? matches : null);
          }
        }
        // boost up to 10 based on distance to next match, penalty for very close matches
        nextMatchDistance = Math.min(10, (bestConfidence - secondBestConfidence - 10) / 2);
        if (verbose) {
          addNote(best, "nextMatch=" + nextMatchDistance);
        }
      }
      best.setConfidence(bestConfidence + nextMatchDistance);

      // normalize confidence values into the range of 0 to 100
      best.setConfidence(normConfidence(best.getConfidence()));

      // finally check if match is good enough
      if (best.getConfidence() < minConfidence) {
        return noMatch(99, "No match because of too little confidence", verbose ? matches : null);
      }
      if (verbose && matches.size() > 1) {
        // the first match is best
        matches.remove(0);
        best.setAlternatives(matches);
        for (NameUsageMatch alt : matches) {
          alt.setConfidence(normConfidence(alt.getConfidence()));
        }
      }

      return best;
    }

    return noMatch(100, null, null);
  }

  private int authorSimilarity(@Nullable ParsedName pn, NameUsageMatch m, boolean strict) {
    int similarity = 0;
    if (pn != null) {
      try {
        ParsedName mpn = parser.parse(m.getScientificName(), m.getRank());
        // authorship comparison was requested!
        Equality recomb = authComp.compare(pn.getAuthorship(), pn.getYear(), mpn.getAuthorship(), mpn.getYear());
        Equality bracket = authComp.compare(pn.getBracketAuthorship(), pn.getBracketYear(), mpn.getBracketAuthorship(), mpn.getBracketYear());

        similarity = equality2Similarity(recomb, 3);
        similarity = similarity + equality2Similarity(bracket, 1);

      } catch (UnparsableException e) {
        if (e.type.isParsable()) {
          LOG.warn("Failed to parse name: {}", m.getScientificName());
        }
      }
    }

    return similarity;
  }

  private int equality2Similarity(Equality eq, int factor) {
    switch (eq) {
      case EQUAL: return 10*factor;
      case DIFFERENT: return -7*factor;
    }
    return 0;
  }

  private boolean equalClassificationKeys(LinneanClassificationKeys best, LinneanClassificationKeys m) {
    for (Rank r : Rank.LINNEAN_RANKS) {
      if (best.getHigherRankKey(r) == null) {
        if (m.getHigherRankKey(r) != null){
          return false;
        }

      } else {
        if (m.getHigherRankKey(r) == null || !best.getHigherRankKey(r).equals(m.getHigherRankKey(r))){
          return false;
        }
      }
    }
    return true;
  }

  private boolean equalClassification(LinneanClassification best, LinneanClassification m) {
    for (Rank r : Rank.LINNEAN_RANKS) {
      if (best.getHigherRank(r) == null) {
        if (m.getHigherRank(r) != null){
          return false;
        }

      } else {
        if (m.getHigherRank(r) == null || !best.getHigherRank(r).equals(m.getHigherRank(r))){
          return false;
        }
      }
    }
    return true;
  }

  private List<NameUsageMatch> extractEqualMatches(List<NameUsageMatch> matches) {
    List<NameUsageMatch> equal = Lists.newArrayList();
    if (!matches.isEmpty()) {
      int conf = matches.get(0).getConfidence();
      for (NameUsageMatch m : matches){
        if (m.getConfidence().equals(conf)) {
          equal.add(m);
        } else {
          // matches are sorted by confidence!
          break;
        }
      }
    }
    // finally sort by usageKey lowest to highest to preserve old ids
    return USAGE_KEY_ORDER.sortedCopy(matches);
  }

  private static void addNote(NameUsageMatch m, String note) {
    if (m.getNote() == null) {
      m.setNote(note);
    } else {
      m.setNote(m.getNote() + "; " + note);
    }
  }
  private static NameUsageMatch noMatch(int confidence, String note, List<NameUsageMatch> alternatives) {
    NameUsageMatch no = new NameUsageMatch();
    no.setMatchType(NameUsageMatch.MatchType.NONE);
    no.setConfidence(confidence);
    no.setNote(note);
    no.setAlternatives(alternatives);
    return no;
  }

  private int nameSimilarity(String canonicalName, NameUsageMatch m) {
    // calculate name distance
    int confidence;
    if (canonicalName.equalsIgnoreCase(m.getCanonicalName())) {
      // straight match
      confidence = 100;
      // binomial straight match? That is pretty trustworthy
      if (canonicalName.contains(" ")) {
        confidence += 20;
      }

    } else {
      // fuzzy - be careful!
      confidence = (int) sim.getSimilarity(canonicalName, m.getCanonicalName()) - 5;
      // modify confidence according to genus comparison in bionomials.
      // slightly trust binomials with a matching genus more, and truss less if we matched a different genus name
      int spaceIdx = m.getCanonicalName().indexOf(" ");
      if (spaceIdx > 0) {
        String genus = m.getCanonicalName().substring(0, spaceIdx);
        if (canonicalName.startsWith(genus)) {
          confidence += 5;
        } else {
          confidence -= 10;
        }
      }
    }
    return confidence;
  }

  @VisibleForTesting
  protected int classificationSimilarity(LinneanClassification query, LinneanClassification reference, boolean strict) {
    // kingdom is super important
    int rate = htComp.compareHigherRank(Rank.KINGDOM, query, reference, 5, -10, -1);
    // plant and animal kingdoms are better delimited than Chromista, Fungi, etc. , so punish those mismatches higher
    if (rate == -10 && htComp.isInKingdoms(query, Kingdom.ANIMALIA, Kingdom.PLANTAE)
        && htComp.isInKingdoms(reference, Kingdom.ANIMALIA, Kingdom.PLANTAE)){
      //TODO: decrease this to 30 once the backbone is in a better state again !!!
      rate = -51;
    }
    // phylum to family
    rate += htComp.compareHigherRank(Rank.PHYLUM, query, reference, 10, -10, -1);
    rate += htComp.compareHigherRank(Rank.CLASS, query, reference, 15, -10, 0);
    rate += htComp.compareHigherRank(Rank.ORDER, query, reference, 15, -10, 0);
    rate += htComp.compareHigherRank(Rank.FAMILY, query, reference, 25, -15, 0);

    return minMax(-60, 50, rate);
  }


  // rate ranks from -10 to +5, zero if nothing is know
  private int rankSimilarity(Rank query, Rank ref) {
    int similarity = 0;
    if (ref != null) {
        // rate ranks lower that are not represented in the canonical, e.g. cultivars
      if (Rank.CULTIVAR == ref || Rank.CULTIVAR_GROUP == ref){
        similarity -= 7;
      }else if (Rank.STRAIN == ref){
        similarity -= 7;
      }else if (Rank.INFORMAL == ref){
        similarity -= 5;
      }
      if (ref.isUncomparable()){
        // this also includes informal again
        similarity -= 3;
      }

      if(query != null){
        // both ranks exist. Compare directly
        if (query.equals(ref)) {
          similarity += 10;
        } else if (Rank.UNCOMPARABLE_RANKS.contains(query)) {
          // uncomparable query ranks
          similarity -= 5;
        } else {
          // rate lower the further away the ranks are
          similarity -= 1 * Math.abs(ref.ordinal() - query.ordinal());
        }
      }

    } else if (query != null) {
      // reference has no rank, rate it lower
      similarity -= 1;
    }
    return minMax(-10, 5, similarity);
  }

  /**
   * Produces a value between 0 and 100 by taking the not properly normalized confidence in the expected range of
   * 0 to 175. This function is optimized to deal with acceptable matches being above 80, good matches above 90 and
   * very good matches incl and above 100. The maximum of 100 is reached for an input of 175 or above.
   */
  @VisibleForTesting
  protected static int normConfidence(int s) {
    return minMax(0, 100, s <= 80 ? s : (int) Math.round(75.8 + (26d * (Math.log10((s - 70d) * 1.5) - 1))));
  }

  private static int minMax(int min, int max, int value) {
    return Math.max(min, Math.min(max, value));
  }


  /**
   * Ordering based on match confidence and scientific name secondly.
   */
  public static class ConfidenceOrder implements Comparator<NameUsageMatch> {

    @Override
    public int compare(NameUsageMatch o1, NameUsageMatch o2) {
      return ComparisonChain.start()
        .compare(o1.getConfidence(), o2.getConfidence(), Ordering.natural().reverse().nullsLast())
        .compare(o1.getScientificName(), o2.getScientificName(), Ordering.natural().nullsLast())
        .result();
    }
  }

}
