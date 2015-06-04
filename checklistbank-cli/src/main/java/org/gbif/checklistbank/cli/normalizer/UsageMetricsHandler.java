package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.traverse.StartEndHandler;
import org.gbif.checklistbank.neo.traverse.Traversals;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds higher classification keys and NameUsageMetrics for all accepted usages.
 * Synonym usages do not need a metrics record as its zero all over.
 */
public class UsageMetricsHandler implements StartEndHandler {

  private static final Logger LOG = LoggerFactory.getLogger(UsageMetricsHandler.class);
  // neo node ids for the higher classification links
  private final NameUsage classification = new NameUsage();
  private final LinkedList<NameUsageMetrics> parentCounts = Lists.newLinkedList();
  private final NeoMapper mapper = NeoMapper.instance();
  private int counter;
  private int roots;
  private int maxDepth;
  private int depth;
  private int synonyms;
  private Map<Origin, Integer> countByOrigin = Maps.newHashMap();
  private Map<Rank, Integer> countByRank = Maps.newHashMap();

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
    // final metrics update
    NameUsageMetrics metrics = parentCounts.removeLast();
    metrics.setNumSynonyms(processSynonyms(n));
    metrics.setNumDescendants(counter - metrics.getNumDescendants());
    // persist metrics and classification with nub key
    mapper.store(n, classification, false);
    mapper.store(n, metrics, false);
    // remove this rank from current classification
    if (rank != null && rank.isLinnean()) {
      ClassificationUtils.setHigherRankKey(classification, rank, null);
      ClassificationUtils.setHigherRank(classification, rank, null);
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
    for (Node syn : Traversals.SYNONYMS.traverse(n).nodes()) {
      synCounter++;
      count(syn);
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
    //System.out.println("#" + n.getId() + " " + mapper.readEnum(n, "taxonomicStatus", TaxonomicStatus.class, null) + " " + rank + " ["+origin+"] " + mapper.readScientificName(n));
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
