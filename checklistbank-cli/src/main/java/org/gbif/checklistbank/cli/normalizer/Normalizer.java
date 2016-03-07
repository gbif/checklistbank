package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.Metrics;
import org.gbif.checklistbank.cli.model.NameUsageNode;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.neo.ImportDb;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoInserter;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.NotUniqueException;
import org.gbif.checklistbank.neo.NotUniqueRuntimeException;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.NubMatchHandler;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.neo.traverse.TreeWalker;
import org.gbif.checklistbank.neo.traverse.UsageMetricsHandler;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupPassThru;
import org.gbif.utils.concurrent.NamedThreadFactory;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import org.apache.commons.lang3.ObjectUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a good id based dwc archive and produces a neo4j graph from it.
 */
public class Normalizer extends ImportDb implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(Normalizer.class);
  private static final List<Splitter> COMMON_SPLITTER = Lists.newArrayList();
  private static final Set<Rank> UNKNOWN_RANKS = ImmutableSet.of(Rank.UNRANKED, Rank.INFORMAL);
  private static final List<Rank> DWC_RANKS_REVERSE = ImmutableList.copyOf(Lists.reverse(Rank.DWC_RANKS));
  private static final NamedThreadFactory THREAD_FACTORY = new NamedThreadFactory("normalizer-parser");

  static {
    for (char del : "[|;, ]".toCharArray()) {
      COMMON_SPLITTER.add(Splitter.on(del).trimResults().omitEmptyStrings());
    }
  }

  private final IdLookup lookup;
  private final ExecutorService exec;

  private final Map<String, UUID> constituents;
  private final File dwca;
  private final Meter relationMeter;
  private final Meter denormedMeter;
  private final Meter metricsMeter;
  private final int batchSize;
  private InsertMetadata meta;
  private int ignored;
  private List<String> cycles = Lists.newArrayList();
  private UsageMetricsHandler metricsHandler;
  private NubMatchHandler matchHandler;

  private Normalizer(UUID datasetKey, UsageDao dao, File dwca, int batchSize,
                     MetricRegistry registry, Map<String, UUID> constituents, IdLookup lookup) {
    super(datasetKey, dao);
    this.constituents = constituents;
    this.relationMeter = registry.meter(Metrics.RELATION_METER);
    this.metricsMeter = registry.meter(Metrics.METRICS_METER);
    this.denormedMeter = registry.meter(Metrics.DENORMED_METER);
    this.dwca = dwca;
    this.lookup = lookup;
    this.batchSize = batchSize;
    exec = Executors.newFixedThreadPool(1, THREAD_FACTORY);
  }


  public static Normalizer create(NormalizerConfiguration cfg, UUID datasetKey, MetricRegistry registry,
                                  Map<String, UUID> constituents, IdLookup lookup) {
    return new Normalizer(datasetKey,
        UsageDao.persistentDao(cfg.neo, datasetKey, false, registry, true),
        cfg.archiveDir(datasetKey),
        cfg.neo.batchSize,
        registry, constituents, lookup);
  }

  /**
   * Creates a dataset specific normalizer with an internal metrics registry and a pass thru nub matcher.
   */
  public static Normalizer create(NormalizerConfiguration cfg, UUID datasetKey) {
    MetricRegistry registry = new MetricRegistry("normalizer");
    return Normalizer.create(cfg, registry, datasetKey);
  }

  /**
   * Creates a dataset specific normalizer with a pass thru nub matcher.
   */
  public static Normalizer create(NormalizerConfiguration cfg, MetricRegistry registry, UUID datasetKey) {
    return Normalizer.create(cfg, datasetKey, registry, Maps.<String, UUID>newHashMap(), new IdLookupPassThru());
  }

  /**
   * Simple wrapper class that lazily loads a name usage from the dao if needed.
   * used for logging just in some cases
   */
  private class LazyUsage {
    private final Node n;
    private NameUsage u;

    private LazyUsage(Node n) {
      this.n = n;
    }

    public NameUsage getUsage() {
      if (u == null) {
        u = dao.readUsage(n, false);
      }
      return u;
    }

    public String scientificName() {
      return getUsage().getScientificName();
    }
  }

  private class NameParsingJob implements Callable<Integer> {
    private final NameParser parser = new NameParser();

    @Override
    public Integer call() throws Exception {
      int counter = 0;
      try (Transaction tx = dao.beginTx()) {
        for (Node n : dao.allNodes()) {
          String sciname = NeoProperties.getScientificName(n);
          ParsedName pn;
          try {
            pn = parser.parse(sciname, NeoProperties.getRank(n, null));
          } catch (UnparsableException e) {
            // allow any name at least as a scientific name string
            pn = new ParsedName();
            pn.setScientificName(sciname);
            pn.setType(e.type);
          }
          dao.store(n.getId(), pn);
          counter++;
        }
      }
      return counter;
    }
  }

  public void run() throws NormalizationFailedException {
    LOG.info("Start normalization of checklist {}", datasetKey);
    try {
      // batch import uses its own batchdb
      batchInsertData();
      // insert regular neo db for further processing
      setupRelations();
      // start name processing job
      Future<Integer> f = parseNames();
      // while parsing match to nub and build metrics
      buildMetricsAndMatchBackbone();
      // now wait for name parsing to finish
      LOG.info("Wait for name parsing to finish");
      Integer parserCount = f.get();
      LOG.info("Finish to parse all {} names", parserCount);
      LOG.info("Normalization succeeded");

    } catch (InterruptedException e) {
      LOG.error("Name parsing thread interrupted.", datasetKey);
      throw new NormalizationFailedException("Name parsing interrupted", e);

    } catch (ExecutionException e) {
      LOG.error("Name parsing failed: {}", datasetKey, e.getMessage());
      throw new NormalizationFailedException("Name parsing failed", e);

    } finally {
      dao.close();
      LOG.info("Neo database shut down");
    }
    ignored = meta.getIgnored();
  }

  private Future<Integer> parseNames() {
    return exec.submit(new NameParsingJob());
  }

  public NormalizerStats getStats() {
    return metricsHandler.getStats(ignored, cycles);
  }

  private void batchInsertData() throws NormalizationFailedException {
    try {
      NeoInserter inserter = dao.createBatchInserter(batchSize);
      try {
        meta = inserter.insert(dwca, constituents);
      } finally {
        // closing the batch inserter open the neo db again for regular access via the DAO
        inserter.close();
      }
    } catch (NotUniqueRuntimeException e) {
      throw new NormalizationFailedException(e.getProperty() + " values not unique: " + e.getKey(), e);
    }
  }

  /**
   * Applies the classificaiton given as denormalized higher taxa terms
   * after the parent / accepted relations have been applied.
   * It also removes the ROOT label if new parents are assigned.
   * We need to be careful as the classification coming in first via the parentNameUsage(ID) terms
   * is variable and must not always include a rank.
   */
  private void applyDenormedClassification() {
    LOG.info("Start processing higher denormalized classification ...");
    if (!meta.isDenormedClassificationMapped()) {
      LOG.info("No higher classification mapped");
      return;
    }

    int counter = 0;
    Transaction tx = dao.getNeo().beginTx();
    try {
      for (Node n : GlobalGraphOperations.at(dao.getNeo()).getAllNodes()) {
        if (counter % batchSize * 10 == 0) {
          tx.success();
          tx.close();
          LOG.info("Higher classifications processed for {} taxa", counter);
          tx = dao.getNeo().beginTx();
        }
        applyClassification(n);
        counter++;
        denormedMeter.mark();
      }
    } finally {
      tx.success();
      tx.close();
    }
    LOG.info("Classification processing completed, {} nodes processed", counter);
  }

  private void applyClassification(Node n) {
    RankedName highest;
    if (meta.isParentNameMapped()) {
      // verify if we already have a classification, that it ends with a known rank
      highest = getHighestParent(n);
      if (highest.node != n && (highest.rank == null || UNKNOWN_RANKS.contains(highest.rank))) {
        LOG.debug("Node {} already has a classification which ends in an uncomparable rank.", n.getId());
        addIssueRemark(n, null, NameUsageIssue.CLASSIFICATION_NOT_APPLIED);
        return;
      }
    } else {
      // use this node
      highest = dao.readRankedName(n);
    }
    // shortcut: exit if highest is already a kingdom, the denormed classification cannot add to it anymore!
    if (highest.rank != null && highest.rank == Rank.KINGDOM) {
      return;
    }
    LinneanClassification lc = dao.readUsage(n, false);
    applyClassification(highest, lc);
  }

  @VisibleForTesting
  protected static Rank getLowestExistingRank(LinneanClassification lc) {
    for (Rank r : DWC_RANKS_REVERSE) {
      if (lc.getHigherRank(r) != null) {
        return r;
      }
    }
    return null;
  }

  private void removeGenusAndBelow(LinneanClassification lc) {
    ClassificationUtils.setHigherRank(lc, Rank.GENUS, null);
    ClassificationUtils.setHigherRank(lc, Rank.SUBGENUS, null);
    ClassificationUtils.setHigherRank(lc, Rank.SPECIES, null);
  }

  private void applyClassification(RankedName taxon, LinneanClassification lc) {
    Node parent = null;
    Rank parentRank = null;
    // exclude lowest rank from classification to be applied if this taxon is rankless and has the same name
    if (taxon.rank == null || taxon.rank.isUncomparable()) {
      Rank lowest = getLowestExistingRank(lc);
      if (lowest != null && lc.getHigherRank(lowest).equalsIgnoreCase(taxon.name)) {
        ClassificationUtils.setHigherRank(lc, lowest, null);
      }
    }
    // ignore same rank from classification if accepted
    if (!taxon.node.hasLabel(Labels.SYNONYM) && taxon.rank != null) {
      ClassificationUtils.setHigherRank(lc, taxon.rank, null);
    }
    // ignore genus and below for synonyms
    // http://dev.gbif.org/issues/browse/POR-2992
    if (taxon.node.hasLabel(Labels.SYNONYM)) {
      removeGenusAndBelow(lc);
    }

    // from kingdom to genus
    for (Rank hr : Rank.DWC_RANKS) {
      if ((taxon.rank == null || !taxon.rank.higherThan(hr)) && lc.getHigherRank(hr) != null) {
        // test for existing usage with that name & rank
        boolean found = false;
        for (Node n : nodesByCanonicalAndRank(lc.getHigherRank(hr), hr)) {
          if (parent == null) {
            // make sure node does also not have a parent
            if (!n.hasRelationship(RelType.PARENT_OF, Direction.INCOMING)) {
              // match!
              parent = n;
              parentRank = hr;
              found = true;
              break;
            }
          } else {
            // verify the parents for the next higher rank are the same
            try {
              Node p = n.getSingleRelationship(RelType.PARENT_OF, Direction.INCOMING).getStartNode();
              Node p2 = Traversals.findParentWithRank(n, parentRank);
              if (p.equals(parent) || (p2 !=null && p2.equals(parent))) {
                parent = n;
                parentRank = hr;
                found = true;
                break;
              }
            } catch (Exception e) {
              // log?
            }
          }
        }
        if (!found) {
          // persistent new higher taxon if not found
          Node lowerParent = create(Origin.DENORMED_CLASSIFICATION, lc.getHigherRank(hr), hr, TaxonomicStatus.ACCEPTED, parent == null).node;
          // insert parent relationship?
          assignParent(parent, lowerParent);
          parent = lowerParent;
          parentRank = hr;
        }
      }
    }
    // finally apply to initial node
    assignParent(parent, taxon.node);
  }

  private void assignParent(Node parent, Node child) {
    if (parent != null) {
      parent.createRelationshipTo(child, RelType.PARENT_OF);
      child.removeLabel(Labels.ROOT);
    }
  }

  /**
   * Sanitizes relations and does the following cleanup:
   * <ul>
   * <li>Relink synonym of synonyms to make sure synonyms always point to a direct accepted taxon.</li>
   * <li>(Re)move parent relationship for synonyms.</li>
   * <li>Break eternal classification loops at lowest rank</li>
   * </ul>
   */
  private void cleanupRelations() {
    LOG.info("Cleanup relations ...");
    // cut synonym cycles
    int counter = 0;
    while (true) {
      try (Transaction tx = dao.getNeo().beginTx()) {
        Result result = dao.getNeo().execute("MATCH (s:TAXON)-[sr:SYNONYM_OF]->(x)-[:SYNONYM_OF*]->(s) RETURN sr LIMIT 1");
        if (result.hasNext()) {
          Relationship sr = (Relationship) result.next().get("sr");

          Node syn = sr.getStartNode();

          NameUsage su = dao.readUsage(syn, false);
          su.addIssue(NameUsageIssue.CHAINED_SYNOYM);
          su.addIssue(NameUsageIssue.PARENT_CYCLE);
          dao.store(syn.getId(), su, false);

          String taxonID = (String) syn.getProperty(NeoProperties.TAXON_ID, null);
          cycles.add(taxonID);

          NameUsageNode acc = create(Origin.MISSING_ACCEPTED, NormalizerConstants.PLACEHOLDER_NAME, null, TaxonomicStatus.DOUBTFUL, true, null, "Synonym cycle cut for taxonID " + taxonID);
          createSynonymRel(syn, acc.node);
          sr.delete();
          tx.success();

          if (counter++ % 100 == 0) {
            LOG.debug("Synonym cycles cut so far: {}", counter);
          }
        } else {
          break;
        }
      }
    }

    // relink synonym chain to single accepted
    int chainedSynonyms = 0;
    while (true) {
      try (Transaction tx = dao.getNeo().beginTx()) {
        Result result = dao.getNeo().execute("MATCH (s:TAXON)-[sr:SYNONYM_OF*]->(x)-[:SYNONYM_OF]->(t:TAXON) " +
            "WHERE NOT (t)-[:SYNONYM_OF]->() " +
            "RETURN sr, t LIMIT 1");
        if (result.hasNext()) {
          Map<String, Object> row = result.next();
          Node acc = (Node) row.get("t");
          for (Relationship sr : (Collection<Relationship>) row.get("sr")) {
            Node syn = sr.getStartNode();
            addIssueRemark(syn, null, NameUsageIssue.CHAINED_SYNOYM);
            createSynonymRel(syn, acc);
            sr.delete();
            chainedSynonyms++;
          }
          tx.success();
          if (counter++ % 100 == 0) {
            LOG.debug("Synonym chain cut so far: {}", counter);
          }

        } else {
          break;
        }
      }
    }


    // removes parent relations for synonyms
    // if synonyms are parents of other taxa relinks relationship to the accepted
    // presence of both confuses subsequent imports, see http://dev.gbif.org/issues/browse/POR-2755
    int parentOfRelDeleted = 0;
    int parentOfRelRelinked = 0;
    int childOfRelDeleted = 0;
    int childOfRelRelinkedToAccepted = 0;
    try (Transaction tx = dao.getNeo().beginTx()) {
      for (Node syn : IteratorUtil.loop(dao.allSynonyms())) {
        Node accepted = syn.getSingleRelationship(RelType.SYNONYM_OF, Direction.OUTGOING).getEndNode();
        LazyUsage synU = new LazyUsage(syn);
        LazyUsage accU = new LazyUsage(accepted);
        // if the synonym is a parent of another child taxon - relink accepted as parent of child
        for (Relationship rel : syn.getRelationships(RelType.PARENT_OF, Direction.OUTGOING)) {
          Node child = rel.getOtherNode(syn);
          if (child.equals(accepted)) {
            // accepted is also the parent. Delete parent rel in this case
            rel.delete();
            parentOfRelDeleted++;
          } else {
            rel.delete();
            accepted.createRelationshipTo(child, RelType.PARENT_OF);
            parentOfRelRelinked++;
            addIssueRemark(child, "Parent relation taken from synonym " + synU.scientificName());
          }
        }
        // remove parent rel for synonyms
        for (Relationship rel : syn.getRelationships(RelType.PARENT_OF, Direction.INCOMING)) {
          // before we delete the relation make sure the accepted does have a parent rel or is ROOT
          if (accepted.hasRelationship(RelType.PARENT_OF, Direction.INCOMING)) {
            // delete
            childOfRelDeleted++;
            rel.delete();
          } else {
            Node parent = rel.getOtherNode(syn);
            // relink if parent is not the accepted and parent rank is higher than accepted or null
            if (!parent.equals(accepted)) {
              NameUsage parentU = dao.readUsage(parent, false);
              if (parentU.getRank() == null ||
                  (accU.getUsage().getRank() != null && parentU.getRank().higherThan(accU.getUsage().getRank()))) {
                LOG.debug("Relink parent rel of synonym {}", synU.scientificName());
                childOfRelRelinkedToAccepted++;
                parent.createRelationshipTo(accepted, RelType.PARENT_OF);
                addIssueRemark(accepted, "Parent relation taken from synonym " + synU.scientificName());
              }
            }
            rel.delete();
          }
        }
      }
      tx.success();
    }

    LOG.info("Relations cleaned up, {} synonym cycles detected, {} chained synonyms relinked", cycles.size(), chainedSynonyms);
    LOG.info("Synonym relations cleaned up. "
            + "{} childOf relations deleted, {} childOf rels relinked to accepted,"
            + "{} parentOf relations deleted, {} parentOf rels moved from synonym to accepted",
        childOfRelDeleted, childOfRelRelinkedToAccepted, parentOfRelDeleted, parentOfRelRelinked);
  }

  /**
   * Reads a name usage from the kvp store, adds issues and or remarks and persists it again.
   * Only use this method if you just have a node a no usage instance yet at hand.
   */
  private NameUsageNode addIssueRemark(Node n, @Nullable String remark, NameUsageIssue... issues) {
    NameUsageNode nn = new NameUsageNode(n, dao.readUsage(n, false), true);
    nn.addIssue(issues);
    if (remark != null) {
      nn.addRemark(remark);
    }
    dao.store(nn, false);
    return nn;
  }

  /**
   * Creates a synonym relationship between the given synonym and the accepted node, updating labels accordingly
   * and also moving potentially existing parent_of relations.
   */
  private void createSynonymRel(Node synonym, Node accepted) {
    synonym.createRelationshipTo(accepted, RelType.SYNONYM_OF);
    if (synonym.hasRelationship(RelType.PARENT_OF)) {
      try {
        Relationship rel = synonym.getSingleRelationship(RelType.PARENT_OF, Direction.INCOMING);
        if (rel != null) {
          // check if accepted has a parent relation already
          if (!accepted.hasRelationship(RelType.PARENT_OF, Direction.INCOMING)) {
            rel.getStartNode().createRelationshipTo(accepted, RelType.PARENT_OF);
            accepted.removeLabel(Labels.ROOT);
          }
        }
      } catch (RuntimeException e) {
        // more than one parent relationship exists, should never be the case, sth wrong!
        LOG.warn("Synonym {} has multiple parent relationships. Deleting them all!", synonym.getId());
        //for (Relationship r : synonym.getRelationships(RelType.PARENT_OF)) {
        //  r.delete();
        //}
      }
    }
  }

  /**
   * Matches every node to the backbone and calculates a usage metric.
   * This is done jointly as both needs the full linnean classification for every node.
   */
  private void buildMetricsAndMatchBackbone() {
    LOG.info("Walk all accepted taxa, build metrics and match to the GBIF backbone");
    metricsHandler = new UsageMetricsHandler(dao);
    matchHandler = new NubMatchHandler(lookup, dao);
    TreeWalker.walkAcceptedTree(dao.getNeo(), null, null, metricsMeter, metricsHandler, matchHandler);
    LOG.info("Walked all accepted taxa and built metrics");
  }

  /**
   * @return if splittable 2 ore more values, otherwise the original value alone unless its an empty string
   */
  @VisibleForTesting
  protected static List<String> splitByCommonDelimiters(String val) {
    if (Strings.isNullOrEmpty(val)) {
      return Lists.newArrayList();
    }
    for (Splitter splitter : COMMON_SPLITTER) {
      List<String> vals = splitter.splitToList(val);
      if (vals.size() > 1) {
        return vals;
      }
    }
    return Lists.newArrayList(val);
  }

  /**
   * Checks if this node is a pro parte synonym by looking if multiple accepted taxa are referred to.
   * If so, new taxon nodes are created each with a single, unique acceptedNameUsageID property.
   */
  private List<String> parseAcceptedIDs(NameUsageNode nn, @Nullable VerbatimNameUsage v) {
    List<String> acceptedIds = Lists.newArrayList();
    final String unsplitIds = v.getCoreField(DwcTerm.acceptedNameUsageID);
    if (unsplitIds != null) {
      if (unsplitIds.equals(nn.usage.getTaxonID())) {
        acceptedIds.add(unsplitIds);
      } else {
        if (meta.getMultiValueDelimiters().containsKey(DwcTerm.acceptedNameUsageID)) {
          acceptedIds = meta.getMultiValueDelimiters().get(DwcTerm.acceptedNameUsageID).splitToList(unsplitIds);
        } else {
          // lookup by taxon id to see if this is an existing identifier or if we should try to split it
          Node a = nodeByTaxonId(unsplitIds);
          if (a != null) {
            acceptedIds.add(unsplitIds);
          } else {
            acceptedIds = splitByCommonDelimiters(unsplitIds);
          }
        }
      }
    }
    return acceptedIds;
  }


  private Transaction renewTx(Transaction tx) {
    tx.success();
    tx.close();
    return dao.getNeo().beginTx();
  }

  /**
   * Creates implicit nodes and sets up relations between taxa.
   */
  private void setupRelations() {
    LOG.info("Start processing explicit relations ...");
    int counter = 0;

    Transaction tx = dao.getNeo().beginTx();
    try {
      // This iterates over ALL NODES, even the ones created within this loop which trigger a transaction commit!
      // iteration is by node id starting from node id 1 to highest.
      // if nodes are created within this loop they receive the highest node id and thus are added to the end of this loop
      for (Node n : GlobalGraphOperations.at(dao.getNeo()).getAllNodes()) {
        setupRelation(n);
        counter++;
        relationMeter.mark();
        tx = renewTx(tx);
        if (counter % 10000 == 0) {
          LOG.debug("Processed relations for {} nodes", counter);
        }
      }

    } finally {
      tx.success();
      tx.close();
    }

    // now process the denormalized classifications
    applyDenormedClassification();

    // finally resolve cycles and other bad relations
    cleanupRelations();

    LOG.info("Relation setup completed, {} nodes processed. Setup rate: {}", counter, relationMeter.getMeanRate());
  }

  private void setupRelation(Node n) {
    final NameUsageNode nn = new NameUsageNode(n, dao.readUsage(n, false), true);
    final VerbatimNameUsage v = dao.readVerbatim(n.getId());
    setupAcceptedRel(nn, v);
    setupParentRel(nn, v);
    setupBasionymRel(nn, v);
    dao.store(nn, false);
  }

  /**
   * Creates synonym_of relationship based on the verbatim dwc:acceptedNameUsageID and dwc:acceptedNameUsage term values.
   * Assumes pro parte synonyms are dealt with before and the remaining accepted identifier refers to a single taxon only.
   *
   * @param nn the usage to process
   */
  private void setupAcceptedRel(NameUsageNode nn, @Nullable VerbatimNameUsage v) {
    Node accepted = null;
    if (v != null && meta.isAcceptedNameMapped()) {
      List<String> acceptedIds = parseAcceptedIDs(nn, v);
      if (!acceptedIds.isEmpty()) {
        String id = acceptedIds.get(0);
        // make sure it is not an accepted taxon pointing to itself
        if (!id.equals(nn.usage.getTaxonID())) {
          accepted = nodeByTaxonId(id);
          if (accepted == null) {
            nn.addIssue(NameUsageIssue.ACCEPTED_NAME_USAGE_ID_INVALID);
            LOG.debug("acceptedNameUsageID {} not existing", id);
            // is the accepted name also mapped?
            String name = ObjectUtils.firstNonNull(v.getCoreField(DwcTerm.acceptedNameUsage), NormalizerConstants.PLACEHOLDER_NAME);
            accepted = createTaxonWithClassification(Origin.MISSING_ACCEPTED, name, nn.usage.getRank(), TaxonomicStatus.DOUBTFUL, nn, id,
                "Placeholder for the missing accepted taxonID for synonym " + nn.usage.getScientificName(), v);
          }
          // persistent proparte rels if needed
          Iterator<String> additionalIds = acceptedIds.listIterator(1);
          while (additionalIds.hasNext()) {
            final String id2 = additionalIds.next();
            Node accepted2 = nodeByTaxonId(id2);
            if (accepted2 == null) {
              nn.addIssue(NameUsageIssue.ACCEPTED_NAME_USAGE_ID_INVALID);
              LOG.debug("acceptedNameUsageID {} not existing", id2);
            } else {
              nn.node.createRelationshipTo(accepted2, RelType.PROPARTE_SYNONYM_OF);
            }
          }
        }

      } else {
        final String name = v.getCoreField(DwcTerm.acceptedNameUsage);
        if (name != null && !name.equals(nn.usage.getScientificName())) {
          try {
            accepted = nodeBySciname(name);
            if (accepted == null && !name.equals(nn.usage.getCanonicalName())) {
              accepted = nodeByCanonical(name);
              if (accepted == null) {
                LOG.debug("acceptedNameUsage {} not existing, materialize it", name);
                accepted = createTaxonWithClassification(Origin.VERBATIM_ACCEPTED, name, null, TaxonomicStatus.DOUBTFUL, nn, null, null, v);
              }
            }
          } catch (NotUniqueException e) {
            nn.addIssue(NameUsageIssue.ACCEPTED_NAME_NOT_UNIQUE);
            LOG.warn("acceptedNameUsage {} not unique, duplicate accepted name for synonym {} and taxonID {}", name, nn.usage.getScientificName(), nn.usage.getTaxonID());
            accepted = createTaxonWithClassification(Origin.VERBATIM_ACCEPTED, name, null, TaxonomicStatus.DOUBTFUL, nn, null, null, v);
          }
        }
      }
    }

    // if status is synonym but we aint got no idea of the accepted insert an incertae sedis record of same rank
    if (nn.usage.isSynonym() && accepted == null) {
      nn.addIssue(NameUsageIssue.ACCEPTED_NAME_MISSING);
      accepted = createTaxonWithClassification(Origin.MISSING_ACCEPTED, NormalizerConstants.PLACEHOLDER_NAME, nn.usage.getRank(), TaxonomicStatus.DOUBTFUL, nn, null,
          "Placeholder for the missing accepted taxon for synonym " + nn.usage.getScientificName(), v);
    }

    if (accepted != null && !accepted.equals(nn.node)) {
      // make sure taxonomic status reflects the synonym relation
      if (!nn.usage.isSynonym()) {
        nn.usage.setSynonym(true);
        nn.usage.setTaxonomicStatus(TaxonomicStatus.SYNONYM);
      }
      nn.node.createRelationshipTo(accepted, RelType.SYNONYM_OF);
      nn.node.addLabel(Labels.SYNONYM);
    }
  }

  /**
   * Sets up the parent relations using the parentNameUsage(ID) term values.
   * The denormed, flat classification is used in a next step later.
   */
  private void setupParentRel(NameUsageNode nn, @Nullable VerbatimNameUsage v) {
    Node parent = null;
    if (v != null) {
      final String id = v.getCoreField(DwcTerm.parentNameUsageID);
      if (id != null) {
        if ((nn.usage.getTaxonID() == null || !id.equals(nn.usage.getTaxonID()))) {
          parent = nodeByTaxonId(id);
          if (parent == null) {
            nn.addIssue(NameUsageIssue.PARENT_NAME_USAGE_ID_INVALID);
            LOG.debug("parentNameUsageID {} not existing", id);
          }
        }
      } else {
        final String name = v.getCoreField(DwcTerm.parentNameUsage);
        if (name != null && !name.equals(nn.usage.getScientificName())) {
          try {
            parent = nodeBySciname(name);
            if (parent == null && !name.equals(nn.usage.getCanonicalName())) {
              parent = nodeByCanonical(name);
            }
            if (parent == null) {
              LOG.debug("parentNameUsage {} not existing, materialize it", name);
              parent = create(Origin.VERBATIM_PARENT, name, null, TaxonomicStatus.DOUBTFUL, true).node;
            }
          } catch (NotUniqueException e) {
            nn.addIssue(NameUsageIssue.PARENT_NAME_NOT_UNIQUE);
            LOG.warn("parentNameUsage {} not unique, ignore relationship for name {} and taxonID {}", name, nn.usage.getScientificName(), nn.usage.getTaxonID());
            parent = create(Origin.VERBATIM_PARENT, name, null, TaxonomicStatus.DOUBTFUL, true).node;
          }
        }
      }
    }
    if (parent != null && !parent.equals(nn.node)) {
      parent.createRelationshipTo(nn.node, RelType.PARENT_OF);
    } else if (!nn.usage.isSynonym()) {
      nn.node.addLabel(Labels.ROOT);
    }
  }

  private void setupBasionymRel(NameUsageNode nn, @Nullable VerbatimNameUsage v) {
    if (meta.isOriginalNameMapped() && v != null) {
      Node basionym = null;
      final String id = v.getCoreField(DwcTerm.originalNameUsageID);
      if (id != null) {
        if (!id.equals(nn.usage.getTaxonID())) {
          basionym = nodeByTaxonId(id);
          if (basionym == null) {
            nn.addIssue(NameUsageIssue.ORIGINAL_NAME_USAGE_ID_INVALID);
            LOG.debug("originalNameUsageID {} not existing", id);
          }
        }
      } else {
        final String name = v.getCoreField(DwcTerm.originalNameUsage);
        if (name != null && !name.equals(nn.usage.getScientificName())) {
          try {
            basionym = nodeBySciname(name);
            if (basionym == null && !name.equals(nn.usage.getCanonicalName())) {
              basionym = nodeByCanonical(name);
            }
            if (basionym == null) {
              LOG.debug("originalNameUsage {} not existing, materialize it", name);
              basionym = create(Origin.VERBATIM_BASIONYM, name, null, TaxonomicStatus.DOUBTFUL, true).node;
            }
          } catch (NotUniqueException e) {
            nn.addIssue(NameUsageIssue.ORIGINAL_NAME_NOT_UNIQUE);
            LOG.warn("originalNameUsage {} not unique, ignore relationship for taxonID {}", nn.usage.getScientificName(), nn.usage.getTaxonID());
          }
        }
      }
      if (basionym != null && !basionym.equals(nn.node)) {
        basionym.createRelationshipTo(nn.node, RelType.BASIONYM_OF);
      }
    }
  }

  /**
   * Creates a new taxon in neo and the name usage kvp using the source usages as a template for the classification properties.
   * Only copies the classification above genus and ignores genus and below!
   * A verbatim usage is created with just the parentNameUsage(ID) values so they can get resolved into proper neo relations later.
   *
   * @param taxonID the optional taxonID to apply to the new node
   */
  private Node createTaxonWithClassification(Origin origin, String sciname, Rank rank, TaxonomicStatus status, NameUsageNode source,
                                             @Nullable String taxonID, @Nullable String remarks, VerbatimNameUsage sourceVerbatim) {
    NameUsage u = new NameUsage();
    u.setScientificName(sciname);
    u.setCanonicalName(sciname);
    u.setRank(rank);
    u.setOrigin(origin);
    u.setTaxonomicStatus(status);
    u.setTaxonID(taxonID);
    u.setRemarks(remarks);
    // copy verbatim classification from source
    ClassificationUtils.copyLinneanClassification(source.usage, u);
    removeGenusAndBelow(u);
    Node n = create(u, false).node;
    // copy parent props from source
    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.parentNameUsageID, sourceVerbatim.getCoreField(DwcTerm.parentNameUsageID));
    v.setCoreField(DwcTerm.parentNameUsage, sourceVerbatim.getCoreField(DwcTerm.parentNameUsage));
    dao.store(n.getId(), v);
    return n;
  }

}
