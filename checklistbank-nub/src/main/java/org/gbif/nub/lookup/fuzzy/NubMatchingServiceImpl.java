/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.v2.NameUsageMatch2;
import org.gbif.api.v2.RankedName;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.utils.CleanupUtils;
import org.gbif.checklistbank.utils.NameParsers;
import org.gbif.checklistbank.utils.RankUtils;
import org.gbif.nub.lookup.NameUsageMatchingService2;
import org.gbif.nub.lookup.similarity.ScientificNameSimilarity;
import org.gbif.nub.lookup.similarity.StringSimilarity;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.*;

@Service
public class NubMatchingServiceImpl implements NameUsageMatchingService, NameUsageMatchingService2 {

  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingServiceImpl.class);
  private static final int MIN_CONFIDENCE = 80;
  private static final int MIN_CONFIDENCE_FOR_HIGHER_MATCHES = 90;
  private static final int MIN_CONFIDENCE_ACROSS_RANKS = 1;
  private static final Set<Kingdom> VAGUE_KINGDOMS = ImmutableSet.of(Kingdom.ARCHAEA, Kingdom.BACTERIA, Kingdom.FUNGI, Kingdom.CHROMISTA, Kingdom.PROTOZOA, Kingdom.INCERTAE_SEDIS);
  private static final List<Rank> DWC_RANKS_REVERSE = ImmutableList.copyOf(Lists.reverse(Rank.DWC_RANKS));
  private static ConfidenceOrder CONFIDENCE_ORDER = new ConfidenceOrder();
  private final NubIndex nubIndex;
  private final HigherTaxaComparator htComp;
  // name string to usageId
  private Map<String, NameUsageMatch> hackMap = Maps.newHashMap();
  private final StringSimilarity sim = new ScientificNameSimilarity();

  private static final Set<NameType> STRICT_MATCH_TYPES = ImmutableSet.of(NameType.OTU, NameType.VIRUS, NameType.HYBRID);
  private static final List<Rank> HIGHER_QUERY_RANK = ImmutableList.of(Rank.SPECIES, Rank.GENUS, Rank.FAMILY, Rank.ORDER, Rank.CLASS, Rank.PHYLUM, Rank.KINGDOM);
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
   */
  @Autowired
  public NubMatchingServiceImpl(NubIndex nubIndex, HigherTaxaComparator htComp) {
    this.nubIndex = nubIndex;
    this.htComp = htComp;
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
    addAlternatives(match, firstMatch.getAlternatives());
    return match;
  }

  @Override
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
    m2.getDiagnostics().setConfidence(m.getConfidence());
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
   * Adds the given alternatives to the alternatives existing in the match,
   * making sure we dont get infinite recursions my clearing all alternate matches on the arguments
   */
  private static void addAlternatives(NameUsageMatch match, List<NameUsageMatch> alts) {
    if (match.getAlternatives() != null && alts != null) {
      alts.addAll(match.getAlternatives());
    }
    setAlternatives(match, alts);
  }

  /**
   * Sets the alternative on a match making sure we dont get infinite recursions my clearing all alternate matches on the arguments
   */
  private static void setAlternatives(NameUsageMatch match, List<NameUsageMatch> alts) {
    if (alts != null) {
      Set<Integer> keys = new HashSet<>(); // remember keys and make unique - we can have the same usages in here
      ListIterator<NameUsageMatch> iter = alts.listIterator();
      while (iter.hasNext()) {
        NameUsageMatch m = iter.next();
        if (Objects.equals(m.getUsageKey(), match.getUsageKey()) || keys.contains(m.getUsageKey())) {
          // same usage, remove!
          iter.remove();
        } else if (m.getAlternatives() != null && !m.getAlternatives().isEmpty()) {
          m.setAlternatives(Lists.<NameUsageMatch>newArrayList());
        }
        keys.add(m.getUsageKey());
      }
    }
    match.setAlternatives(alts);
  }

  // Wrapper method doing the time tracking and logging only.
  @Override
  public NameUsageMatch match(String scientificName, @Nullable Rank rank, @Nullable LinneanClassification classification, boolean strict, boolean verbose) {
    return match2(null, scientificName, null, null, null, null, rank, classification, new HashSet<>(), strict, verbose);
  }

  @Override
  public NameUsageMatch match2(@Nullable Integer usageKey, @Nullable String scientificName, @Nullable String authorship,
                               @Nullable String genericName, @Nullable String specificEpithet, @Nullable String infraSpecificEpithet,
                               @Nullable Rank rank, @Nullable LinneanClassification classification, Set<Integer> exclude, boolean strict, boolean verbose) {
    StopWatch watch = new StopWatch();
    watch.start();
    NameUsageMatch match;

    // When provided a usageKey is used exclusively
    if (usageKey != null) {
      match = nubIndex.matchByUsageId(usageKey);

      // maintain backward compatible API
      if (match == null ) {
        match = new NameUsageMatch();
        match.setMatchType(NameUsageMatch.MatchType.NONE);
        match.setConfidence(100);
      } else {
        match.setNote("All provided names were ignored since the usageKey was provided");
        match.setMatchType(NameUsageMatch.MatchType.EXACT);
      }
      watch.stop();
      LOG.debug("{} Match of usageKey[{}] in {}", match.getMatchType(), usageKey, watch);
    } else {
      NameNRank nr = NameNRank.build(scientificName, authorship, genericName, specificEpithet, infraSpecificEpithet, rank, classification);
      match = matchInternal(nr.name, nr.rank, classification, exclude, strict, verbose);
      watch.stop();
      LOG.debug("{} Match of {} >{}< to {} [{}] in {}", match.getMatchType(), nr.rank, nr.name, match.getUsageKey(), match.getScientificName(), watch);
    }

    return match;
  }

  /**
   * Real method doing the work
   */
  private NameUsageMatch matchInternal(@Nullable String scientificName, @Nullable Rank rank, @Nullable LinneanClassification classification, Set<Integer> exclude, boolean strict, boolean verbose) {

    ParsedName pn = null;
    NameType queryNameType;
    MatchingMode mainMatchingMode = strict ? MatchingMode.STRICT : MatchingMode.FUZZY;

    // clean strings, replacing odd whitespace, iso controls and trimming
    scientificName = CleanupUtils.clean(scientificName);
    if (classification == null) {
      classification = new NameUsageMatch();
    } else {
      cleanClassification(classification);
    }

    // treat names that are all upper or lower case special - they cannot be parsed properly so rather use them as they are!
    if (scientificName != null && (scientificName.toLowerCase().equals(scientificName) || scientificName.toUpperCase().equals(scientificName))) {
      LOG.debug("All upper or lower case name found. Don't try to parse: {}", scientificName);
      queryNameType = null;
      if (mainMatchingMode != MatchingMode.STRICT) {
        // turn off fuzzy matching
        mainMatchingMode = MatchingMode.STRICT;
      }
      if (rank == null) {
        rank = Rank.UNRANKED;
      }

    } else {
      try {
        // use name parser to make the name a canonical one
        // we build the name with flags manually as we wanna exclude indet. names such as "Abies spec." and rather match them to Abies only
        pn = NameParsers.INSTANCE.parse(scientificName, rank);
        queryNameType = pn.getType();
        scientificName = pn.buildName(false, false, false, false, false, false, true, true, false, false, false, false, false, false);
        // parsed genus provided for a name lower than genus?
        if (classification.getGenus() == null && pn.getGenusOrAbove() != null && pn.getRank() != null && pn.getRank().isInfragenericStrictly() ) {
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
    }

    NameUsageMatch match1 = match(queryNameType, pn, scientificName, rank, classification, exclude, mainMatchingMode, verbose);
    // use genus higher match instead of fuzzy one?
    // https://github.com/gbif/portal-feedback/issues/2930
    if (match1.getMatchType() == NameUsageMatch.MatchType.FUZZY &&
        match1.getRank() != null && match1.getRank().isSpeciesOrBelow() &&
        pn != null && !match1.getCanonicalName().startsWith(pn.getGenusOrAbove()+" ") &&
        nextAboveGenusDiffers(classification, match1)
    ) {
      NameUsageMatch genusMatch = match(pn.getType(), null, pn.getGenusOrAbove(), Rank.GENUS, classification, exclude, MatchingMode.HIGHER, verbose);
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
    boolean supraGenericOnly = false;
    if (pn != null && pn.getGenusOrAbove() != null) {
      if (pn.getSpecificEpithet() != null || (rank != null && rank.isInfrageneric())) {
        if (pn.getInfraSpecificEpithet() != null || (rank != null && rank.isInfraspecific())) {
          // try with species
          String species = pn.canonicalSpeciesName();
          match = match(pn.getType(), null, species, Rank.SPECIES, classification, exclude, MatchingMode.FUZZY, verbose);
          if (isMatch(match)) {
            return higherMatch(match, match1);
          }
        }

        // try with genus
        // we're not sure if this is really a genus, so don't set the rank
        // we get non species names sometimes like "Chaetognatha eyecount" that refer to a phylum called
        // "Chaetognatha"
        match = match(pn.getType(), null, pn.getGenusOrAbove(), null, classification, exclude, MatchingMode.HIGHER, verbose);
        if (isMatch(match)) {
          return higherMatch(match, match1);
        }
        supraGenericOnly = true;
      }
    }

    // use classification query strings
    for (Rank qr : HIGHER_QUERY_RANK) {
      if (supraGenericOnly && !qr.isSuprageneric()) continue;
      String name = ClassificationUtils.getHigherRank(classification, qr);
      if (!StringUtils.isEmpty(name)) {
        match = match(null, null, name, qr, classification, exclude, MatchingMode.HIGHER, verbose);
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
        String val = CleanupUtils.clean(cl.getHigherRank(r));
        if (val != null) {
          Matcher m = FIRST_WORD.matcher(val);
          if (m.find()) {
            ClassificationUtils.setHigherRank(cl, r, m.group(1));
          }
        }
      }
    }
  }

  private List<NameUsageMatch> queryIndex(Rank rank, String canonicalName, boolean fuzzy) {
    List<NameUsageMatch> matches = nubIndex.matchByName(canonicalName, fuzzy, 50);
    // flag aggregate matches, see https://github.com/gbif/portal-feedback/issues/2935
    final int before = matches.size();
    matches.removeIf(m -> {
      if (m.getMatchType() == NameUsageMatch.MatchType.EXACT
              && rank == Rank.SPECIES_AGGREGATE
              && m.getRank() != Rank.SPECIES_AGGREGATE) {
        LOG.info("Species aggregate match found for {} {}. Ignore and prefer higher matches", m.getRank(), m.getScientificName());
        return true;
      }
      return false;
    });
    // did we remove matches because of aggregates? Then also remove any fuzzy matches
    if (matches.size() < before) {
      matches.removeIf(m -> {
        if (m.getMatchType() == NameUsageMatch.MatchType.FUZZY) {
          LOG.info("Species aggregate match found for {}. Ignore also fuzzy match {} {}", canonicalName, m.getRank(), m.getScientificName());
          return true;
        }
        return false;
      });
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
  protected NameUsageMatch match(@Nullable NameType queryNameType, @Nullable ParsedName pn, @Nullable String canonicalName,
                         Rank rank, LinneanClassification lc, Set<Integer> exclude, final MatchingMode mode, final boolean verbose) {
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

    // exclude any matches against the explicit exclusion list
    if (exclude != null && !exclude.isEmpty()) {
      for (NameUsageMatch m : matches) {
        if (exclude.contains(m.getUsageKey())) {
          m.setConfidence(0);
          addNote(m, "excluded by "+m.getUsageKey());
        } else {
          for (Rank r : Rank.DWC_RANKS) {
            if (exclude.contains(m.getHigherRankKey(r))) {
              m.setConfidence(0);
              addNote(m, "excluded by "+m.getHigherRankKey(r));
              break;
            }
          }
        }
      }
    }
    // order by confidence
    Collections.sort(matches, CONFIDENCE_ORDER);

    // having the pre-normalized confidence is necessary to understand usage selection in some cases
    if (verbose) {
      for (NameUsageMatch match : matches) {
        addNote(match, "score=" + match.getConfidence());
      }
    }

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
        // we have more than one match to choose from
        int secondBestConfidence = matches.get(1).getConfidence();

        // Do our results fall within the confidence score range AND differ across classes?
        boolean ambiguousAcrossClasses = similarButSpanRank(matches, MIN_CONFIDENCE_ACROSS_RANKS, Rank.CLASS);

        if (bestConfidence == secondBestConfidence || ambiguousAcrossClasses) {
          // similarly good matches, happens when there are homonyms in the nub as synonyms only

          // If we have similar results spanning classes, compare them all
          int threshold = ambiguousAcrossClasses ? MIN_CONFIDENCE_ACROSS_RANKS : 0;
          List<NameUsageMatch> suitableMatches = extractMatchesOfInterest(matches, threshold);
          boolean sameClassification = true;
          for (NameUsageMatch m : suitableMatches) {
            if (!equalClassification(best, m)) {
              sameClassification = false;
              break;
            }
          }
          if (sameClassification) {
            // if they both have the same classification pick the one with the lowest, hence oldest id!
            Collections.sort(suitableMatches, USAGE_KEY_ORDER);
            best = suitableMatches.get(0);
            addNote(best, suitableMatches.size() + " synonym homonyms");
          } else {
            best = matchLowestDenominator(canonicalName, suitableMatches);
            if (!isMatch(best)) {
              return noMatch(99, "Multiple equal matches for " + canonicalName, verbose ? matches : null);
            }
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

  /**
   * Returns true when the preferred match has a classification that differs to the other matches within the confidence
   * threshold, when compared to the stated rank.
   */
  @VisibleForTesting
  boolean similarButSpanRank(List<NameUsageMatch> matches, int confidenceThreshold, Rank rank) {
    boolean similarButSpanRanks = false;
    if (matches.size() > 1) {
      NameUsageMatch best = matches.get(0);
      for (int i=1; i<matches.size(); i++) {
        NameUsageMatch curr = matches.get(i);

        if (best.getConfidence() - curr.getConfidence() <= confidenceThreshold) {
          if (!equalClassification(best, curr, rank)) {
            similarButSpanRanks = true; // within confidence threshold but higher classifications differ
            break;
          }
        } else {
          break; // we're past the confidence threshold
        }
      }
    }
    return similarButSpanRanks;
  }

  /**
   * Tries to match to the lowest common higher rank from all best equal matches
   */
  private NameUsageMatch matchLowestDenominator(String canonicalName, List<NameUsageMatch> matches) {
    for (Rank r : DWC_RANKS_REVERSE) {
      Integer higherKey = matches.get(0).getHigherRankKey(r);
      if (higherKey == null) continue;

      for (NameUsageMatch m : matches) {
        if (!Objects.equals(higherKey, m.getHigherRankKey(r))) {
          higherKey = null;
          break;
        }
      }
      // did all equal matches have the same higherKey?
      if (higherKey != null) {
        // NPE safetly first - maybe the key is missing in the index
        NameUsageMatch match = nubIndex.matchByUsageId(higherKey);
        if (match != null) {
          match.setMatchType(NameUsageMatch.MatchType.HIGHERRANK);
          return match;
        }
      }
    }
    return noMatch(99, "No lowest denominator in equal matches for " + canonicalName, null);
  }

  // -12 to 8
  private int authorSimilarity(@Nullable ParsedName pn, NameUsageMatch m) {
    int similarity = 0;
    if (pn != null) {
      try {
        ParsedName mpn = NameParsers.INSTANCE.parse(m.getScientificName(), m.getRank());
        // authorship comparison was requested!
        Equality recomb = authComp.compare(pn.getAuthorship(), pn.getYear(), mpn.getAuthorship(), mpn.getYear());
        Equality bracket = authComp.compare(pn.getBracketAuthorship(), pn.getBracketYear(), mpn.getBracketAuthorship(), mpn.getBracketYear());
        if (bracket == Equality.UNKNOWN) {
          // dont have 2 bracket authors to compare. Try with combination authors as brackets are sometimes forgotten or wrong
          if (pn.getBracketAuthorship() != null) {
            bracket = authComp.compare(pn.getBracketAuthorship(), pn.getBracketYear(), mpn.getAuthorship(), mpn.getYear());
          } else if (mpn.getBracketAuthorship() != null) {
            bracket = authComp.compare(pn.getAuthorship(), pn.getYear(), mpn.getBracketAuthorship(), mpn.getBracketYear());
          }
          if (bracket == Equality.EQUAL) {
            similarity -= 1;
          } else if (bracket == Equality.DIFFERENT) {
            similarity += 1;
          }
        }

        similarity += equality2Similarity(recomb, 3);
        similarity += equality2Similarity(bracket, 1);

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
    return equalClassification(best, m, null);
  }

  /**
   * Compares classifications starting from kingdom stopping after the stopRank if provided.
   */
  private boolean equalClassification(LinneanClassification best, LinneanClassification m, Rank stopRank) {
    for (Rank r : Rank.LINNEAN_RANKS) {
      if (stopRank != null && stopRank.higherThan(r)) {
        break;

      } else if (best.getHigherRank(r) == null) {
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

  /**
   * Returns all matches that are within the given threshold of the best.
   */
  private List<NameUsageMatch> extractMatchesOfInterest(List<NameUsageMatch> matches, int threshold) {
    List<NameUsageMatch> target = Lists.newArrayList();
    if (!matches.isEmpty()) {
      final int conf = matches.get(0).getConfidence();
      for (NameUsageMatch m : matches) {
        if (conf - m.getConfidence() <= threshold) {
          target.add(m);
        } else {
          // matches are sorted by confidence!
          break;
        }
      }
    }
    return target;
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


  @VisibleForTesting
  // rate ranks from -25 to +5, zero if nothing is know
  protected static int rankSimilarity(Rank query, Rank ref) {
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

        } else if (
            either(query, ref, r -> r == Rank.INFRASPECIFIC_NAME, Rank::isInfraspecific) ||
            either(query, ref, r -> r == Rank.INFRASUBSPECIFIC_NAME, Rank::isInfrasubspecific) ||
            either(query, ref, r -> r == Rank.INFRAGENERIC_NAME, Rank::isInfrageneric)
        ) {
          // unspecific rank matching its group
          similarity += 5;

        } else if (either(query, ref, r -> r == Rank.INFRAGENERIC_NAME, r -> r == Rank.GENUS)) {
          similarity += 4;

        } else if (either(query, ref, not(Rank::notOtherOrUnknown))) {
          // unranked
          similarity = 0;

        } else if (either(query, ref, (r1, r2) -> r1 == Rank.SPECIES && r2 == Rank.SPECIES_AGGREGATE)) {
          similarity += 2;

        } else if (either(query,ref, (r1, r2) ->
            (r1 == Rank.SPECIES || r1 == Rank.SPECIES_AGGREGATE) && r2.isInfraspecific() ||
            r1.isSupraspecific() && r1 != Rank.SPECIES_AGGREGATE && r2.isSpeciesOrBelow()
        )) {
          // not good, different number of epithets means very unalike
          similarity -= 30;

        } else if (either(query, ref, r -> !r.isSuprageneric(), Rank::isSuprageneric)) {
          // we often have genus "homonyms" with higher taxa, e.g. Vertebrata, Dinosauria. Avoid this
          similarity -= 35;

        } else {
          // GENERIC: rate lower the further away the ranks are
          similarity -= Math.abs(ref.ordinal() - query.ordinal());
        }
      }

    } else if (query != null) {
      // reference has no rank, rate it lower
      similarity -= 1;
    }
    return minMax(-35, 6, similarity);
  }

  private static Predicate<Rank> not(Predicate<Rank> predicate) {
    return predicate.negate();
  }

  private static boolean either(Rank r1, Rank r2, Predicate<Rank> p) {
    return p.test(r1) || p.test(r2);
  }

  private static boolean either(Rank r1, Rank r2, BiFunction<Rank, Rank, Boolean> evaluator) {
    return evaluator.apply(r1, r2) || evaluator.apply(r2, r1);
  }

  private static boolean either(Rank r1, Rank r2, Predicate<Rank> p1, Predicate<Rank> p2) {
    return p1.test(r1) && p2.test(r2) || p2.test(r1) && p1.test(r2);
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
