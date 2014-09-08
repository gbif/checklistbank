package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.NeoRunnable;
import org.gbif.checklistbank.cli.common.RankedName;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.gbif.checklistbank.neo.traverse.TaxonWalker;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.api.model.crawler.NormalizerStats;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.jvm.MemoryUsageGaugeSet;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;
import org.parboiled.common.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a good id based dwc archive and produces a neo4j graph from it.
 */
public class Normalizer extends NeoRunnable {

  private static final Logger LOG = LoggerFactory.getLogger(Normalizer.class);
  private static final List<Splitter> COMMON_SPLITTER = Lists.newArrayList();
  private static final Set<Rank> UNKNOWN_RANKS = ImmutableSet.of(Rank.UNRANKED, Rank.INFORMAL);
  private static final List<String> CLASSIFICATION_PROPERTIES = ImmutableList.of(
    NeoMapper.propertyName(DwcTerm.parentNameUsageID),
    NeoMapper.propertyName(DwcTerm.parentNameUsage),
    DwcTerm.kingdom.simpleName(),
    DwcTerm.phylum.simpleName(),
    DwcTerm.class_.simpleName(),
    DwcTerm.order.simpleName(),
    DwcTerm.family.simpleName(),
    DwcTerm.genus.simpleName(),
    DwcTerm.subgenus.simpleName()
  );

  static {
    for (char del : "[|;, ]".toCharArray()) {
      COMMON_SPLITTER.add(Splitter.on(del).trimResults().omitEmptyStrings());
    }
  }

  private final Map<String, UUID> constituents;
  private final File dwca;
  private final File storeDir;
  private final Meter insertMeter;
  private final Meter relationMeter;
  private final Meter metricsMeter;

  private InsertMetadata meta;
  private NormalizerStats stats;
  private int roots;
  private int depth;
  private int synonyms;
  private Map<Origin, Integer> countByOrigin = Maps.newHashMap();
  private Map<Rank, Integer> countByRank = Maps.newHashMap();
  private List<String> cycles = Lists.newArrayList();

  public Normalizer(NormalizerConfiguration cfg, UUID datasetKey, MetricRegistry registry,
    Map<String, UUID> constituents) {
    super(datasetKey, cfg.neo, registry);
    this.constituents = constituents;
    this.insertMeter = registry.getMeters().get(NormalizerService.INSERT_METER);
    this.relationMeter = registry.getMeters().get(NormalizerService.RELATION_METER);
    this.metricsMeter = registry.getMeters().get(NormalizerService.METRICS_METER);
    storeDir = cfg.neo.neoDir(datasetKey);
    dwca = cfg.archiveDir(datasetKey);
  }

  /**
   * Uses an internal metrics registry to setup the normalizer
   */
  public static Normalizer build(NormalizerConfiguration cfg, UUID datasetKey, Map<String, UUID> constituents) {
    MetricRegistry registry = new MetricRegistry("normalizer");
    MemoryUsageGaugeSet mgs = new MemoryUsageGaugeSet();
    registry.registerAll(mgs);

    registry.meter(NormalizerService.INSERT_METER);
    registry.meter(NormalizerService.RELATION_METER);
    registry.meter(NormalizerService.METRICS_METER);

    return new Normalizer(cfg, datasetKey, registry, constituents);
  }

  public void run() throws NormalizationFailedException {
    LOG.info("Start normalization of checklist {}", datasetKey);
    // batch import uses its own batchdb
    batchInsertData();
    // create regular neo db for further processing
    setupDb();
    setupIndices();
    setupRelations();
    buildMetrics();
    tearDownDb();

    LOG.info("Normalization of {} finished. Database shut down.", datasetKey);
    countByOrigin.put(Origin.SOURCE, meta.getRecords());
    stats = new NormalizerStats(roots, depth, synonyms, countByOrigin, countByRank, cycles);
  }

  public NormalizerStats getStats() {
    return stats;
  }

  private void batchInsertData() throws NormalizationFailedException {
    NeoInserter inserter = new NeoInserter();
    meta = inserter.insert(storeDir, dwca, batchSize, insertMeter, constituents);
  }

  /**
   * Creates implicit nodes and sets up relations between taxa.
   */
  private void setupRelations() {
    LOG.debug("Start processing relations ...");
    int counter = 0;

    Transaction tx = db.beginTx();
    try {
      //This iterates over ALL NODES, even the ones creating within this loop!
      // iteration is by node id starting from node id 1 to highest.
      // if nodes are created within this loop they receive the highest node id and thus are added to the end of this loop
      for (Node n : GlobalGraphOperations.at(db).getAllNodes()) {
        if (counter % batchSize == 0) {
          tx.success();
          tx.close();
          LOG.debug("Relations processed for taxa: {}", counter);
          //logMemory();
          tx = db.beginTx();
        }

        final String taxonID = (String) n.getProperty(TaxonProperties.TAXON_ID, "");
        final String canonical = (String) n.getProperty(TaxonProperties.CANONICAL_NAME, "");
        final String sciname = (String) n.getProperty(TaxonProperties.SCIENTIFIC_NAME, "");
        final Rank rank = mapper.readEnum(n, TaxonProperties.RANK, Rank.class, Rank.UNRANKED);
        boolean isSynonym = setupAcceptedRel(n, taxonID, sciname, canonical, rank);
        setupParentRel(n, isSynonym, taxonID, sciname, canonical);
        setupBasionymRel(n, taxonID, sciname, canonical);

        counter++;
        if (isSynonym) {
          synonyms++;
        }
        relationMeter.mark();
      }
      tx.success();

    } finally {
      tx.close();
    }

    // now process the denormalized classifications
    applyDenormedClassification();

    // finally resolve cycles and other bad relations
    cleanupRelations();

    LOG.info("Relation setup completed, {} nodes processed", counter);
    LOG.info("Relation setup rate: {}", relationMeter.getMeanRate());
  }

  /**
   * Applies the classificaiton given as denormalized higher taxa terms
   * after the parent / accepted relations have been applied.
   * It also removes the ROOT label if new parents are assigned.
   * We need to be careful as the classification coming in first via the parentNameUsage(ID) terms
   * is variable and must not always include a rank.
   */
  private void applyDenormedClassification() {
    LOG.debug("Start processing higher classification ...");
    if (!meta.isDenormedClassificationMapped()) {
      LOG.info("No higher classification mapped");
      return;
    }

    int counter = 0;
    Transaction tx = db.beginTx();
    try {
      for (Node n : GlobalGraphOperations.at(db).getAllNodes()) {
        if (counter % batchSize == 0) {
          tx.success();
          tx.close();
          LOG.debug("Higher classifications processed for {} taxa", counter);
          tx = db.beginTx();
        }
        applyDenormedClassification(n);
        counter++;
      }
      tx.success();
    } finally {
      tx.close();
    }
    LOG.info("Classification processing completed, {} nodes processed", counter);
  }

  private void applyDenormedClassification(Node n) {
    RankedName highest = null;
    if (meta.isParentNameMapped()) {
      // verify if we already have a classification, that it ends with a known rank
      highest = getHighestParent(n);
      if (highest.rank == null || UNKNOWN_RANKS.contains(highest.rank)) {
        LOG.debug("Taxon {} already has a classification which ends in an uncomparable rank.");
        mapper.addIssue(n, NameUsageIssue.CLASSIFICATION_NOT_APPLIED);
        return;
      }
    } else {
      // use this node
      highest = mapper.readRankedName(n);
    }

    // convert to list excluding all ranks equal and below highest.rank
    List<RankedName> denormedClassification = mapper.listVerbatimClassification(n, highest.rank);
    if (!denormedClassification.isEmpty()) {
      // exclude first parent if this taxon is rankless and has the same name
      if ((highest.rank == null || highest.rank.isUncomparable()) && highest.name.equals(denormedClassification.get(0).name)) {
        denormedClassification.remove(0);
      }
      updateDenormedClassification(highest.node, denormedClassification);
    }
  }

  private void updateDenormedClassification(Node taxon, List<RankedName> denormedClassification) {
    if (denormedClassification.isEmpty()) return;

    RankedName parent = denormedClassification.remove(0);
    for (Node n : nodesByCanonical(parent.name)) {
      if (matchesClassification(n, denormedClassification)) {
        assignParent(n, taxon);
        return;
      }
    }
    // create higher taxon if not found
    parent.node = createTaxon(Origin.DENORMED_CLASSIFICATION, parent.name, parent.rank, TaxonomicStatus.ACCEPTED);
    // create parent relationship
    assignParent(parent.node, taxon);

    // link further up recursively?
    if (!denormedClassification.isEmpty()) {
      updateDenormedClassification(parent.node, denormedClassification);
    }
  }

  private void assignParent(Node parent, Node child) {
    parent.createRelationshipTo(child, RelType.PARENT_OF);
    child.removeLabel(Labels.ROOT);
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
    LOG.debug("Cleanup relations ...");

    // cut synonym cycles
    try (Transaction tx = db.beginTx()) {
      try {
        while (true) {
          ExecutionResult result =
            engine.execute("MATCH (s:TAXON)-[sr:SYNONYM_OF]->(x)-[:SYNONYM_OF*]->(s) RETURN sr LIMIT 1");
          Relationship sr = (Relationship) IteratorUtil.first(result.columnAs("sr"));

          Node syn = sr.getStartNode();
          cycles.add((String) syn.getProperty(TaxonProperties.TAXON_ID, null));

          Node acc = createTaxon(Origin.MISSING_ACCEPTED, NormalizerConstants.PLACEHOLDER_NAME, null, TaxonomicStatus.DOUBTFUL);
          createSynonymRel(syn, acc, true);
          sr.delete();
        }
      } catch (NoSuchElementException e) {
        // all cycles removed
        tx.success();
      }
    }

    // relink synonym chain to single accepted
    try (Transaction tx = db.beginTx()) {
      boolean more = true;
      while (more) {
        more = false;
        ExecutionResult result = engine.execute("MATCH (s:TAXON)-[sr:SYNONYM_OF*]->(x)-[:SYNONYM_OF]->(t:TAXON) " +
                                                "WHERE NOT (t)-[:SYNONYM_OF]->() " +
                                                "RETURN sr, t LIMIT 1");
        for (Map<String, Object> row : result) {
          more = true;
          Node acc = (Node) row.get("t");
          for (Relationship sr : (Collection<Relationship>) row.get("sr")) {
            Node syn = sr.getStartNode();
            createSynonymRel(syn, acc, true);
            sr.delete();
          }
        }
      }
      tx.success();
    }

    LOG.info("Relations cleaned up, {} cycles detected", cycles.size());
  }

  private void createSynonymRel(Node synonym, Node accepted, boolean moveParentRel) {
    synonym.createRelationshipTo(accepted, RelType.SYNONYM_OF);
    if (moveParentRel && synonym.hasRelationship(RelType.PARENT_OF)) {
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

  private void buildMetrics() {
    ImportTaxonMetricsHandler handler = new ImportTaxonMetricsHandler();
    TaxonWalker.walkAccepted(db, handler, 10000, metricsMeter);
    depth = handler.getMaxDepth();
    // do other final metrics
    try (Transaction tx = db.beginTx()) {
      roots = IteratorUtil.count(GlobalGraphOperations.at(db).getAllNodesWithLabel(Labels.ROOT));
    }
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
   * Must deal with pro parte synonyms, i.e. a single synonym can have multiple accepted taxa!
   *
   * @return true if it is a synonym of some type
   */
  private boolean setupAcceptedRel(Node n, String taxonID, String sciname, String canonical, Rank rank) {
    TaxonomicStatus status = mapper.readEnum(n, TaxonProperties.STATUS, TaxonomicStatus.class, TaxonomicStatus.DOUBTFUL);
    List<Node> accepted = Lists.newArrayList();
    if (meta.isAcceptedNameMapped()) {
      if (NeoMapper.hasProperty(n, DwcTerm.acceptedNameUsageID)) {
        List<String> ids = Lists.newArrayList();
        final String unsplitIds = NeoMapper.value(n, DwcTerm.acceptedNameUsageID);
        if (unsplitIds != null && !unsplitIds.equals(taxonID)) {
          if (meta.getMultiValueDelimiters().containsKey(DwcTerm.acceptedNameUsageID)) {
            ids = meta.getMultiValueDelimiters().get(DwcTerm.acceptedNameUsageID).splitToList(unsplitIds);
          } else {
            // lookup by taxon id to see if this is an existing identifier or if we should try to split it
            Node a = nodeByTaxonId(unsplitIds);
            if (a != null) {
              accepted.add(a);
            } else {
              ids = splitByCommonDelimiters(unsplitIds);
            }
          }
          //setup relation
          for (String id : ids) {
            if (id != null && !id.equals(taxonID)) {
              Node a = nodeByTaxonId(id);
              if (a == null) {
                LOG.debug("acceptedNameUsageID {} not existing", id);
                mapper.addIssue(n, NameUsageIssue.ACCEPTED_NAME_USAGE_ID_INVALID);
                a = createTaxonWithClassificationProps(Origin.MISSING_ACCEPTED, NormalizerConstants.PLACEHOLDER_NAME, null, TaxonomicStatus.DOUBTFUL, n);
              }
              accepted.add(a);
            }
          }
        }

      } else if (NeoMapper.hasProperty(n, DwcTerm.acceptedNameUsage)) {
        final String name = NeoMapper.value(n, DwcTerm.acceptedNameUsage);
        if (name != null && !name.equals(sciname)) {
          Node a = nodeBySciname(name);
          if (a == null && !name.equals(canonical)) {
            a = nodeByCanonical(name);
            if (a == null) {
              // create doubtful verbatim accepted usage
              LOG.debug("acceptedNameUsage {} not existing, materialize it", name);
              a = createTaxonWithClassificationProps(Origin.VERBATIM_ACCEPTED, name, null, TaxonomicStatus.DOUBTFUL, n);
            }
          }
          if (a != null) {
            accepted.add(a);
          }
        }
      }
    }
    // if status is synonym but we aint got no idea of the accepted create an incertae sedis record of same rank
    if (status.isSynonym() && accepted.isEmpty()) {
      accepted.add( createTaxonWithClassificationProps(Origin.MISSING_ACCEPTED, NormalizerConstants.PLACEHOLDER_NAME, rank, TaxonomicStatus.DOUBTFUL, n) );
    }
    // create synonym relations
    if (!accepted.isEmpty()) {
      if (!status.isSynonym()) {
        status = TaxonomicStatus.SYNONYM;
        mapper.storeEnum(n, TaxonProperties.STATUS, status);
      }
      for (Node a : accepted) {
        n.createRelationshipTo(a, RelType.SYNONYM_OF);
        n.addLabel(Labels.SYNONYM);
      }
    }
    return status.isSynonym();
  }

  /**
   * Sets up the parent relations using the parentNameUsage(ID) term values.
   * The denormed, flat classification is used in a next step later.
   */
  private void setupParentRel(Node n, boolean isSynonym, @Nullable String taxonID, String sciname, String canonical) {
    Node parent = null;
    if (NeoMapper.hasProperty(n, DwcTerm.parentNameUsageID)) {
      final String id = NeoMapper.value(n, DwcTerm.parentNameUsageID);
      if (id != null && (taxonID == null || !id.equals(taxonID))) {
        parent = nodeByTaxonId(id);
        if (parent == null) {
          mapper.addIssue(n, NameUsageIssue.PARENT_NAME_USAGE_ID_INVALID);
          LOG.debug("parentNameUsageID {} not existing", id);
        }
      }
    } else if (NeoMapper.hasProperty(n, DwcTerm.parentNameUsage)) {
      final String name = NeoMapper.value(n, DwcTerm.parentNameUsage);
      if (name != null && !name.equals(sciname)) {
        parent = nodeBySciname(name);
        if (parent == null && !name.equals(canonical)) {
          parent = nodeByCanonical(name);
        }
        if (parent == null) {
          LOG.debug("parentNameUsage {} not existing, materialize it", name);
          parent = createTaxon(Origin.VERBATIM_PARENT, name, null, TaxonomicStatus.DOUBTFUL);
        }
      }
    }
    if (parent != null) {
      parent.createRelationshipTo(n, RelType.PARENT_OF);
    } else if (!isSynonym) {
      n.addLabel(Labels.ROOT);
    }
  }

  private void setupBasionymRel(Node n, String taxonID, String sciname, String canonical) {
    if (meta.isOriginalNameMapped()) {
      Node basionym = null;
      if (NeoMapper.hasProperty(n, DwcTerm.originalNameUsageID)) {
        final String id = NeoMapper.value(n, DwcTerm.originalNameUsageID);
        if (id != null && !id.equals(taxonID)) {
          basionym = nodeByTaxonId(id);
          if (basionym == null) {
            mapper.addIssue(n, NameUsageIssue.ORIGINAL_NAME_USAGE_ID_INVALID);
            LOG.debug("originalNameUsageID {} not existing", id);
          }
        }
      } else if (NeoMapper.hasProperty(n, DwcTerm.originalNameUsage)) {
        final String name = NeoMapper.value(n, DwcTerm.originalNameUsage);
        if (name != null && !name.equals(sciname)) {
          basionym = nodeBySciname(name);
          if (basionym == null && !name.equals(canonical)) {
            basionym = nodeByCanonical(name);
          }
          if (basionym == null) {
            LOG.debug("originalNameUsage {} not existing, materialize it", name);
            basionym = createTaxon(Origin.VERBATIM_BASIONYM, name, null, TaxonomicStatus.DOUBTFUL);
          }
        }
      }
      if (basionym != null) {
        basionym.createRelationshipTo(n, RelType.BASIONYM_OF);
      }
    }
  }

  protected Node createTaxon(Origin origin, String sciname, Rank rank, TaxonomicStatus status) {
    incRank(rank);
    incOrigin(origin);
    Node n = super.create(origin, sciname, rank, status);
    n.addLabel(Labels.ROOT);
    return n;
  }

  private Node createTaxonWithClassificationProps(Origin origin, String sciname, Rank rank, TaxonomicStatus status,
    Node classificationSource) {
    Node n = createTaxon(origin, sciname, rank, status);
    // copy props from source
    for (String p : CLASSIFICATION_PROPERTIES) {
      try {
        n.setProperty(p, classificationSource.getProperty(p));
      } catch (NotFoundException e) {
        // ignore
      }
    }
    return n;
  }

  private void incOrigin(Origin origin) {
    if (origin != null) {
      if (!countByOrigin.containsKey(origin)) {
        countByOrigin.put(origin, 1);
      } else {
        countByOrigin.put(origin, countByOrigin.get(origin) + 1);
      }
    }
  }

  private void incRank(Rank rank) {
    if (rank != null) {
      if (!countByRank.containsKey(rank)) {
        countByRank.put(rank, 1);
      } else {
        countByRank.put(rank, countByRank.get(rank) + 1);
      }
    }
  }

}
