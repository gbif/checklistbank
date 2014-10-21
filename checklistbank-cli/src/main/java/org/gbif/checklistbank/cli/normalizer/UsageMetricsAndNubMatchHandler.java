package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.traverse.StartEndHandler;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.google.common.base.Preconditions;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds higher classification keys and NameUsageMetrics for all accepted usages.
 * Synonym usages do not need a metrics record as its zero all over.
 */
public class UsageMetricsAndNubMatchHandler implements StartEndHandler {

  private static final Logger LOG = LoggerFactory.getLogger(UsageMetricsAndNubMatchHandler.class);
  // neo node ids for the higher classification links
  private final NameUsage classification = new NameUsage();
  private final LinkedList<NameUsageMetrics> parentCounts = Lists.newLinkedList();
  private final NeoMapper mapper = NeoMapper.instance();
  private final NameUsageMatchingService matchingService;
  private final TraversalDescription synonymsTD;
  private int counter;
  private int roots;
  private int maxDepth;
  private int depth;
  private int synonyms;
  private Map<Origin, Integer> countByOrigin = Maps.newHashMap();
  private Map<Rank, Integer> countByRank = Maps.newHashMap();

  public UsageMetricsAndNubMatchHandler(NameUsageMatchingService matchingService, GraphDatabaseService db) {
    this.matchingService = Preconditions.checkNotNull(matchingService, "Backbone matching client required");
    synonymsTD = db.traversalDescription()
      .breadthFirst()
      .relationships(RelType.SYNONYM_OF, Direction.INCOMING)
      .evaluator(Evaluators.toDepth(1))
      .evaluator(Evaluators.excludeStartPosition());
  }

  @Override
  public void start(Node n) {
    // increase counters
    count(n);
    counter++;
    depth++;
    if (depth > maxDepth) {
      maxDepth = depth;
    }
    if (depth == 1) {
      roots++;
    }

    Rank rank = mapper.readRank(n);
    if (rank != null && rank.isLinnean()) {
      ClassificationUtils.setHigherRankKey(classification, rank, (int)n.getId());
      ClassificationUtils.setHigherRank(classification, rank, mapper.readCanonicalName(n));
    }
    // for linnean ranks increase all parent metrics
    if (rank != null && rank.isLinnean() && rank != Rank.KINGDOM) {
      for (NameUsageMetrics m : parentCounts) {
        setNumByRank(m, rank, m.getNumByRank(rank) + 1);
      }
    }
    // increase direct parents children counter by one
    if (!parentCounts.isEmpty()) {
      parentCounts.getLast().setNumChildren(parentCounts.getLast().getNumChildren() + 1);
    }
    // add new metrics to list of parent metrics
    NameUsageMetrics m = new NameUsageMetrics();
    // keep current total counter state so we can calculate the difference for the num descendants when coming up again
    m.setNumDescendants(counter);
    parentCounts.add(m);
  }

  @Override
  public void end(Node n) {
    depth--;
    Rank rank = mapper.readRank(n);
    String name = mapper.readScientificName(n);
    // final metrics update
    NameUsageMetrics metrics = parentCounts.removeLast();
    metrics.setNumSynonyms(processSynonyms(n));
    metrics.setNumDescendants(counter - metrics.getNumDescendants());
    // nub lookup
    matchToNub(n, classification, name, rank);
    // persist metrics and classification with nub key
    mapper.store(n, classification, false);
    mapper.store(n, metrics, false);
    // remove this rank from current classification
    if (rank != null && rank.isLinnean()) {
      ClassificationUtils.setHigherRankKey(classification, rank, null);
      ClassificationUtils.setHigherRank(classification, rank, null);
    }
  }

  /**
   * Use the backbone matching webservice and make sure the service is working.
   * We continue with retries for up to 12h before we let an IllegalStateException bring down the entire normalization.
   * @param n
   * @param usage
   * @param name
   * @param rank
   */
  private void matchToNub(Node n, LinneanClassification usage, String name, Rank rank) {
    NameUsageMatch match = null;
    final long started = System.currentTimeMillis();
    boolean first = true;
    while (match == null) {
      try {
        match = matchingService.match(name, rank, usage, true, false);
      } catch (Exception e) {
        LOG.debug("Nub matching for >{}< failed. Sleep and then retry", name, e);
        try {
          // first time we retry after 5s, then after every minute
          if (first) {
            Thread.sleep(5000);
            first = false;
          } else {
            // check if we tried for a long time already
            long sinceHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - started);
            if (sinceHours > 12) {
              throw new IllegalStateException("Backbone matching service unavailable for at least 12h. Interrupting normalization!");
            }
            Thread.sleep(60000);
          }
        } catch (InterruptedException e1) {
        }
      }
    }

    // store nub key
    mapper.setNubKey(n, match.getUsageKey());
    if (match.getUsageKey() == null) {
      LOG.debug("Failed nub match: {}", name);
      mapper.addIssue(n, NameUsageIssue.BACKBONE_MATCH_NONE);

    } else if (match.getMatchType() == NameUsageMatch.MatchType.FUZZY) {
      mapper.addIssue(n, NameUsageIssue.BACKBONE_MATCH_FUZZY);
    }
  }

  public NormalizerStats getStats(int ignored, List<String> cycles) {
    return new NormalizerStats(roots, maxDepth, synonyms, ignored, countByOrigin, countByRank, cycles);
  }

  /**
   * Process all synonymsTD doing a nub lookup for each of them
   * @return the number of processed synonymsTD
   */
  private int processSynonyms(Node n) {
    int synCounter = 0;
    for (Node syn : synonymsTD.traverse(n).nodes()) {
      synCounter++;
      count(syn);
      matchToNub(syn, classification, mapper.readScientificName(syn), mapper.readRank(n));
    }
    synonyms = synonyms + synCounter;
    return synCounter;
  }

  private static void setNumByRank(NameUsageMetrics u, Rank rank, int count) {
    if (rank == Rank.PHYLUM) {
      u.setNumPhylum(count);
    }
    if (rank == Rank.CLASS) {
      u.setNumClass(count);
    }
    if (rank == Rank.ORDER) {
      u.setNumOrder(count);
    }
    if (rank == Rank.FAMILY) {
      u.setNumFamily(count);
    }
    if (rank == Rank.GENUS) {
      u.setNumGenus(count);
    }
    if (rank == Rank.SUBGENUS) {
      u.setNumSubgenus(count);
    }
    if (rank == Rank.SPECIES) {
      u.setNumSpecies(count);
    }
  }


  private void count(Node n) {
    Rank rank = mapper.readRank(n);
    Origin origin = mapper.readOrigin(n);
    // please leave outcommented code for debugging
    // System.out.println("#" + n.getId() + " " + mapper.readEnum(n, "taxonomicStatus", TaxonomicStatus.class, null) + " " + rank + " ["+origin+"] " + mapper.readScientificName(n));
    if (origin != null) {
      if (!countByOrigin.containsKey(origin)) {
        countByOrigin.put(origin, 1);
      } else {
        countByOrigin.put(origin, countByOrigin.get(origin) + 1);
      }
    }
    if (rank != null) {
      if (!countByRank.containsKey(rank)) {
        countByRank.put(rank, 1);
      } else {
        countByRank.put(rank, countByRank.get(rank) + 1);
      }
    }
  }
}
