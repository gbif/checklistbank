package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.checklistbank.cli.model.NameUsageNode;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.NubDb;
import org.gbif.checklistbank.utils.NameFormatter;
import org.gbif.common.parsers.KingdomParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.LookupUsage;

import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assigns existing usageKey through a backbone match.
 */
public class NubMatchHandler implements StartEndHandler {

  private static final Logger LOG = LoggerFactory.getLogger(NubMatchHandler.class);
  private final Set<NameType> unparsableMatchTypes = Sets.newHashSet(NameType.VIRUS, NameType.HYBRID, NameType.CULTIVAR, NameType.CANDIDATUS);
  // neo node ids for the higher classification links
  private final IdLookup lookup;
  private final UsageDao dao;
  private int counter;
  private Kingdom currKingdom;
  private long currKingdomNodeId = -1;
  private KingdomParser kParser = KingdomParser.getInstance();

  public NubMatchHandler(IdLookup lookup, UsageDao dao) {
    this.dao = dao;
    this.lookup = Preconditions.checkNotNull(lookup, "Backbone matching client required");
  }

  @Override
  public void start(Node n) {
    // increase counters
    counter++;
    if (counter % 1000 == 0) {
      LOG.debug("Nub matching done for: {}", counter);
    }
    NameUsage u = dao.readUsage(n, false);
    if (currKingdom == null) {
      ParseResult<Kingdom> k = kParser.parse(u.getCanonicalName());
      if (k.isSuccessful()) {
        setCurrKingdom(k.getPayload(), n);
      }
    }
  }

  @Override
  public void end(Node n) {
    NameUsage u = dao.readUsage(n, false);
    // nub lookup
    NameUsageNode nn = new NameUsageNode(n, u, false);
    matchToNub(nn);
    processSynonyms(nn);
    // remove kingdom?
    if (currKingdomNodeId == n.getId()) {
      currKingdom = null;
      currKingdomNodeId = -1;
    }
  }

  /**
   * Use an in memory backbone matching so there is no need for retries.
   * The resulting usageKey of the match and potential issues will be stored in the neo node.
   */
  private void matchToNub(NameUsageNode nn) {
    ParsedName pn = dao.readName(nn.node.getId());
    if (pn == null) {
      LOG.warn("No parsed name found for {} {}", nn.node, nn.usage.getScientificName());
    }
    LookupUsage match;
    if (pn == null || unparsableMatchTypes.contains(pn.getType())) {
      // try with full scientific name for certain name types (we dont want to match informal or no names)
      match = lookup.match(nn.usage.getScientificName(), null, null, nn.usage.getRank(), nn.usage.getTaxonomicStatus(), currKingdom);
    } else {
      match = lookup.match(NameFormatter.canonicalOrScientificName(pn), pn.getAuthorship(), pn.getYear(), nn.usage.getRank(), nn.usage.getTaxonomicStatus(), currKingdom);
    }

    // store nub key
    if (match != null) {
      nn.usage.setNubKey(match.getKey());
      if (currKingdom == null) {
        setCurrKingdom(match.getKingdom(), nn.node);
        LOG.debug("Nub match {} complementing kingdom: {}", nn.usage.getScientificName(), currKingdom);
      }
    } else {
      LOG.debug("Failed nub match: {} {}  -  TaxonID={}", nn.usage.getRank(), nn.usage.getScientificName(), nn.usage.getTaxonID());
      nn.usage.setNubKey(null);
      nn.addIssue(NameUsageIssue.BACKBONE_MATCH_NONE);
    }
    nn.modified = true;
    dao.store(nn, false);
  }

  private void setCurrKingdom(Kingdom k, Node n) {
    currKingdom = k;
    currKingdomNodeId = n.getId();
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
