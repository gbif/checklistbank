package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.TaxonWalker;
import org.gbif.checklistbank.neo.traverse.UsageMetricsHandler;
import org.gbif.checklistbank.nub.model.NubBuildStats;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
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
import org.apache.commons.lang3.ObjectUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubBuilder implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(NubBuilder.class);
  private static final Set<Rank> NUB_RANKS;
  static {
    List<Rank> ranks = Lists.newArrayList(Rank.LINNEAN_RANKS);
    ranks.add(Rank.SUBSPECIES);
    ranks.add(Rank.VARIETY);
    ranks.add(Rank.FORM);
    ranks.remove(Rank.KINGDOM); // we only use kingdoms from our enum
    NUB_RANKS = ImmutableSet.copyOf(ranks);
  }

  private final Set<Rank> allowedRanks = Sets.newHashSet();
  private final NubDb db;
  private final NameUsageMatchingService matchingService;
  private final UsageSource usageSource;
  private final NameParser parser = new NameParser();
  private final NubBuildStats buildStats = new NubBuildStats();
  private NormalizerStats normalizerStats;
  private NubSource currSrc;
  private ParentStack parents;
  private int sourceUsageCounter = 0;
  private final Map<Kingdom, NubUsage> kingdoms = Maps.newHashMap();

    private NubBuilder(UsageDao dao, UsageSource usageSource, NameUsageMatchingService matchingService) {
    db = NubDb.create(dao, 1000);
    this.usageSource = usageSource;
    this.matchingService = matchingService;
  }

  public static NubBuilder create(NubConfiguration cfg) {
    UsageDao dao = UsageDao.persistentDao(cfg.neo, Constants.NUB_DATASET_KEY, null, true);
    NameUsageMatchingService matchingService = cfg.matching.createMatchingService();
    return new NubBuilder(dao, cfg.usageSource(), matchingService);
  }

  public static NubBuilder create(UsageDao dao, UsageSource usageSource, NameUsageMatchingService matchingService) {
    return new NubBuilder(dao, usageSource, matchingService);
  }

  @Override
  public void run() {
    addKingdoms();
    parents = new ParentStack(kingdoms.get(Kingdom.INCERTAE_SEDIS));
    addDatasets();
    setEmptyGroupsDoubtful();
    groupByOriginalName();
    addExtensionData();
    assignUsageKeys();
    db.dao.convertNubUsages();
    builtUsageMetrics();
    LOG.info("New backbone built");
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
    LOG.info("NOT IMPLEMENTED: Copy extension data to backbone");
    if (false) {
      Joiner commaJoin = Joiner.on(", ").skipNulls();
      for (Node n : IteratorUtil.loop(db.dao.allTaxa())) {
        NubUsage nub = db.dao.readNub(n);
        if (!nub.sourceIds.isEmpty()) {
          LOG.debug("Add extension data from source ids {}", commaJoin.join(nub.sourceIds));
        }
      }
    }
  }

  private void setEmptyGroupsDoubtful() {
    LOG.info("flag empty genera as doubtful");
  }

  private void addDatasets() {
    LOG.info("Query registry for backbone sources ...");
    List<NubSource> sources = usageSource.listSources();
    LOG.info("Start adding {} backbone sources:", sources.size());
    for (NubSource source : sources) {
      LOG.debug("Nub source: {}", source.name);
    }
    for (NubSource source : sources) {
      addDataset(source);
    }
    db.closeTx();
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
      LOG.debug("process {} {} {}", u.status, u.rank, u.scientificName);
      sourceUsageCounter++;
      parents.add(u);
      try {
        NubUsage nub = processSourceUsage(u, Origin.SOURCE, parents.nubParent());
        if (nub != null) {
          parents.put(nub);
        }
      } catch (RuntimeException e) {
        LOG.error("Error processing {} for source {}", u.scientificName, source.name);
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
          updateNub(nub, u, origin, parent);
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

    // first check if parent is a genus synonym
    // http://dev.gbif.org/issues/browse/POR-2780
    if (!u.status.isSynonym() && p.rank == Rank.GENUS && p.status.isSynonym()) {
      // create a new accepted and make this name a synonym
      NubUsage pacc = db.getParent(p);
      SrcUsage comb = new SrcUsage();
      comb.status = TaxonomicStatus.DOUBTFUL;
      comb.rank = u.rank;
      comb.parsedName = new ParsedName();
      comb.parsedName.setGenusOrAbove(pacc.parsedName.getGenusOrAbove());
      comb.parsedName.setSpecificEpithet(u.parsedName.getSpecificEpithet());
      comb.parsedName.setInfraSpecificEpithet(u.parsedName.getInfraSpecificEpithet());
      comb.parsedName.setRank(u.rank);
      comb.parsedName.setBracketAuthorship(ObjectUtils.firstNonNull(u.parsedName.getBracketAuthorship(), u.parsedName.getAuthorship()));
      comb.parsedName.setBracketYear(ObjectUtils.firstNonNull(u.parsedName.getBracketYear(), u.parsedName.getYear()));
      LOG.info("Recombining {} into {}", u.parsedName.fullName(), comb.parsedName.fullName());
      p = processSourceUsage(comb, Origin.AUTO_RECOMBINATION, pacc);
      u.status = TaxonomicStatus.SYNONYM;
    }

    if (!u.status.isSynonym()) {
      // check if implicit species or genus parents are needed
      if (u.rank == Rank.SPECIES && p.rank != Rank.GENUS) {
        SrcUsage genus = new SrcUsage();
        genus.rank = Rank.GENUS;
        genus.scientificName = u.parsedName.getGenusOrAbove();
        genus.status = u.status;
        p = processSourceUsage(genus, Origin.IMPLICIT_NAME, p);

      } else if (u.rank.isInfraspecific() && p.rank != Rank.SPECIES) {
        SrcUsage spec = new SrcUsage();
        spec.rank = Rank.SPECIES;
        spec.scientificName = u.parsedName.canonicalSpeciesName();
        spec.status = u.status;
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

  private void copyAuthorship(ParsedName from, ParsedName to) {
    to.setAuthorship(from.getAuthorship());
    to.setYear(from.getYear());
    to.setBracketAuthorship(from.getBracketAuthorship());
    to.setBracketYear(from.getBracketYear());
    to.setAuthorsParsed(true);

  }

  private void updateNub(NubUsage nub, SrcUsage u, Origin origin, NubUsage parent) {
    if (nub.status.isSynonym() != u.status.isSynonym()) {
      return;
    }
    LOG.debug("Updating {} from source {}", nub.parsedName.fullName(), u.parsedName.fullName());
    nub.sourceIds.add(u.key);
    if (origin == Origin.SOURCE) {
      // only override original origin value if we update from a true source
      nub.origin = Origin.SOURCE;
    }
    nub.authors.add(u.parsedName.authorshipComplete());

    NubUsage currNubParent = db.getParent(nub);
    // prefer accepted version over doubtful if its coming from the same dataset!
    if (nub.status == TaxonomicStatus.DOUBTFUL && u.status == TaxonomicStatus.ACCEPTED && fromCurrentSource(nub)) {
      nub.status = u.status;
      if (!u.parsedName.authorshipComplete().isEmpty()) {
        copyAuthorship(u.parsedName, nub.parsedName);
      }
      if (parent != null && (currNubParent.rank.higherThan(parent.rank) || currNubParent.rank == parent.rank)) {
        if (db.existsInClassification(currNubParent.node, parent.node)) {
          // current classification has this parent already in its parents list. No need to change anything
        } else {
          // current classification doesnt have that parent, we need to apply it
          updateParent(nub, parent);
        }
      }

    } else if (nub.status.isSynonym()) {
      // maybe we have a prop arte synonym from the same dataset?
      if (fromCurrentSource(nub) && !parent.node.equals(currNubParent.node)) {
        nub.status = TaxonomicStatus.PROPARTE_SYNONYM;
        // create new pro parte relation
        LOG.debug("New accepted name {} found for pro parte synonym {}",  parent.parsedName.getScientificName(), nub.parsedName.getScientificName());
        db.setSingleRelationship(nub.node, parent.node, RelType.PROPARTE_SYNONYM_OF);

      } else {
        // this might be a more exact kind of synonym status
        if (nub.status == TaxonomicStatus.SYNONYM) {
          nub.status = u.status;
        }
        if (nub.parsedName.authorshipComplete().isEmpty() && !u.parsedName.authorshipComplete().isEmpty()) {
          copyAuthorship(u.parsedName, nub.parsedName);
        }
      }

    } else {
      // ACCEPTED
      if (parent != null && currNubParent.rank.higherThan(parent.rank) ) {
        if (db.existsInClassification(parent.node, currNubParent.node)) {
          updateParent(nub, parent);
        }
      }
      if (nub.parsedName.authorshipComplete().isEmpty() && !u.parsedName.authorshipComplete().isEmpty()) {
        copyAuthorship(u.parsedName, nub.parsedName);
      }
    }
    if (nub.publishedIn == null) {
      nub.publishedIn = u.publishedIn;
    }
    if (nub.nomStatus.isEmpty()) {
      nub.addNomStatus(u.nomStatus);
    }
    db.store(nub);
  }

  private void updateParent(NubUsage child, NubUsage parent) {
    LOG.debug("Update {} classification with new parent {} {}",  child.parsedName.getScientificName(), parent.rank, parent.parsedName.getScientificName());
    db.updateParentRel(child.node, parent.node);
  }

  private boolean fromCurrentSource(NubUsage nub) {
    if (nub.datasetKey.equals(currSrc.key)) {
      return true;
    }
    return false;
  }

  private void groupByOriginalName() {
    LOG.info("Start grouping by original names");
  }

  /**
   * Assigns a unique usageKey to all nodes by matching a usage to the previous backbone to keep stable identifiers.
   */
  private void assignUsageKeys() {
    //TODO: nub matching using the new author based matching...
    LOG.info("NOT IMPLEMENTED: match to previous GBIF backbone");
  }

  private void builtUsageMetrics() {
    LOG.info("Walk all accepted taxa and build usage metrics");
    UsageMetricsHandler metricsHandler = new UsageMetricsHandler(db.dao);
    TaxonWalker.walkAccepted(db.dao.getNeo(), null, metricsHandler);
    normalizerStats = metricsHandler.getStats(0, null);
    LOG.info("Walked all taxa (root={}, total={}, synonyms={}) and built usage metrics", normalizerStats.getRoots(), normalizerStats.getCount(), normalizerStats.getSynonyms());
  }

    public NubBuildStats getBuildStats() {
        return buildStats;
    }

    public NormalizerStats getStats() {
        return normalizerStats;
    }
}
