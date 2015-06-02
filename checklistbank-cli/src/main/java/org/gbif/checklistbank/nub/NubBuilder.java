package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.normalizer.UsageMetricsAndNubMatchHandler;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.traverse.TaxonWalker;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.nub.source.ClbUsageSource;
import org.gbif.checklistbank.nub.source.NubSource;
import org.gbif.checklistbank.nub.source.UsageSource;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubBuilder implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(NubBuilder.class);
  private static final String NEO_PROP_SRC_IDS = "sourceIds";
  private static final Set<Rank> NUB_RANKS;
  static {
    List<Rank> ranks = Lists.newArrayList(Rank.LINNEAN_RANKS);
    ranks.add(Rank.SUBSPECIES);
    ranks.add(Rank.VARIETY);
    ranks.add(Rank.FORM);
    NUB_RANKS = ImmutableSet.copyOf(ranks);
  }

  private final Set<Rank> allowedRanks = Sets.newHashSet();
  private final NubConfiguration cfg;
  private final NubDb db;
  private final NameUsageMatchingService matchingService;
  private final UsageSource usageSource;
  private final NameParser parser = new NameParser();
  private NubSource currSrc;
  private ParentStack parents;
  private int sourceUsageCounter = 0;
  private final NeoMapper mapper = NeoMapper.instance();
  private final Map<Kingdom, NubUsage> kingdoms = Maps.newHashMap();

  private NubBuilder(NubConfiguration cfg, UsageSource usageSource) {
    db = new NubDb(cfg.neo.newEmbeddedDb(Constants.NUB_DATASET_KEY, true), 1000);
    this.cfg = cfg;
    this.usageSource = usageSource;
    matchingService = cfg.matching.createMatchingService();
  }

  public static NubBuilder create(NubConfiguration cfg) {
    return new NubBuilder(cfg, new ClbUsageSource(cfg));
  }

  public static NubBuilder create(NubConfiguration cfg, UsageSource usageSource) {
    return new NubBuilder(cfg, usageSource);
  }

  @Override
  public void run() {
    addKingdoms();
    parents = new ParentStack(kingdoms.get(Kingdom.INCERTAE_SEDIS));
    addDatasets();
    setEmptyGroupsDoubtful();
    groupByOriginalName();
    addExtensionData();
    assignUsageKeysAndMetrics();
    db.close();
  }

  private void addKingdoms() {
    LOG.info("Adding kingdom");
    currSrc = new NubSource();
    currSrc.key = Constants.NUB_DATASET_KEY;
    for (Kingdom k : Kingdom.values()) {
      NubUsage ku = new NubUsage();
      ku.kingdom_ = k;
      ku.datasetKey = Constants.NUB_DATASET_KEY;
      ku.origin = Origin.SOURCE;
      ku.rank = Rank.KINGDOM;
      ku.status = TaxonomicStatus.ACCEPTED;
      ku.parsedName = new ParsedName();
      ku.parsedName.setGenusOrAbove(k.scientificName());

      db.addRoot(ku);
      kingdoms.put(k, ku);
    }

  }


  /**
   * TODO: to be implemented.
   * Now clb still dynamically retrieves extension data from all checklists, but in the future we like to control
   * which extension record is attached to a backbone usage.
   *
   * Adds all extension data, e.g. vernacular names, to the backbone directly.
   * TODO:
   *  - build map from source usage key to nub node id
   *  - stream (jdbc copy) through all extension data in postgres and attach to relevant nub node
   */
  private void addExtensionData() {
    LOG.info("No extension data copied to backbone");
    Joiner commaJoin = Joiner.on(", ").skipNulls();
    for (Node n : IteratorUtil.asIterable(db.gds.findNodes(Labels.TAXON))) {
      List<Integer> srcIds = mapper.readList(n, NEO_PROP_SRC_IDS, Integer.class);
      if (srcIds != null) {
        LOG.debug("Add extension data from source ids {}", commaJoin.join(srcIds));
      }
    }
  }

  private void setEmptyGroupsDoubtful() {
    LOG.info("flag empty genera as doubtful");
  }

  private void addDatasets() {
    List<NubSource> sources = usageSource.listSources();
    LOG.info("Start adding {} backbone sources", sources.size());
    for (NubSource source : sources) {
      addDataset(source);
    }
  }

  private void addDataset(NubSource source) {
    LOG.info("Adding source {}", source.name);
    currSrc = source;
    // prepare set of allowed ranks for this source
    allowedRanks.clear();
    for (Rank r : Rank.values()) {
      if (NUB_RANKS.contains(r) && r.ordinal() >= source.ignoreRanksAbove.ordinal()) {
        allowedRanks.add(r);
      }
    }
    parents.clear();
    int start = sourceUsageCounter;
    for (SrcUsage u : usageSource.iterateSource(source)) {
      LOG.debug("process {} {}", u.rank, u.scientificName);
      sourceUsageCounter++;
      parents.add(u);
      NubUsage nub = processSourceUsage(u, Origin.SOURCE, parents.nubParent());
      if (nub != null) {
        parents.put(nub);
      }
    }
    db.renewTx();
    LOG.info("Processed {} source usages for {}", sourceUsageCounter - start, source.name);
  }

  private NubUsage processSourceUsage(SrcUsage u, Origin origin, NubUsage parent) {
    Preconditions.checkNotNull(u.status);
    Preconditions.checkNotNull(u.rank);
    // try to parse name
    NubUsage nub = null;
    try {
      addParsedNameIfNull(u);
      nub = db.findNubUsage(u, parents.nubKingdom());
      if (u.rank != null && allowedRanks.contains(u.rank)) {
        if (nub == null) {
          // create new nub usage
          nub = createNubUsage(u, origin, parent);
        } else {
          // update nub usage
          updateNub(nub, u, parent);
        }
      } else {
        LOG.debug("Ignore source usage with undesired rank {}: {}", u.rank, u.scientificName);
      }

    } catch (UnparsableException e) {
      // exclude virus, hybrid and blacklisted names
      // TODO: review if we want to include them!
      LOG.warn("Ignore unparsable {} name: {}", e.type, e.name);
    }
    return nub;
  }

  private NubUsage createNubUsage(SrcUsage u, Origin origin, NubUsage p) throws UnparsableException {
    addParsedNameIfNull(u);
    if (!u.status.isSynonym()) {
      // check if implicit species or genus parents are needed
      if (u.rank == Rank.SPECIES && p.rank != Rank.GENUS) {
        SrcUsage genus = new SrcUsage();
        genus.rank = Rank.GENUS;
        genus.scientificName = u.parsedName.getGenusOrAbove();
        genus.status = TaxonomicStatus.ACCEPTED;
        p = processSourceUsage(genus, Origin.IMPLICIT_NAME, p);

      } else if (u.rank.isInfraspecific() && p.rank != Rank.SPECIES) {
        SrcUsage spec = new SrcUsage();
        spec.rank = Rank.SPECIES;
        spec.scientificName = u.parsedName.canonicalSpeciesName();
        spec.status = TaxonomicStatus.ACCEPTED;
        p = processSourceUsage(spec, Origin.IMPLICIT_NAME, p);
      }

      // check for autonyms
      if (u.rank.isInfraspecific() && !u.parsedName.isAutonym()) {
        ParsedName autonym = new ParsedName();
        autonym.setGenusOrAbove(u.parsedName.getGenusOrAbove());
        autonym.setSpecificEpithet(u.parsedName.getSpecificEpithet());
        autonym.setInfraSpecificEpithet(u.parsedName.getSpecificEpithet());
        autonym.setScientificName(autonym.canonicalName());
        autonym.setRank(u.rank);

        NubUsage auto = db.findNubUsage(autonym.canonicalName(), u.rank);
        if (auto == null) {
          SrcUsage autoUsage = new SrcUsage();
          autoUsage.rank = u.rank;
          autoUsage.scientificName = autonym.canonicalName();
          autoUsage.parsedName = autonym;
          autoUsage.status = TaxonomicStatus.ACCEPTED;
          processSourceUsage(autoUsage, Origin.AUTONYM, p);
        }
      }

    }
    return db.addUsage(p, u, origin, currSrc.key);
  }

  private void addParsedNameIfNull(SrcUsage u) throws UnparsableException {
    if (u.parsedName == null) {
      u.parsedName = parser.parse(u.scientificName);
    }
  }

  private void updateNub(NubUsage nub, SrcUsage u, NubUsage parent) {
    LOG.debug("Updating {} from source {}", nub.parsedName.getScientificName(), u.scientificName);
    nub.sourceIds.add(u.key);
    nub.origin = Origin.SOURCE;
    nub.authors.add(u.parsedName.authorshipComplete());
    if (u.parsedName.isAuthorsParsed() && nub.parsedName.authorshipComplete().isEmpty()) {
      nub.parsedName.setAuthorship(u.parsedName.getAuthorship());
      nub.parsedName.setYear(u.parsedName.getYear());
      nub.parsedName.setBracketAuthorship(u.parsedName.getBracketAuthorship());
      nub.parsedName.setBracketYear(u.parsedName.getBracketYear());
      nub.parsedName.setAuthorsParsed(true);
    }
    if (nub.publishedIn == null) {
      nub.publishedIn = u.publishedIn;
    }
    if (nub.nomStatus.isEmpty()) {
      nub.addNomStatus(u.nomStatus);
    }
    //TODO: update parent???
    db.store(nub);
  }

  private void groupByOriginalName() {
    LOG.info("Start grouping by original names");
  }

  /**
   * Assigns a unique usageKey to all nodes by matching a usage to the previous backbone to keep stable identifiers.
   */
  private void assignUsageKeysAndMetrics() {
    LOG.info("Walk all accepted taxa, build metrics and match to previous GBIF backbone");
    UsageMetricsAndNubMatchHandler metricsHandler = new UsageMetricsAndNubMatchHandler(matchingService, db.gds);
    TaxonWalker.walkAccepted(db.gds, metricsHandler, 10000, null);
    db.renewTx();
    NormalizerStats stats = metricsHandler.getStats(0, null);
    LOG.info("Walked all taxa (root={}, total={}, synonyms={}) and built usage metrics", stats.getRoots(), stats.getCount(), stats.getSynonyms());
  }

  private void trackSourceIds(Node n, Integer id) {
    List<Integer> ids = mapper.readList(n, NEO_PROP_SRC_IDS, Integer.class);
    if (ids == null) {
      ids = Lists.newArrayList();
    }
    ids.add(id);
    mapper.storeList(n, NEO_PROP_SRC_IDS, ids);
  }
}
