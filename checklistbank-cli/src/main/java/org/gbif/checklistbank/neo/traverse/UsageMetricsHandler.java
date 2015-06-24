package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.model.ClassificationKeys;
import org.gbif.checklistbank.neo.model.NeoTaxon;
import org.gbif.checklistbank.neo.model.UsageFacts;
import org.gbif.checklistbank.neo.UsageDao;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds higher classification keys (not the verbatim names) and NameUsageMetrics for all accepted usages.
 * Synonym usages do not need a data record as its zero all over.
 */
public class UsageMetricsHandler implements StartEndHandler {

  private static final Logger LOG = LoggerFactory.getLogger(UsageMetricsHandler.class);
  // neo node ids for the higher classification links
  private final ClassificationKeys classification = new ClassificationKeys();
  private final LinkedList<NameUsageMetrics> parentCounts = Lists.newLinkedList();
  private int counter;
  private int roots;
  private int maxDepth;
  private int depth;
  private int synonyms;
  private Map<Origin, Integer> countByOrigin = Maps.newHashMap();
  private Map<Rank, Integer> countByRank = Maps.newHashMap();
  private final UsageDao dao;

  public UsageMetricsHandler(UsageDao dao) {
    this.dao = dao;
  }

  @Override
  public void start(Node n) {
    NeoTaxon nt = dao.read(n);
    // increase counters
    count(nt);
    counter++;
    depth++;
    if (depth > maxDepth) {
      maxDepth = depth;
    }
    if (depth == 1) {
      roots++;
    }
    if (nt.rank != null && nt.rank.isLinnean()) {
      ClassificationUtils.setHigherRankKey(classification, nt.rank, (int)n.getId());
    }
    // for linnean ranks increase all parent data
    if (nt.rank != null && nt.rank.isLinnean() && nt.rank != Rank.KINGDOM) {
      for (NameUsageMetrics m : parentCounts) {
        setNumByRank(m, nt.rank, m.getNumByRank(nt.rank) + 1);
      }
    }
    // increase direct parents children counter by one
    if (!parentCounts.isEmpty()) {
      parentCounts.getLast().setNumChildren(parentCounts.getLast().getNumChildren() + 1);
    }
    // add new data to list of parent data
    NameUsageMetrics m = new NameUsageMetrics();
    // keep current total counter state so we can calculate the difference for the num descendants when coming up again
    m.setNumDescendants(counter);
    parentCounts.add(m);
  }

  @Override
  public void end(Node n) {
    depth--;
    NeoTaxon nt = dao.read(n);
    // final data update
    NameUsageMetrics metrics = parentCounts.removeLast();
    metrics.setNumSynonyms(processSynonyms(n));
    metrics.setNumDescendants(counter - metrics.getNumDescendants());

    // persist data and classification with nub key
    UsageFacts facts = new UsageFacts();
    facts.metrics = metrics;
    facts.classification = classification;
    dao.store(n.getId(), facts);

    // remove this rank from current classification
    if (nt.rank != null && nt.rank.isLinnean()) {
      ClassificationUtils.setHigherRankKey(classification, nt.rank, null);
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
      NeoTaxon nt = dao.read(syn);
      synCounter++;
      count(nt);
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

  private void count(NeoTaxon nt) {
    // please leave outcommented code for debugging
    //System.out.println("#" + node.getId() + " " + mapper.readEnum(node, "taxonomicStatus", TaxonomicStatus.class, null) + " " + rank + " ["+origin+"] " + mapper.readScientificName(node));
    if (nt.origin != null) {
      if (!countByOrigin.containsKey(nt.origin)) {
        countByOrigin.put(nt.origin, 1);
      } else {
        countByOrigin.put(nt.origin, countByOrigin.get(nt.origin) + 1);
      }
    }
    if (nt.rank != null) {
      if (!countByRank.containsKey(nt.rank)) {
        countByRank.put(nt.rank, 1);
      } else {
        countByRank.put(nt.rank, countByRank.get(nt.rank) + 1);
      }
    }
  }

}
