package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.checklistbank.cli.model.NameUsageNode;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.service.MatchingService;

import com.google.common.base.Preconditions;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assigns existing usageKey through a backbone match.
 */
public class NubMatchHandler implements StartEndHandler {

  private static final Logger LOG = LoggerFactory.getLogger(NubMatchHandler.class);
  // neo node ids for the higher classification links
  private final MatchingService matchingService;
  private final UsageDao dao;
  private int counter;
  private LinneanClassification currClassification = new NameUsage();

  public NubMatchHandler(MatchingService lookup, UsageDao dao) {
    this.dao = dao;
    this.matchingService = Preconditions.checkNotNull(lookup, "Backbone matching client required");
  }

  @Override
  public void start(Node n) {
    // increase counters
    counter++;
    if (counter % 1000 == 0) {
      LOG.debug("Nub matching done for: {}", counter);
    }
    NameUsage u = dao.readUsage(n, false);
    // update classification
    ClassificationUtils.setHigherRank(currClassification, u.getRank(), u.getCanonicalName());
  }

  @Override
  public void end(Node n) {
    NameUsage u = dao.readUsage(n, false);
    // nub lookup
    NameUsageNode nn = new NameUsageNode(n, u, false);
    matchToNub(nn);
    processSynonyms(nn);
    // remove classification
    ClassificationUtils.setHigherRank(currClassification, u.getRank(), null);
  }

  /**
   * Use an in memory backbone matching so there is no need for retries.
   * The resulting usageKey of the match and potential issues will be stored in the neo node.
   */
  private void matchToNub(NameUsageNode nn) {
    ParsedName pn = dao.readName(nn.node.getId());
    if (pn == null) {
      LOG.warn("No parsed name found for {} {}", nn.node, nn.usage.getScientificName());
      return;
    }
    Integer nubKey = matchingService.matchStrict(pn, nn.usage.getScientificName(), nn.usage.getRank(), currClassification);

    // store nub key
    nn.usage.setNubKey(nubKey);
    if (nubKey == null) {
      LOG.debug("Failed nub match: {} {}", nn.usage.getRank(), nn.usage.getScientificName());
      nn.addIssue(NameUsageIssue.BACKBONE_MATCH_NONE);
    }
    nn.modified = true;
    dao.store(nn, false);
  }

  /**
   * Process all synonymsTD doing a nub lookup for each of them
   *
   * @return the number of processed synonymsTD
   */
  private void processSynonyms(NameUsageNode nn) {
    for (Node syn : Traversals.SYNONYMS.traverse(nn.node).nodes()) {
      NameUsage s = dao.readUsage(syn, false);
      matchToNub(new NameUsageNode(syn, s, false));
    }
  }

}
