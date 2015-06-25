package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.model.NameUsageNode;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assigns existing usageKey through a backbone match.
 */
public class NubMatchHandler implements StartEndHandler {

  private static final Logger LOG = LoggerFactory.getLogger(NubMatchHandler.class);
  // neo node ids for the higher classification links
  private final NameUsage classification = new NameUsage();
  private final NameUsageMatchingService matchingService;
  private int counter;
  private boolean warnSlowMatching = true;
  private final UsageDao dao;

  public NubMatchHandler(NameUsageMatchingService matchingService, UsageDao dao) {
    this.dao = dao;
    this.matchingService = Preconditions.checkNotNull(matchingService, "Backbone matching client required");
  }

  @Override
  public void start(Node n) {
    // increase counters
    counter++;
    if (counter % 10000 == 0) {
      warnSlowMatching = true;
      LOG.debug("Metrics & nub matching done for: {}", counter);
    }
    NameUsage u = dao.readUsage(n, false);
    if (u.getRank() != null && u.getRank().isLinnean()) {
      ClassificationUtils.setHigherRankKey(classification, u.getRank(), (int)n.getId());
      ClassificationUtils.setHigherRank(classification, u.getRank(), u.getCanonicalName());
    }
  }

  @Override
  public void end(Node n) {
    NameUsage u = dao.readUsage(n, false);
    // nub lookup
    NameUsageNode nn = new NameUsageNode(n, u, false);
    matchToNub(nn, classification, u.getScientificName(), u.getRank());
    processSynonyms(nn);
    // remove this rank from current classification
    if (u.getRank() != null && u.getRank().isLinnean()) {
      ClassificationUtils.setHigherRankKey(classification, u.getRank(), null);
      ClassificationUtils.setHigherRank(classification, u.getRank(), null);
    }
  }

  /**
   * Use the backbone matching webservice and make sure the service is working.
   * We continue with retries for up to 12h before we let an IllegalStateException bring down the entire normalization.
   * The resulting usageKey of the match and potential issues (e.g. fuzzy matching) will be stored in the neo node.
   */
  private void matchToNub(NameUsageNode nn, LinneanClassification usage, String name, Rank rank) {
    NameUsageMatch match = null;
    final long started = System.currentTimeMillis();
    boolean first = true;
    while (match == null) {
      try {
        match = matchingService.match(name, rank, usage, true, false);
        if (first && warnSlowMatching && System.currentTimeMillis() - started > 100) {
          LOG.warn("Nub matching for {} took {}ms", name, System.currentTimeMillis() - started);
          // log this only once per reporting batch!
          warnSlowMatching = false;
        }
      } catch (UniformInterfaceException e) {
        // in case of a 404 allow a retry for 30 minutes in case the service needs to be starting up still
        if (e.getResponse().getStatus() == 404) {
          try {
            // first time we retry after 5s, then after every minute
            if (first) {
              LOG.warn("Nub matching for >{}< failed with 404. Retry ever minute", name, e);
              Thread.sleep(TimeUnit.SECONDS.toMillis(5));
              first = false;
            } else {
              // check if we tried for a long time already
              long sinceMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - started);
              if (sinceMinutes > 30) {
                throw new IllegalStateException("Backbone matching service unavailable for at least 30 minutes. Interrupting normalization!");
              }
              Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            }
          } catch (InterruptedException e1) {
          }
        } else {
          LOG.error("Species matching for >>{}<< failed due to http error {}. Continue without match", name, e.getResponse().getStatus(), e);
          match = new NameUsageMatch();
        }

      } catch (Exception e) {
        LOG.error("Species matching for {} failed due to jersey client error. Continue without match", name, e);
        match = new NameUsageMatch();
      }
    }

    // store nub key
    nn.usage.setNubKey(match.getUsageKey());
    nn.modified = true;
    if (match.getUsageKey() == null) {
      LOG.debug("Failed nub match: {}", name);
      nn.addIssue(NameUsageIssue.BACKBONE_MATCH_NONE);

    } else if (match.getMatchType() == NameUsageMatch.MatchType.FUZZY) {
      nn.addIssue(NameUsageIssue.BACKBONE_MATCH_FUZZY);
    }
    dao.store(nn, false);
  }

  /**
   * Process all synonymsTD doing a nub lookup for each of them
   * @return the number of processed synonymsTD
   */
  private void processSynonyms(NameUsageNode nn) {
    for (Node syn : Traversals.SYNONYMS.traverse(nn.node).nodes()) {
      NameUsage s = dao.readUsage(syn, false);
      matchToNub(new NameUsageNode(syn, s, false), classification, s.getScientificName(), nn.usage.getRank());
    }
  }

}
