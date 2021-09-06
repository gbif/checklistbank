package org.gbif.nub.lookup.fuzzy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.v2.NameUsageMatch2;
import org.gbif.api.v2.RankedName;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.utils.RankUtils;
import org.gbif.nub.lookup.NameUsageMatchingService2;
import org.gbif.nub.lookup.similarity.ScientificNameSimilarity;
import org.gbif.nub.lookup.similarity.StringSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NubMatchingServiceImpl implements NameUsageMatchingService, NameUsageMatchingService2 {

  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingServiceImpl.class);
  private static final int MIN_CONFIDENCE = 80;
  private static final int MIN_CONFIDENCE_FOR_HIGHER_MATCHES = 90;
  private static final Set<Kingdom> VAGUE_KINGDOMS = ImmutableSet.of(Kingdom.ARCHAEA, Kingdom.BACTERIA, Kingdom.FUNGI, Kingdom.CHROMISTA, Kingdom.PROTOZOA, Kingdom.INCERTAE_SEDIS);
  private static ConfidenceOrder CONFIDENCE_ORDER = new ConfidenceOrder();
  private final NubIndex nubIndex;
  private final HigherTaxaComparator htComp;
  private final NameParser parser;
  // name string to usageId
  private Map<String, NameUsageMatch> hackMap = Maps.newHashMap();
  private final StringSimilarity sim = new ScientificNameSimilarity();

  private static final Set<NameType> STRICT_MATCH_TYPES = ImmutableSet.of(NameType.OTU, NameType.VIRUS, NameType.HYBRID);
  private static final List<Rank> PARSED_QUERY_RANK = ImmutableList.of(Rank.SPECIES, Rank.GENUS);
  private static final List<Rank> HIGHER_QUERY_RANK = ImmutableList.of(Rank.FAMILY, Rank.ORDER, Rank.CLASS, Rank.PHYLUM, Rank.KINGDOM);
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

  protected enum MatchingMode {FUZZY, STRICT, HIGHER}

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

  private void initHackMap() {
    //TODO: load the map from configs
    if (nubIndex.getDatasetKey() != null && nubIndex.getDatasetKey().equals(Constants.NUB_DATASET_KEY)) {
      LOG.debug("Add entries to hackmap for GBIF Backbone ...");
      try {
        hackMap.put("radiolaria", nubIndex.matchByUsageId(7));
        hackMap.put("hepatic", nubIndex.matchByUsageId(9));
      } catch (Exception e) {
        LOG.debug("Hackmap entry not existing, skip", e.getMessage());
      }
    }
  }

  private static boolean isMatch(NameUsageMatch match) {
    return NameUsageMatch.MatchType.NONE != match.getMatchType();
  }

  private static NameUsageMatch higherMatch(NameUsageMatch match, NameUsageMatch firstMatch) {
    match.setMatchType(NameUsageMatch.MatchType.HIGHERRANK);
    setAlternatives(match, firstMatch.getAlternatives());
    return match;
  }

  public NameUsageMatch2 v2(NameUsageMatch m) {
    NameUsageMatch2 m2 = new NameUsageMatch2();
    if (m.getUsageKey() != null) {
      // main usage
      RankedName u = new RankedName();
      m2.setUsage(match2rankedName(m));
      // accepted
      if (m.getAcceptedUsageKey() != null) {
        m2.setSynonym(true);
        NameUsageMatch accM = nubIndex.matchByUsageId(m.getAcceptedUsageKey());
        m2.setAcceptedUsage(match2rankedName(accM));
      }
      // classification
      for (Rank r : Rank.LINNEAN_RANKS) {
        Integer key = ClassificationUtils.getHigherRankKey(m, r);
        if (key != null) {
          RankedName ht = new RankedName();
          ht.setRank(r);
          ht.setKey(key);
          ht.setName(ClassificationUtils.getHigherRank(m, r));
          m2.getClassification().add(ht);
        }
      }
    }
    // diagnostics in all cases
    m2.getDiagnostics().setMatchType(m.getMatchType());
    m2.getDiagnostics().setStatus(m.getStatus());
    m2.getDiagnostics().setNote(m.getNote());
    if (m.getAlternatives() != null) {
      for (NameUsageMatch alt : m.getAlternatives()) {
        m2.getDiagnostics().getAlternatives().add(v2(alt));
      }
    }
    return m2;
  }

  private static RankedName match2rankedName(NameUsageMatch m) {
    RankedName rn = null;
    if (m.getUsageKey() != null) {
      rn = new RankedName();
      rn.setKey(m.getUsageKey());
      rn.setName(m.getScientificName());
      rn.setRank(m.getRank());
    }
    return rn;
  }

  /**
   * Sets the alternative on a match making sure we dont get infinite recursions my clearing all alternate matches on the arguments
   */
  private static void setAlternatives(NameUsageMatch match, List<NameUsageMatch> alts) {
    if (alts != null) {
      ListIterator<NameUsageMatch> iter = alts.listIterator();
      while (iter.hasNext()) {
        NameUsageMatch m = iter.next();
        if (m.getUsageKey().equals(match.getUsageKey())) {
          // same usage, remove!
          iter.remove();
        } else if (m.getAlternatives() != null && !m.getAlternatives().isEmpty()) {
          m.setAlternatives(Lists.<NameUsageMatch>newArrayList());
        }
      }
    }
    match.setAlternatives(alts);
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
    NameType queryNameType;
    MatchingMode mainMatchingMode = strict ? MatchingMode.STRICT : MatchingMode.FUZZY;

    if (classification == null) {
      classification = new NameUsageMatch();
    } else {
      cleanClassification(classification);
    }
    try {
      // use name parser to make the name a canonical one
      // we build the name with flags manually as we wanna exclude indet. names such as "Abies spec." and rather match them to Abies only
      pn = parser.parse(scientificName, rank);
      queryNameType = pn.getType();
      interpretGenus(pn, classification.getGenus());
      scientificName = pn.buildName(false, false, false, false, false, false, true, true, false, false, false, false, false, false);
      // parsed genus provided for a name lower than genus?
      if (classification.getGenus() == null && pn.getGenusOrAbove() != null && pn.getRank() != null && pn.getRank().isInfrageneric() ) {
        classification.setGenus(pn.getGenusOrAbove());
      }
      // used parsed rank if not given explicitly
      if (rank == null) {
        rank = pn.getRank();
      }
      // hybrid names, virus names, OTU & blacklisted ones don't provide any parsed name
      if (mainMatchingMode != MatchingMode.STRICT && !pn.getType().isParsable()) {
        // turn off fuzzy matching
        mainMatchingMode = MatchingMode.STRICT;
        LOG.debug("Unparsable {} name, turn off fuzzy matching for {}", pn.getType(), scientificName);
      }

    } catch (UnparsableException e) {
      // hybrid names, virus names & blacklisted ones - dont provide any parsed name
      queryNameType = e.type;
      // we assign all OTUs unranked
      if (NameType.OTU == e.type) {
        rank = Rank.UNRANKED;
      }
      if (mainMatchingMode != MatchingMode.STRICT) {
        // turn off fuzzy matching
        mainMatchingMode = MatchingMode.STRICT;
        LOG.debug("Unparsable {} name, turn off fuzzy matching for {}", e.type, scientificName);
      } else {
        LOG.debug("Unparsable {} name: {}", e.type, scientificName);
      }
    }

    NameUsageMatch match1 = match(queryNameType, pn, scientificName, rank, classification, mainMatchingMode, verbose);
    // use genus higher match instead of fuzzy one?
    // https://github.com/gbif/portal-feedback/issues/2930
    if (match1.getMatchType() == NameUsageMatch.MatchType.FUZZY &&
        match1.getRank() != null && match1.getRank().isSpeciesOrBelow() &&
        pn != null && !Objects.equals(pn.getGenusOrAbove(), match1.getGenus()) &&
        nextAboveGenusDiffers(classification, match1)
    ) {
      NameUsageMatch genusMatch = match(pn.getType(), null, pn.getGenusOrAbove(), Rank.GENUS, classification, MatchingMode.HIGHER, verbose);
      if (isMatch(genusMatch) && genusMatch.getRank() == Rank.GENUS) {
        return higherMatch(genusMatch, match1);
      }
    }
    // for strict matching do not try higher ranks
    if (isMatch(match1) || strict) {
      return match1;
    }


    // try to MATCH TO HIGHER RANKS if we can
    // include species or genus only matches from parsed name?
    NameUsageMatch match;
    if (pn != null && pn.getGenusOrAbove() != null) {
      if (pn.getSpecificEpithet() != null || (rank != null && rank.isInfrageneric())) {
        if (pn.getInfraSpecificEpithet() != null || (rank != null && rank.isInfraspecific())) {
          // try with species
          String species = pn.canonicalSpeciesName();
          match = match(pn.getType(), null, species, Rank.SPECIES, classification, MatchingMode.FUZZY, verbose);
          if (isMatch(match)) {
            return higherMatch(match, match1);
          }
        }

        // try with genus
        // we're not sure if this is really a genus, so don't set the rank
        // we get non species names sometimes like "Chaetognatha eyecount" that refer to a phylum called
        // "Chaetognatha"
        match = match(pn.getType(), null, pn.getGenusOrAbove(), null, classification, MatchingMode.HIGHER, verbose);
        if (isMatch(match)) {
          return higherMatch(match, match1);
        }
      }

    } else {
      // use classification strings instead
      for (Rank qr : PARSED_QUERY_RANK) {
        String name = ClassificationUtils.getHigherRank(classification, qr);
        if (!StringUtils.isEmpty(name)) {
          match = match(null, null, name, qr, classification, MatchingMode.HIGHER, verbose);
          if (isMatch(match)) {
            return higherMatch(match, match1);
          }
        }
      }
    }

    // last resort - try higher ranks above genus
    for (Rank qr : HIGHER_QUERY_RANK) {
      String name = ClassificationUtils.getHigherRank(classification, qr);
      if (!StringUtils.isEmpty(name)) {
        match = match(null, null, name, qr, classification, MatchingMode.HIGHER, verbose);
        if (isMatch(match)) {
          return higherMatch(match, match1);
        }
      }
    }

    // if finally we cant find anything, return empty match object - but not null!
    LOG.debug("No match for name {}", scientificName);
    return noMatch(100, match1.getNote(), verbose ? match1.getAlternatives() : null);
  }

  private boolean nextAboveGenusDiffers(LinneanClassification cl, LinneanClassification cl2) {
    for (Rank r = RankUtils.nextHigherLinneanRank(Rank.GENUS); r != null; r = RankUtils.nextHigherLinneanRank(r)) {
      String h1 = cl.getHigherRank(r);
      String h2 = cl2.getHigherRank(r);
      if (h1 != null && h2 != null) {
        return !Objects.equals(h1, h2);
      }
    }
    return false;
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
   *
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

  private List<NameUsageMatch> queryIndex(Rank rank, String canonicalName, boolean fuzzy) {
    List<NameUsageMatch> matches = nubIndex.matchByName(canonicalName, fuzzy, 50);
    // flag aggregate matches, see https://github.com/gbif/portal-feedback/issues/2935
    for (NameUsageMatch m : matches) {
      if (m.getMatchType() == NameUsageMatch.MatchType.EXACT
              && rank == Rank.SPECIES_AGGREGATE
              && m.getRank() != Rank.SPECIES_AGGREGATE) {
        LOG.warn("Species aggregate match found for {} {}, but use type EXACT until supported in API", m.getRank(), m.getScientificName());
        //TODO: change to MatchType.AGGREGATE once available
        m.setMatchType(NameUsageMatch.MatchType.EXACT);
      }
    }
    return matches;
  }

  private List<NameUsageMatch> queryFuzzy(@Nullable NameType queryNameType, ParsedName pn, String canonicalName, Rank rank, LinneanClassification lc, boolean verbose) {
    // do a lucene matching
    List<NameUsageMatch> matches = queryIndex(rank, canonicalName, true);
    for (NameUsageMatch m : matches) {
      // 0 - +120
      final int nameSimilarity = nameSimilarity(queryNameType, canonicalName, m);
      // -36 - +40
      final int authorSimilarity = incNegScore(authorSimilarity(pn, m) * 2, 2);
      // -50 - +50
      final int classificationSimilarity = classificationSimilarity(lc, m);
      // -10 - +5
      final int rankSimilarity = rankSimilarity(rank, m.getRank());
      // -5 - +1
      final int statusScore = STATUS_SCORE.get(m.getStatus());
      // -25 - 0
      final int fuzzyMatchUnlikely = fuzzyMatchUnlikelyhood(canonicalName, m);

      // preliminary total score, -5 - 20 distance to next best match coming below!
      m.setConfidence(nameSimilarity + authorSimilarity + classificationSimilarity + rankSimilarity + statusScore + fuzzyMatchUnlikely);

      if (verbose) {
        addNote(m, "Similarity: name=" + nameSimilarity);
        addNote(m, "authorship=" + authorSimilarity);
        addNote(m, "classification=" + classificationSimilarity);
        addNote(m, "rank=" + rankSimilarity);
        addNote(m, "status=" + statusScore);
        if (fuzzyMatchUnlikely<0) {
          addNote(m, "fuzzy match unlikely=" + fuzzyMatchUnlikely);
        }
      }
    }

    return matches;
  }

  private List<NameUsageMatch> queryHigher(String canonicalName, Rank rank, LinneanClassification lc, boolean verbose) {
    // do a lucene matching
    List<NameUsageMatch> matches = queryIndex(rank, canonicalName, false);
    for (NameUsageMatch m : matches) {
      // 0 - +100
      final int nameSimilarity = nameSimilarity(null, canonicalName, m);
      // -50 - +50
      final int classificationSimilarity = classificationSimilarity(lc, m);
      // -10 - +5
      final int rankSimilarity = rankSimilarity(rank, m.getRank()) * 2;
      // -5 - +1
      final int statusScore = STATUS_SCORE.get(m.getStatus());

      // preliminary total score, -5 - 20 distance to next best match coming below!
      m.setConfidence(nameSimilarity + classificationSimilarity + rankSimilarity + statusScore);

      if (verbose) {
        addNote(m, "Similarity: name=" + nameSimilarity);
        addNote(m, "classification=" + classificationSimilarity);
        addNote(m, "rank=" + rankSimilarity);
        addNote(m, "status=" + statusScore);
      }
    }

    return matches;
  }

  private List<NameUsageMatch> queryStrict(@Nullable NameType queryNameType, ParsedName pn, String canonicalName, Rank rank, LinneanClassification lc, boolean verbose) {
    // do a lucene matching
    List<NameUsageMatch> matches = queryIndex(rank, canonicalName, false);
    for (NameUsageMatch m : matches) {
      // 0 - +120
      final int nameSimilarity = nameSimilarity(queryNameType, canonicalName, m);
      // -28 - +40
      final int authorSimilarity = incNegScore(authorSimilarity(pn, m) * 4, 8);
      // -50 - +50
      final int kingdomSimilarity = incNegScore(kingdomSimilarity(htComp.toKingdom(lc.getKingdom()), htComp.toKingdom(m.getKingdom())), 10);
      // -10 - +5
      final int rankSimilarity = incNegScore(rankSimilarity(rank, m.getRank()), 10);
      // -5 - +1
      final int statusScore = STATUS_SCORE.get(m.getStatus());

      // preliminary total score, -5 - 20 distance to next best match coming below!
      m.setConfidence(nameSimilarity + authorSimilarity + kingdomSimilarity + rankSimilarity + statusScore);

      if (verbose) {
        addNote(m, "Similarity: name=" + nameSimilarity);
        addNote(m, "authorship=" + authorSimilarity);
        addNote(m, "kingdom=" + kingdomSimilarity);
        addNote(m, "rank=" + rankSimilarity);
        addNote(m, "status=" + statusScore);
      }
    }

    return matches;
  }

  private int incNegScore(int score, int factor) {
    return score < 0 ? score * factor : score;
  }


  /**
   * Use our custom similarity algorithm and compare the higher classifications to select the best match
   *
   * @return the best match, might contain no usageKey
   */
  @VisibleForTesting
  protected NameUsageMatch match(@Nullable NameType queryNameType, @Nullable ParsedName pn, String canonicalName,
                                 Rank rank, LinneanClassification lc, final MatchingMode mode, final boolean verbose) {
    if (Strings.isNullOrEmpty(canonicalName)) {
      return noMatch(100, "No name given", null);
    }

    // first try our manual hackmap
    if (hackMap.containsKey(canonicalName.toLowerCase())) {
      return hackMap.get(canonicalName.toLowerCase());
    }

    // do the matching
    List<NameUsageMatch> matches = null;
    switch (mode) {
      case FUZZY:
        matches = queryFuzzy(queryNameType, pn, canonicalName, rank, lc, verbose);
        break;
      case STRICT:
        matches = queryStrict(queryNameType, pn, canonicalName, rank, lc, verbose);
        break;
      case HIGHER:
        matches = queryHigher(canonicalName, rank, lc, verbose);
        break;
    }

    // order by confidence
    Collections.sort(matches, CONFIDENCE_ORDER);

    if (matches.size() > 0) {
      // add 0 - 5 confidence based on distance to next best match
      NameUsageMatch best = matches.get(0);
      int bestConfidence = best.getConfidence();
      int nextMatchDistance;

      if (matches.size() == 1) {
        // boost results with a single match to pick from
        nextMatchDistance = 5;
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
            Collections.sort(equalMatches, USAGE_KEY_ORDER);
            best = equalMatches.get(0);
            addNote(best, equalMatches.size() + " synonym homonyms");
          } else {
            return noMatch(99, "Multiple equal matches for " + canonicalName, verbose ? matches : null);
          }
        }
        // boost up to 5 based on distance to next match
        nextMatchDistance = Math.min(5, (bestConfidence - secondBestConfidence) / 2);
        if (verbose) {
          addNote(best, "nextMatch=" + nextMatchDistance);
        }
      }
      // normalize confidence values into the range of 0 to 100
      best.setConfidence(normConfidence(bestConfidence + nextMatchDistance));

      // finally check if match is good enough
      if (best.getConfidence() < (mode == MatchingMode.HIGHER ? MIN_CONFIDENCE_FOR_HIGHER_MATCHES : MIN_CONFIDENCE)) {
        return noMatch(99, "No match because of too little confidence", verbose ? matches : null);
      }
      // verbose and alternatives?
      if (verbose && matches.size() > 1) {
        // remove best match
        matches.remove(best);
        setAlternatives(best, matches);
        for (NameUsageMatch alt : matches) {
          alt.setConfidence(normConfidence(alt.getConfidence()));
        }
      }

      return best;
    }

    return noMatch(100, null, null);
  }

  // -12 to 8
  private int authorSimilarity(@Nullable ParsedName pn, NameUsageMatch m) {
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
      case EQUAL:
        return 2 * factor;
      case DIFFERENT:
        return -3 * factor;
    }
    return 0;
  }

  private boolean equalClassification(LinneanClassification best, LinneanClassification m) {
    for (Rank r : Rank.LINNEAN_RANKS) {
      if (best.getHigherRank(r) == null) {
        if (m.getHigherRank(r) != null) {
          return false;
        }

      } else {
        if (m.getHigherRank(r) == null || !best.getHigherRank(r).equals(m.getHigherRank(r))) {
          return false;
        }
      }
    }
    return true;
  }

  private List<NameUsageMatch> extractEqualMatches(List<NameUsageMatch> matches) {
    List<NameUsageMatch> equal = Lists.newArrayList();
    if (!matches.isEmpty()) {
      final int conf = matches.get(0).getConfidence();
      for (NameUsageMatch m : matches) {
        if (m.getConfidence().equals(conf)) {
          equal.add(m);
        } else {
          // matches are sorted by confidence!
          break;
        }
      }
    }
    return equal;
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
    setAlternatives(no, alternatives);
    return no;
  }

  private int fuzzyMatchUnlikelyhood(String canonicalName, NameUsageMatch m) {
    // ignore fuzzy matches with a terminal epithet of "indet" meaning usually indeterminate
    if (m.getMatchType() == NameUsageMatch.MatchType.FUZZY
            && m.getRank().isSpeciesOrBelow()
            && canonicalName.endsWith(" indet")) {
      return -25;
    }
    return 0;
  }

  private int nameSimilarity(@Nullable NameType queryNameType, String canonicalName, NameUsageMatch m) {
    // calculate name distance
    int confidence;
    if (canonicalName.equalsIgnoreCase(m.getCanonicalName())) {
      // straight match
      confidence = 100;
      // binomial straight match? That is pretty trustworthy
      if (queryNameType != null && STRICT_MATCH_TYPES.contains(queryNameType)) {
        confidence += 20;
      } else if (canonicalName.contains(" ")) {
        confidence += 10;
      }

    } else {
      // fuzzy - be careful!
      confidence = (int) sim.getSimilarity(canonicalName, m.getCanonicalName()) - 5;
      // fuzzy OTU match? That is dangerous, often one character/number means sth entirely different
      if (queryNameType == NameType.OTU) {
        confidence -= 50;
      }

      // modify confidence according to genus comparison in bionomials.
      // slightly trust binomials with a matching genus more, and trust less if we matched a different genus name
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
  protected int classificationSimilarity(LinneanClassification query, LinneanClassification reference) {
    // kingdom is super important
    int rate = htComp.compareHigherRank(Rank.KINGDOM, query, reference, 5, -10, -1);
    if (rate == -10) {
      // plant and animal kingdoms are better delimited than Chromista, Fungi, etc. , so punish those mismatches higher
      if (htComp.isInKingdoms(query, Kingdom.ANIMALIA, Kingdom.PLANTAE)
          && htComp.isInKingdoms(reference, Kingdom.ANIMALIA, Kingdom.PLANTAE)) {
        rate = -51;
        // plant and animal kingdoms should not be confused with Bacteria, Archaea or Viruses
      } else if (htComp.isInKingdoms(query, Kingdom.ANIMALIA, Kingdom.PLANTAE)
              && htComp.isInKingdoms(reference, Kingdom.BACTERIA, Kingdom.ARCHAEA, Kingdom.VIRUSES)) {
        rate = -31;
      }
    }
    // we rarely ever have a virus name, punish these a little more to avoid false virus matches
    if (htComp.isInKingdoms(reference, Kingdom.VIRUSES)) {
      rate -= 10;
    }
    // phylum to family
    rate += htComp.compareHigherRank(Rank.PHYLUM, query, reference, 10, -10, -1);
    rate += htComp.compareHigherRank(Rank.CLASS, query, reference, 15, -10, 0);
    rate += htComp.compareHigherRank(Rank.ORDER, query, reference, 15, -10, 0);
    rate += htComp.compareHigherRank(Rank.FAMILY, query, reference, 25, -15, 0);
    // we compare the genus only for minimal adjustments as it is part of the binomen usually
    // it helps to disambiguate in some cases though
    rate += htComp.compareHigherRank(Rank.GENUS, query, reference, 2, 1, 0);

    return minMax(-60, 50, rate);
  }


  // rate ranks from -25 to +5, zero if nothing is know
  private int rankSimilarity(Rank query, Rank ref) {
    int similarity = 0;
    if (ref != null) {
      // rate ranks lower that are not represented in the canonical, e.g. cultivars
      if (ref.isRestrictedToCode() == NomenclaturalCode.CULTIVARS) {
        similarity -= 7;
      } else if (Rank.STRAIN == ref) {
        similarity -= 7;
      }

      if (ref.isUncomparable()) {
        // this also includes informal again
        similarity -= 3;
      }

      if (query != null) {
        // both ranks exist. Compare directly
        if (query.equals(ref)) {
          similarity += 10;

        } else if (Rank.INFRASPECIFIC_NAME == query && ref.isInfraspecific()
            || Rank.INFRASPECIFIC_NAME == ref && query.isInfraspecific()) {
          // unspecific infraspecific rank
          similarity += 5;

        } else if (Rank.INFRASUBSPECIFIC_NAME == query && ref.isInfraspecific() && ref != Rank.SUBSPECIES
            || Rank.INFRASUBSPECIFIC_NAME == ref && query.isInfraspecific() && query != Rank.SUBSPECIES) {
          // unspecific infrasubspecific rank
          similarity += 5;

        } else if (query.isUncomparable()) {
          // uncomparable query ranks
          similarity -= 5;

        } else if (ref == Rank.SPECIES && query.isInfraspecific() || ref.isSupraspecific() && query.isSpeciesOrBelow()
            || query == Rank.SPECIES && ref.isInfraspecific() || query.isSupraspecific() && ref.isSpeciesOrBelow()) {
          // not good, different number of epithets means rather unalike
          similarity -= 25;

        } else {
          // rate lower the further away the ranks are
          similarity -= Math.abs(ref.ordinal() - query.ordinal());
        }
      }

    } else if (query != null) {
      // reference has no rank, rate it lower
      similarity -= 1;
    }
    return minMax(-25, 5, similarity);
  }

  // rate kingdoms from -10 to +10, zero if nothing is know
  private int kingdomSimilarity(@Nullable Kingdom k1, @Nullable Kingdom k2) {
    if (k1 == null || k2 == null) {
      return 0;
    }
    if (k1 == Kingdom.INCERTAE_SEDIS || k2 == Kingdom.INCERTAE_SEDIS) {
      return 7;
    }

    if (k1 == k2) {
      return 10;
    }
    if (VAGUE_KINGDOMS.contains(k1) && VAGUE_KINGDOMS.contains(k2)) {
      return 8;
    }
    return -10;
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
