package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.neo.NeoRunnable;
import org.gbif.checklistbank.neo.NotUniqueException;
import org.gbif.checklistbank.neo.RankedName;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.gbif.checklistbank.neo.traverse.TaxonWalker;
import org.gbif.dwc.terms.DwcTerm;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import org.apache.commons.lang3.ObjectUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
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
  private static final String PRO_PARTE_KEY_FIELD = "proParteKey";
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

  private final NameUsageMatchingService matchingService;

  private final Map<String, UUID> constituents;
  private final File dwca;
  private final File storeDir;
  private final Meter insertMeter;
  private final Meter relationMeter;
  private final Meter denormedMeter;
  private final Meter metricsMeter;

  private InsertMetadata meta;
  private int ignored;
  private List<String> cycles = Lists.newArrayList();
  private UsageMetricsAndNubMatchHandler metricsHandler;

  public Normalizer(NormalizerConfiguration cfg, UUID datasetKey, MetricRegistry registry,
    Map<String, UUID> constituents, NameUsageMatchingService matchingService) {
    super(datasetKey, cfg.neo, registry);
    this.constituents = constituents;
    this.insertMeter = registry.getMeters().get(NormalizerService.INSERT_METER);
    this.relationMeter = registry.getMeters().get(NormalizerService.RELATION_METER);
    this.metricsMeter = registry.getMeters().get(NormalizerService.METRICS_METER);
    this.denormedMeter = registry.getMeters().get(NormalizerService.DENORMED_METER);
    storeDir = cfg.neo.neoDir(datasetKey);
    dwca = cfg.archiveDir(datasetKey);
    this.matchingService = matchingService;
  }

  public void run() throws NormalizationFailedException {
    LOG.info("Start normalization of checklist {}", datasetKey);
    // batch import uses its own batchdb
    batchInsertData();
    // insert regular neo db for further processing
    setupDb();
    setupRelations();
    buildMetricsAndMatchBackbone();
    tearDownDb();

    LOG.info("Normalization of {} finished. Database shut down.", datasetKey);
    ignored = meta.getIgnored();
  }

  public NormalizerStats getStats() {
    return metricsHandler.getStats(ignored, cycles);
  }

  private void batchInsertData() throws NormalizationFailedException {
    NeoInserter inserter = new NeoInserter(storeDir, batchSize, insertMeter);
    meta = inserter.insert(dwca, constituents);
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
    Transaction tx = db.beginTx();
    try {
      for (Node n : GlobalGraphOperations.at(db).getAllNodes()) {
        if (counter % batchSize == 0) {
          tx.success();
          tx.close();
          LOG.info("Higher classifications processed for {} taxa", counter);
          tx = db.beginTx();
        }
        applyDenormedClassification(n);
        counter++;
        denormedMeter.mark();
        tx.success();
      }
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
      if (highest.node != n && highest.rank == null || UNKNOWN_RANKS.contains(highest.rank)) {
        LOG.debug("Node {} already has a classification which ends in an uncomparable rank.", n.getId());
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
    // insert higher taxon if not found
    parent.node = createTaxon(Origin.DENORMED_CLASSIFICATION, parent.name, parent.rank, TaxonomicStatus.ACCEPTED, true);
    // insert parent relationship
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
    while (true) {
      try (Transaction tx = db.beginTx()) {
        Result result = db.execute("MATCH (s:TAXON)-[sr:SYNONYM_OF]->(x)-[:SYNONYM_OF*]->(s) RETURN sr LIMIT 1");
        if (result.hasNext()) {
          Relationship sr = (Relationship) result.next().get("sr");

          Node syn = sr.getStartNode();
          mapper.addIssue(syn, NameUsageIssue.CHAINED_SYNOYM);
          mapper.addIssue(syn, NameUsageIssue.PARENT_CYCLE);
          String taxonID = (String) syn.getProperty(TaxonProperties.TAXON_ID, null);
          cycles.add(taxonID);

          Node acc = createTaxon(Origin.MISSING_ACCEPTED, NormalizerConstants.PLACEHOLDER_NAME, null, TaxonomicStatus.DOUBTFUL, true);
          mapper.addRemark(acc, "Synonym cycle cut for taxonID " + taxonID);
          createSynonymRel(syn, acc);
          sr.delete();
          tx.success();

        } else {
          break;
        }
      }
    }

    // relink synonym chain to single accepted
    int chainedSynonyms = 0;
    while (true) {
      try (Transaction tx = db.beginTx()) {
        Result result = db.execute("MATCH (s:TAXON)-[sr:SYNONYM_OF*]->(x)-[:SYNONYM_OF]->(t:TAXON) " +
                                                "WHERE NOT (t)-[:SYNONYM_OF]->() " +
                                                "RETURN sr, t LIMIT 1");
        if (result.hasNext()) {
          Map<String, Object> row = result.next();
          Node acc = (Node) row.get("t");
          for (Relationship sr : (Collection<Relationship>) row.get("sr")) {
            Node syn = sr.getStartNode();
            mapper.addIssue(syn, NameUsageIssue.CHAINED_SYNOYM);
            createSynonymRel(syn, acc);
            sr.delete();
            chainedSynonyms++;
          }
          tx.success();

        } else {
          break;
        }
      }
    }

    LOG.info("Relations cleaned up, {} cycles detected, {} chained synonyms relinked", cycles.size(), chainedSynonyms);
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
    metricsHandler = new UsageMetricsAndNubMatchHandler(matchingService, db);
    TaxonWalker.walkAccepted(db, metricsHandler, 10000, metricsMeter);
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
  private void duplicateProParteSynonyms(Node n, String taxonID) {
    if (meta.isAcceptedNameMapped()) {
      if (NeoMapper.hasProperty(n, DwcTerm.acceptedNameUsageID)) {
        List<String> acceptedIds = Lists.newArrayList();
        final String unsplitIds = NeoMapper.value(n, DwcTerm.acceptedNameUsageID);
        if (unsplitIds != null && !unsplitIds.equals(taxonID)) {
          if (meta.getMultiValueDelimiters().containsKey(DwcTerm.acceptedNameUsageID)) {
            acceptedIds = meta.getMultiValueDelimiters().get(DwcTerm.acceptedNameUsageID).splitToList(unsplitIds);
          } else {
            // lookup by taxon id to see if this is an existing identifier or if we should try to split it
            Node a = nodeByTaxonId(unsplitIds);
            if (a == null) {
              acceptedIds = splitByCommonDelimiters(unsplitIds);
            }
          }
        }
        if (acceptedIds.size() > 1) {
          final int primaryId = (int) n.getId();
          // duplicate this node for each accepted id!
          LOG.info("pro parte synonym found with multiple acceptedNameUsageIDs: {}", acceptedIds);

          // now create new nodes and also update their acceptedId so the relations get processed fine later on
          Iterator<String> accIter = acceptedIds.iterator();
          updateProParteSynonym(n, accIter.next(), primaryId);

          // now create new nodes, update their acceptedId and immediately process the relations
          while (accIter.hasNext()) {
            Node p = createTaxon(n, Origin.PROPARTE);
            updateProParteSynonym(p, accIter.next(), primaryId);
            setupRelation(p);
          }
        }
      }
    }
  }

  private Transaction renewTx (Transaction tx) {
    tx.success();
    tx.close();
    return db.beginTx();
  }

  private void updateProParteSynonym(Node n, String acceptedId, int primarySynonymId) {
    mapper.setProperty(n, DwcTerm.acceptedNameUsageID, acceptedId);
    mapper.storeEnum(n, DwcTerm.taxonomicStatus, TaxonomicStatus.PROPARTE_SYNONYM);
    n.setProperty(PRO_PARTE_KEY_FIELD, primarySynonymId);
  }

  /**
   * Creates implicit nodes and sets up relations between taxa.
   */
  private void setupRelations() {
    LOG.debug("Start processing relations ...");
    int counter = 0;

    Transaction tx = db.beginTx();
    try {
      // This iterates over ALL NODES, even the ones created within this loop which trigger a transaction commit!
      // iteration is by node id starting from node id 1 to highest.
      // if nodes are created within this loop they receive the highest node id and thus are added to the end of this loop
      for (Node n : GlobalGraphOperations.at(db).getAllNodes()) {
        setupRelation(n);

        counter++;
        relationMeter.mark();
        if (counter % batchSize == 0) {
          tx = renewTx(tx);
          LOG.debug("Relations processed for taxa: {}", counter);
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
    final String taxonID = (String) n.getProperty(TaxonProperties.TAXON_ID, "");
    final String canonical = (String) n.getProperty(TaxonProperties.CANONICAL_NAME, "");
    final String sciname = (String) n.getProperty(TaxonProperties.SCIENTIFIC_NAME, "");
    final Rank rank = mapper.readEnum(n, TaxonProperties.RANK, Rank.class, Rank.UNRANKED);
    duplicateProParteSynonyms(n, taxonID);
    boolean isSynonym = setupAcceptedRel(n, taxonID, sciname, canonical, rank);
    setupParentRel(n, isSynonym, taxonID, sciname, canonical);
    setupBasionymRel(n, taxonID, sciname, canonical);
  }

  /**
   * Creates synonym_of relationship.
   * Assumes pro parte synonyms are dealt with before and the remaining accepted identifier refers to a single taxon only.
   * See #duplicateProParteSynonyms()
   *
   * @param taxonID taxonID of the synonym
   * @param sciname scientificName of the synonym
   * @param canonical canonical name of the synonym
   * @return true if it is a synonym of some type
   */
  private boolean setupAcceptedRel(Node n, @Nullable String taxonID, String sciname, @Nullable String canonical, Rank rank) {
    TaxonomicStatus status = mapper.readEnum(n, TaxonProperties.STATUS, TaxonomicStatus.class, TaxonomicStatus.DOUBTFUL);
    Node accepted = null;
    if (NeoMapper.hasProperty(n, DwcTerm.acceptedNameUsageID)) {
      final String id = NeoMapper.value(n, DwcTerm.acceptedNameUsageID);
      if (id != null && (taxonID == null || !id.equals(taxonID))) {
        accepted = nodeByTaxonId(id);
        if (accepted == null) {
          mapper.addIssue(n, NameUsageIssue.ACCEPTED_NAME_USAGE_ID_INVALID);
          LOG.debug("acceptedNameUsageID {} not existing", id);
          // is the accepted name also mapped?
          String name = ObjectUtils.defaultIfNull(NeoMapper.value(n, DwcTerm.acceptedNameUsage), NormalizerConstants.PLACEHOLDER_NAME);
          accepted = createTaxonWithClassificationProps(Origin.MISSING_ACCEPTED, name, rank, TaxonomicStatus.DOUBTFUL, n, id, "Placeholder for the missing accepted taxonID for synonym " + sciname);
          setupRelation(accepted);
        }
      }
    } else if (NeoMapper.hasProperty(n, DwcTerm.acceptedNameUsage)) {
      final String name = NeoMapper.value(n, DwcTerm.acceptedNameUsage);
      if (name != null && !name.equals(sciname)) {
        try {
          accepted = nodeBySciname(name);
          if (accepted == null && !name.equals(canonical)) {
            accepted = nodeByCanonical(name);
            if (accepted == null) {
              LOG.debug("acceptedNameUsage {} not existing, materialize it", name);
            }
          }
        } catch (NotUniqueException e) {
          mapper.addIssue(n, NameUsageIssue.ACCEPTED_NAME_NOT_UNIQUE);
          LOG.warn("acceptedNameUsage {} not unique, duplicate accepted name for synonym {} and taxonID {}", name,
            sciname, taxonID);
        }
        if (accepted == null) {
          accepted = createTaxonWithClassificationProps(Origin.VERBATIM_ACCEPTED, name, null, TaxonomicStatus.DOUBTFUL, n, null, null);
          setupRelation(accepted);
        }
      }
    }

    // if status is synonym but we aint got no idea of the accepted insert an incertae sedis record of same rank
    if (status.isSynonym() && accepted == null) {
      mapper.addIssue(n, NameUsageIssue.ACCEPTED_NAME_MISSING);
      accepted = createTaxonWithClassificationProps(Origin.MISSING_ACCEPTED, NormalizerConstants.PLACEHOLDER_NAME, rank, TaxonomicStatus.DOUBTFUL, n, null, "Placeholder for the missing accepted taxon for synonym " + sciname);
      setupRelation(accepted);
    }

    if (accepted != null && !accepted.equals(n)) {
      // make sure taxonomic status reflects the synonym relation
      if (!status.isSynonym()) {
        status = TaxonomicStatus.SYNONYM;
        mapper.storeEnum(n, TaxonProperties.STATUS, status);
      }
      n.createRelationshipTo(accepted, RelType.SYNONYM_OF);
      n.addLabel(Labels.SYNONYM);
    }
    return status.isSynonym();
  }

  /**
   * Sets up the parent relations using the parentNameUsage(ID) term values.
   * The denormed, flat classification is used in a next step later.
   */
  private void setupParentRel(Node n, boolean isSynonym, @Nullable String taxonID, String sciname,
    String canonical) {
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
        try {
          parent = nodeBySciname(name);
          if (parent == null && !name.equals(canonical)) {
            parent = nodeByCanonical(name);
          }
          if (parent == null) {
            LOG.debug("parentNameUsage {} not existing, materialize it", name);
            parent = createTaxon(Origin.VERBATIM_PARENT, name, null, TaxonomicStatus.DOUBTFUL, true);
          }
        } catch (NotUniqueException e) {
          mapper.addIssue(n, NameUsageIssue.PARENT_NAME_NOT_UNIQUE);
          LOG.warn("parentNameUsage {} not unique, ignore relationship for name {} and taxonID {}", name, sciname, taxonID);
          parent = createTaxon(Origin.VERBATIM_PARENT, name, null, TaxonomicStatus.DOUBTFUL, true);
        }
      }
    }
    if (parent != null && !parent.equals(n)) {
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
        try {
          if (name != null && !name.equals(sciname)) {
            basionym = nodeBySciname(name);
            if (basionym == null && !name.equals(canonical)) {
              basionym = nodeByCanonical(name);
            }
            if (basionym == null) {
              LOG.debug("originalNameUsage {} not existing, materialize it", name);
              basionym = createTaxon(Origin.VERBATIM_BASIONYM, name, null, TaxonomicStatus.DOUBTFUL, true);
            }
          }
        } catch (NotUniqueException e) {
          mapper.addIssue(n, NameUsageIssue.ORIGINAL_NAME_NOT_UNIQUE);
          LOG.warn("originalNameUsage {} not unique, ignore relationship for taxonID {}", sciname, taxonID);
        }
      }
      if (basionym != null && !basionym.equals(n)) {
        basionym.createRelationshipTo(n, RelType.BASIONYM_OF);
      }
    }
  }

  private Node createTaxon(Origin origin, String sciname, Rank rank, TaxonomicStatus status, boolean isRoot) {
    Node n = super.create(origin, sciname, rank, status);
    if (isRoot) {
      n.addLabel(Labels.ROOT);
    }
    return n;
  }

  /**
   *
   * @param origin
   * @param sciname
   * @param rank
   * @param status
   * @param classificationSource
   * @param taxonID the optional taxonID to apply to the new node
   */
  private Node createTaxonWithClassificationProps(Origin origin, String sciname, Rank rank, TaxonomicStatus status,
    Node classificationSource, @Nullable String taxonID, @Nullable String remarks) {
    Node n = createTaxon(origin, sciname, rank, status, false);
    // copy props from source
    for (String p : CLASSIFICATION_PROPERTIES) {
      try {
        n.setProperty(p, classificationSource.getProperty(p));
      } catch (NotFoundException e) {
        // ignore
      }
    }
    if (!Strings.isNullOrEmpty(taxonID)) {
      n.setProperty(TaxonProperties.TAXON_ID, taxonID);
    }
    if (!Strings.isNullOrEmpty(remarks)) {
      n.setProperty(TaxonProperties.REMARKS, remarks);
    }
    return n;
  }

  /**
   * Copies an existing taxon node with all its properties and sets the origin value.
   */
  private Node createTaxon(Node source, Origin origin) {
    Node n = db.createNode(Labels.TAXON);
    for (String k : source.getPropertyKeys()) {
      if (!k.equals(TaxonProperties.TAXON_ID)) {
        n.setProperty(k, source.getProperty(k));
      }
    }
    mapper.storeEnum(n, "origin", origin);
    return n;
  }

}
