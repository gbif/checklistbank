package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.gbif.checklistbank.neo.traverse.StartEndHandler;

import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a nested set index for parent child related nodes.
 */
public class ImportTaxonMetricsHandler implements StartEndHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ImportTaxonMetricsHandler.class);
  // neo node ids for the higher classification links
  private final LinneanClassificationKeys classification = new NameUsage();
  private final NameUsageMetrics metrics = new NameUsageMetrics();
  private final NeoMapper mapper = NeoMapper.instance();
  private int idx = 0;
  private int maxDepth;
  private int depth;

  @Override
  public void start(Node n) {
    depth++;
    if (depth > maxDepth) {
      maxDepth = depth;
    }
    n.setProperty("lft", idx++);
    Rank rank = mapper.readEnum(n, TaxonProperties.RANK, Rank.class);
    if (rank != null && rank.isLinnean()) {
      ClassificationUtils.setHigherRankKey(classification, rank, (int)n.getId());
    }
  }

  @Override
  public void end(Node n) {
    depth--;
    Rank rank = mapper.readEnum(n, TaxonProperties.RANK, Rank.class);
    metrics.setNumSynonyms(countSynonyms(n));
    metrics.setNumChildren(countChildren(n));
    // persist
    n.setProperty("rgt", idx++);
    mapper.store(n, metrics, false);
    mapper.store(n, classification, false);
    // remove this rank
    if (rank != null && rank.isLinnean()) {
      ClassificationUtils.setHigherRankKey(classification, rank, null);
      //NeoUtils.setNumByRank(metrics, rank, 0);
    }
    //LOG.debug("[{}] {}   {}-{}", prop(n, DwcTerm.taxonRank), prop(n, DwcTerm.scientificName), n.getProperty(PROP_LFT), n.getProperty(PROP_RGT));
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  private int countChildren(Node n) {
    return 0;
  }

  private int countSynonyms(Node n) {
    return 0;
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

}
