package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.authorship.BasionymGroup;
import org.gbif.checklistbank.authorship.BasionymSorter;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.neo.traverse.TreeWalker;
import org.gbif.checklistbank.neo.traverse.UsageMetricsHandler;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.NubUsageMatch;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.nub.source.ClbSource;
import org.gbif.checklistbank.nub.source.ClbSourceList;
import org.gbif.checklistbank.nub.source.NubSource;
import org.gbif.checklistbank.nub.source.NubSourceList;
import org.gbif.checklistbank.nub.validation.NubAssertions;
import org.gbif.checklistbank.nub.validation.NubTreeValidation;
import org.gbif.checklistbank.nub.validation.NubValidation;
import org.gbif.checklistbank.utils.SciNameNormalizer;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.utils.collection.MapUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import com.carrotsearch.hppc.IntLongHashMap;
import com.carrotsearch.hppc.IntLongMap;
import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongIntHashMap;
import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.ObjectLongHashMap;
import com.carrotsearch.hppc.ObjectLongMap;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.LongCursor;
import com.carrotsearch.hppc.cursors.LongIntCursor;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.helpers.Strings;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubBuilder implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(NubBuilder.class);
  private static final Joiner SEMICOLON_JOIN = Joiner.on("; ").skipNulls();
  public static final Set<Rank> NUB_RANKS;

  static {
    List<Rank> ranks = Lists.newArrayList(Rank.LINNEAN_RANKS);
    ranks.add(Rank.SUBSPECIES);
    ranks.add(Rank.VARIETY);
    ranks.add(Rank.FORM);
    ranks.remove(Rank.KINGDOM); // we only use kingdoms from our enum
    NUB_RANKS = ImmutableSet.copyOf(ranks);
  }

  private final Set<NameType> ignoredNameTypes = Sets.newHashSet(
      NameType.CANDIDATUS, NameType.CULTIVAR, NameType.INFORMAL, NameType.NO_NAME, NameType.PLACEHOLDER, NameType.NO_NAME
  );
  private final static ImmutableMap<TaxonomicStatus, Integer> STATUS_ORDER = ImmutableMap.of(
      TaxonomicStatus.HOMOTYPIC_SYNONYM, 1,
      TaxonomicStatus.HETEROTYPIC_SYNONYM, 2,
      TaxonomicStatus.SYNONYM, 3,
      TaxonomicStatus.ACCEPTED, 4,
      TaxonomicStatus.DOUBTFUL, 5
      );
  private static final Pattern EX_AUTHOR = Pattern.compile("^(.+) ex ", Pattern.CASE_INSENSITIVE);

  private final Set<Rank> allowedRanks = Sets.newHashSet();
  private final NubDb db;
  private final boolean closeDao;
  private final NubSourceList sources;
  private final NameParser parser;
  private final NubConfiguration cfg;
  private NubSource currSrc;
  private ParentStack parents;
  private int sourceUsageCounter = 0;
  private final AuthorComparator authorComparator;
  private final IdGenerator idGen;
  private final IntLongMap src2NubKey = new IntLongHashMap();
  private final LongIntMap basionymRels = new LongIntHashMap(); // node.id -> src.usageKey
  private final Map<UUID, Integer> priorities = Maps.newHashMap();
  private Integer maxPriority = 0;
  private int datasetCounter = 1;

  private NubBuilder(UsageDao dao, NubSourceList sources, IdLookup idLookup, AuthorComparator authorComparator, int newIdStart,
                     boolean closeDao, NubConfiguration cfg) {
    db = NubDb.create(dao, authorComparator);
    this.sources = sources;
    this.authorComparator = authorComparator;
    idGen = new IdGenerator(idLookup, newIdStart);
    this.closeDao = closeDao;
    this.cfg = cfg;
    this.parser = new NameParser(cfg.parserTimeout);
  }

  public static NubBuilder create(NubConfiguration cfg) {
    UsageDao dao = UsageDao.persistentDao(cfg.neo, Constants.NUB_DATASET_KEY, false, null, true);
    try {
      IdLookupImpl idLookup = IdLookupImpl.temp().load(cfg.clb, true);
      return new NubBuilder(dao, ClbSourceList.create(cfg), idLookup, idLookup.getAuthorComparator(), idLookup.getKeyMax() + 1, true, cfg);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load existing backbone ids", e);
    }
  }

  /**
   * @param dao the dao to persistent the nub. Will be left open after run() is called.
   */
  public static NubBuilder create(UsageDao dao, NubSourceList sources, IdLookup idLookup, int newIdStart, int parserTimeout) {
    NubConfiguration cfg = new NubConfiguration();
    cfg.groupBasionyms = true;
    cfg.validate = true;
    cfg.runAssertions = false;
    cfg.autoImport = false;
    cfg.neo.batchSize = 5000;
    cfg.parserTimeout = parserTimeout;
    return new NubBuilder(dao, sources, idLookup, idLookup.getAuthorComparator(), newIdStart, false, cfg);
  }

  /**
   * Builds a new neo4j based backbone with metrics and stable ids already mapped.
   * The DAO is kept open if you provided it explicitly, otherwise its being closed.
   */
  @Override
  public void run() {
    try {
      addKingdoms();
      parents = new ParentStack(db.kingdom(Kingdom.INCERTAE_SEDIS));

      // main work importing all source checklists
      addDatasets();

      // change current datasource to nub algorithm, avoiding using the last source for algorithmically generated usages
      currSrc = new ClbSource(null, Constants.NUB_DATASET_KEY, "Backbone algorithm");

      // detect and group basionyms
      groupByBasionym();

      // extract synonyms from ex authors
      synonymizeExAuthors();

      // flagging of suspicous usages
      flagParentMismatch();
      flagEmptyGenera();
      cleanImplicitTaxa();
      flagDuplicateAcceptedNames();
      flagSimilarNames();
      flagDoubtfulOriginalNames();

      // persist missing autonyms
      fixInfraspeciesHierarchy();
      manageAutonyms();

      // basic neo tree checks, fail fast
      if (cfg.validate) {
        validate(new NubTreeValidation(db));
      }

      // add extra data
      addPublicationDois();
      addExtensionData();

      // match to old nub and assign (stable) usage keys for postgres
      assignUsageKeys();

      // final validation with often reported issues
      if (cfg.runAssertions) {
        validate(new NubAssertions(db));
      }

      // convert usages for the importer and build metrics
      db.dao().convertNubUsages();
      builtUsageMetrics();
      LOG.info("New backbone built successfully!");

    } catch (AssertionError e){
      LOG.error("Backbone invalid, build failed!", e);
      throw e;

    } catch (RuntimeException e){
      LOG.error("Fatal error. Backbone build failed!", e);
      db.dao().consistencyNubReport();
      throw e;

    } finally {
      sources.close();
      if (closeDao) {
        db.dao().close();
        LOG.info("Backbone dao closed orderly");
      } else {
        LOG.warn("Backbone dao not closed!");
      }
    }
  }

  /**
   * Goes through all names with ex authors (99% botanical) and creates homotypic synonyms with the ex authorship.
   * Ex authors are publications which published a name earlier than the regular author, but which are illegitimate according to the code, for example a nomen nudum.
   *
   * See http://dev.gbif.org/issues/browse/POR-3147
   */
  private void synonymizeExAuthors() {
    LOG.info("Extract ex author species synonyms");
    try (Transaction tx = db.beginTx()) {
      synonymizeExAuthors(db.dao().allSpecies());
      tx.success();
    }

    LOG.info("Extract ex author infraspecies synonyms");
    try (Transaction tx = db.beginTx()) {
      synonymizeExAuthors(db.dao().allInfraSpecies());
      tx.success();
    }
  }

  private void synonymizeExAuthors(ResourceIterator<Node> iter) {
    for (Node n : IteratorUtil.loop(iter)) {
      NubUsage nub = read(n);
      if (!Strings.isBlank(nub.parsedName.getAuthorship())) {
        Matcher m = EX_AUTHOR.matcher(nub.parsedName.getAuthorship());
        if (m.find()) {
          try {
            // create synonym if not already existing
            SrcUsage syn = new SrcUsage();
            syn.parsedName = nub.parsedName;
            syn.parsedName.setAuthorship(m.group(1));
            syn.parsedName.setYear(null);
            syn.parsedName.setScientificName(syn.parsedName.canonicalNameComplete());
            syn.rank = nub.rank;
            syn.status = TaxonomicStatus.HOMOTYPIC_SYNONYM;

            // the parent nub does not matter as we always do a qualified author based matching
            NubUsageMatch match = db.findNubUsage(nub.datasetKey, syn, nub.kingdom, null);
            if (!match.isMatch() || !match.usage.parsedName.hasAuthorship()) {
              // create a new synonym
              NubUsage accepted = nub.status.isAccepted() ? nub : db.parent(nub);
              LOG.debug("Create ex author synonym {}", syn.parsedName.fullName());
              db.addUsage(accepted, syn, Origin.EX_AUTHOR_SYNONYM, currSrc.key);
            }
          } catch (IgnoreSourceUsageException e) {
            // swallow
          }
        }
      }
    }
  }

  /**
   * If there are several accepted infraspecific ranks for a given species
   * this method makes sure the species subtree makes sense.
   *
   * The same epithet can be used again within a species, at whatever level, only if the names with the re-used epithet
   * are attached to the same type. Thus there can be a form called Poa secunda f. juncifolia as well as
   * the subspecies Poa secunda subsp. juncifolia if, and only if, the type specimen of Poa secunda f. juncifolia
   * is the same as the type specimen of Poa secunda subsp. juncifolia.
   * In other words, if there is a single type specimen whose classification is Poa secunda subsp. juncifolia f. juncifolia.
   */
  private void fixInfraspeciesHierarchy() {
    //TODO
  }

  /**
   * Writes a file based report about deleted, resurrected and newly added taxa.
   */
  public void report(File reportingDir) {
    try {
      idGen.writeReports(reportingDir);
    } catch (IOException e) {
      LOG.warn("Failed to write ID report", e);
    }
  }

  private void flagDuplicateAcceptedNames() {
    for (Kingdom k : Kingdom.values()) {
      try (Transaction tx = db.beginTx()) {
        LOG.info("Start flagging doubtful duplicate names in {}", k);
        NubUsage ku = db.kingdom(k);
        markDuplicatesRedundant(Traversals.ACCEPTED_TREE.traverse(ku.node).nodes());
        tx.success();
      }
    }
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2815
   */
  private void flagDoubtfulOriginalNames() {
    LOG.info("Start flagging doubtful original names");
    try (Transaction tx = db.beginTx()) {
      for (Node gn : IteratorUtil.loop(db.dao().allGenera())) {
        NubUsage genus = read(gn);
        Integer gYear = genus.parsedName.getYearInt();
        if (gYear != null) {
          // all accepted included taxa should have been described after the genus
          // flag the ones that have an earlier publication date!
          for (Node n : Traversals.SORTED_ACCEPTED_TREE.traverse(gn).nodes()) {
            NubUsage u = read(n);
            Integer year = u.parsedName.getYearInt();
            if (year != null && year < gYear) {
              u.issues.add(NameUsageIssue.PUBLISHED_BEFORE_GENUS);
              db.store(u);
            }
          }
        }
      }
      tx.success();
    }
  }

  /**
   * Goes through all accepted species and infraspcecies and flags suspicous similar names.
   * Adds a NameUsageIssue.ORTHOGRAPHIC_VARIANT to all similar names.
   */
  private void flagSimilarNames() {
    LOG.info("Start flagging similar species");
    try (Transaction tx = db.beginTx()) {
      flagSimilarNames(db.dao().allSpecies());
      tx.success();
    }

    LOG.info("Start flagging similar infraspecies");
    try (Transaction tx = db.beginTx()) {
      flagSimilarNames(db.dao().allInfraSpecies());
      tx.success();
    }
  }

  /**
   * Small null safe wrapper around our DAO that logs missing nub usages for existing nodes.
   */
  private NubUsage read(Node n) {
    NubUsage u = db.dao().readNub(n);
    if (u == null) {
      LOG.error("Missing kvp nub usage for node {} {}", n.getId(), NeoProperties.getScientificName(n));
      throw new IllegalStateException("Missing kvp nub usage for node " + n.getId());
    }
    return u;
  }

  private void flagSimilarNames(ResourceIterator<Node> iter) {
    Map<String, String> names = Maps.newHashMap();
    for (Node n : IteratorUtil.loop(iter)) {
      if (!n.hasLabel(Labels.SYNONYM)) {
        NubUsage u = read(n);
        String normedName = db.dao().canonicalOrScientificName(u.parsedName, false);
        if (!Strings.isBlank(normedName)) {
          if (names.containsKey(normedName)) {
            u.issues.add(NameUsageIssue.ORTHOGRAPHIC_VARIANT);
            u.addRemark("Possible variant of "+names.get(normedName));
            db.store(u);
          } else {
            names.put(normedName, u.parsedName.canonicalNameComplete());
          }
        }
      }
    }
  }

  /**
   * Assigns a doubtful status to accepted names that only differ in authorship
   *
   * @param nodes any node iterable to check for names
   */
  private void markDuplicatesRedundant(ResourceIterable<Node> nodes) {
    ObjectLongMap<String> names = new ObjectLongHashMap<String>();
    for (Node n : nodes) {
      if (!n.hasLabel(Labels.SYNONYM)) {
        NubUsage u = read(n);
        String name = u.parsedName.canonicalName();
        if (u.status == TaxonomicStatus.ACCEPTED && !Strings.isBlank(name)) {
          // prefix with rank ordinal to become unique across ranks (ordinal is shorter than full name to save mem)
          String indexedName = u.rank.ordinal() + name;
          if (names.containsKey(indexedName)) {
            // duplicate accepted canonical name. Check which has priority
            Node n1 = db.getNode(names.get(indexedName));
            NubUsage u1 = read(n1);

            int p1 = priorities.get(u1.datasetKey);
            int p2 = priorities.get(u.datasetKey);

            if (p2 < p1) {
              // the old usage is from a less trusted source
              u1.status = TaxonomicStatus.DOUBTFUL;
              db.store(u1);
              names.put(indexedName, n.getId());
            } else {
              // the old usage is from a higher trusted source, keep it
              u.status = TaxonomicStatus.DOUBTFUL;
              db.store(u);
            }
          } else {
            names.put(indexedName, n.getId());
          }
        }
      }
    }
  }

  /**
   * Incorporate Rod Pages IPNI name DOIs from https://github.com/rdmpage/ipni-names
   */
  private void addPublicationDois() {

  }


  private void validate(NubValidation validator) throws AssertionError {
    try (Transaction tx = db.beginTx()) {
      boolean valid = validator.validate();
      if (valid) {
        LOG.info("{} passed", validator.getClass().getSimpleName());
      } else {
        LOG.error("Backbone is not valid! {} failed", validator.getClass().getSimpleName());
        throw new AssertionError("Backbone is not valid!");
      }
    }
  }

  /**
   * Goes through all accepted infraspecies and checks if a matching autonym exists,
   * creating missing autonyms where needed.
   * An autonym is an infraspecific taxon that has the same species and infraspecific epithet.
   * We do this last to not persistent autonyms that we dont need after basionyms are grouped or status has changed for some other reason.
   */
  private void manageAutonyms() {
    if (!cfg.keepLonelyAutonyms) {
      LOG.info("Delete lonely autonyms");
      try (Transaction tx = db.beginTx()) {
        for (Node n : IteratorUtil.loop(db.dao().allAutonyms())) {
          Rank rank = NeoProperties.getRank(n, Rank.UNRANKED);
          if (!n.hasLabel(Labels.SYNONYM)) {
            Node p = db.parent(n);
            // count all childs of same rank
            int count = 0;
            for (Node c : Traversals.CHILDREN.traverse(p).nodes()) {
              if (NeoProperties.getRank(c, Rank.UNRANKED) == rank) {
                count++;
              }
            }
            if (count == 1) {
              // only this accepted autonym, try to remove!!!
              LOG.info("Removing lonely {} autonym {} {}", rank, n, NeoProperties.getScientificName(n));
              removeTaxonIfEmpty(db.dao().readNub(n));
            }
          }
        }
        tx.success();
      }
    }

    LOG.info("Start creating missing autonyms");
    try (Transaction tx = db.beginTx()) {
      int counter = 0;
      for (Node n : IteratorUtil.loop(db.dao().allInfraSpecies())) {
        if (!n.hasLabel(Labels.SYNONYM)) {
          NubUsage u = read(n);
          // check for autonyms
          if (!u.parsedName.isAutonym()) {
            ParsedName pn = new ParsedName();
            pn.setType(NameType.SCIENTIFIC);
            pn.setGenusOrAbove(u.parsedName.getGenusOrAbove());
            pn.setSpecificEpithet(u.parsedName.getSpecificEpithet());
            pn.setInfraSpecificEpithet(u.parsedName.getSpecificEpithet());
            pn.setScientificName(pn.canonicalName());
            pn.setRank(u.rank);

            try {
              NubUsageMatch autoMatch = db.findNubUsage(pn.canonicalName(), u.rank, u.kingdom, true);
              if (!autoMatch.isMatch()) {
                NubUsage parent = db.parent(u);

                SrcUsage autonym = new SrcUsage();
                autonym.rank = u.rank;
                autonym.scientificName = pn.canonicalName();
                autonym.parsedName = pn;
                autonym.status = TaxonomicStatus.ACCEPTED;
                try {
                  createNubUsage(autonym, Origin.AUTONYM, parent);
                  counter++;
                } catch (IgnoreSourceUsageException e) {
                  LOG.warn("Fail to persistent missing autonym {}", pn.canonicalName());
                }
              }

            } catch (HomonymException e) {
              LOG.error("Homonym autonym found: {}", e.getName());
            }
          }
        }
      }
      tx.success();
      LOG.info("Created {} missing autonyms", counter);
    }
  }

  private List<Node> listFamilies() {
    List<Node> families;
    try (Transaction tx = db.beginTx()) {
      families = IteratorUtil.asList(db.dao().allFamilies());
    }
    return families;
  }

  /**
   * Goes through all usages and tries to discover basionyms by comparing the specific or infraspecific epithet and the authorships within a family.
   * As we often see missing brackets from author names we must code defensively and allow several original names in the data for a single epithet.
   */
  private void detectBasionyms() {
    try {
      LOG.info("Discover basionyms");
      int newBasionyms = 0;
      int newRelations = 0;
      final BasionymSorter basSorter = new BasionymSorter(authorComparator);

      // load all family nodes into list so we can process them seach in a separate transaction later on
      List<Node> families = listFamilies();
      for (Node n : families) {
        try (Transaction tx = db.beginTx()) {
          NubUsage fam = read(n);
          if (!fam.status.isSynonym()) {
            Map<String, List<NubUsage>> epithets = Maps.newHashMap();
            Map<String, Set<String>> epithetBridges = Maps.newHashMap();
            LOG.debug("Discover basionyms in family {}", fam.parsedName.canonicalNameComplete());
            // key all names by their terminal epithet
            for (Node c : Traversals.DESCENDANTS.traverse(n).nodes()) {
              NubUsage nub = read(c);
              // ignore all supra specific names and autonyms
              if (nub.rank.isSpeciesOrBelow() && !c.hasLabel(Labels.AUTONYM)) {
                String epithet = SciNameNormalizer.stemEpithet(nub.parsedName.getTerminalEpithet());
                if (!epithets.containsKey(epithet)) {
                  epithets.put(epithet, Lists.newArrayList(nub));
                } else {
                  epithets.get(epithet).add(nub);
                }
                // now check if a basionym relation exists already that reaches out to some other epithet, e.g. due to gender changes
                for (Node bg : Traversals.BASIONYM_GROUP.evaluator(Evaluators.excludeStartPosition()).traverse(c).nodes()) {
                  NubUsage bgu = read(bg);
                  String epithet2 = SciNameNormalizer.stemEpithet(bgu.parsedName.getTerminalEpithet());
                  if (epithet2 != null && !epithet2.equals(epithet)) {
                    if (!epithetBridges.containsKey(epithet)) {
                      epithetBridges.put(epithet, Sets.newHashSet(epithet2));
                    } else {
                      epithetBridges.get(epithet).add(epithet2);
                    }
                  }
                }
              }
            }
            LOG.debug("{} distinct epithets found in family {}", epithets.size(), fam.parsedName.canonicalNameComplete());

            // merge epithet groups based on existing basionym relations, catching some gender changes
            LOG.debug("{} epithets are connected with explicit basionym relations", epithetBridges.size());
            for (Map.Entry<String, Set<String>> bridge : epithetBridges.entrySet()) {
              if (epithets.containsKey(bridge.getKey())) {
                List<NubUsage> usages = epithets.get(bridge.getKey());
                for (String epi2 : bridge.getValue()) {
                  if (epithets.containsKey(epi2)) {
                    LOG.debug("Merging {} usages of epithet {} into epithet group {}", epithets.get(epi2).size(), epi2, bridge.getKey());
                    usages.addAll(epithets.remove(epi2));
                  }
                }
              }
            }

            // now compare authorships for each epithet group
            for (Map.Entry<String, List<NubUsage>> epithetGroup : epithets.entrySet()) {

              Collection<BasionymGroup<NubUsage>> groups = basSorter.groupBasionyms(epithetGroup.getValue(), new Function<NubUsage, ParsedName>() {
                @Override
                public ParsedName apply(NubUsage nub) {
                  return nub.parsedName;
                }
              });
              // go through groups and persistent basionym relations where needed
              for (BasionymGroup<NubUsage> group : groups) {
                // we only need to process groups that contain recombinations
                if (group.hasRecombinations()) {
                  // if we have a basionym creating relations is straight forward
                  NubUsage basionym = null;
                  if (group.hasBasionym()) {
                    basionym = group.getBasionym();

                  } else if (group.getRecombinations().size() > 1) {
                    // we need to persistent a placeholder basionym to group the 2 or more recombinations
                    newBasionyms++;
                    basionym = createBasionymPlaceholder(fam, group);
                  }
                  // persistent basionym relations
                  if (basionym != null) {
                    for (NubUsage u : group.getRecombinations()) {
                      if (createBasionymRelationIfNotExisting(basionym.node, u.node)) {
                        newRelations++;
                        u.issues.add(NameUsageIssue.ORIGINAL_NAME_DERIVED);
                        db.store(u);
                      }
                    }
                  }
                }
              }
            }
          }
          tx.success();
        } catch (Exception e) {
          LOG.error("Error detecting basionyms for family {}", n.getProperty(NeoProperties.SCIENTIFIC_NAME, "no name"), e);
        }
      }
      LOG.info("Discovered {} new basionym relations and created {} basionym placeholders", newRelations, newBasionyms);

    } catch (Throwable e) {
      LOG.error("Error detecting basionyms", e);
    }
  }

  private NubUsage createBasionymPlaceholder(NubUsage family, BasionymGroup group) {
    NubUsage basionym = new NubUsage();
    basionym.datasetKey = null;
    basionym.origin = Origin.BASIONYM_PLACEHOLDER;
    basionym.rank = Rank.UNRANKED;
    basionym.status = TaxonomicStatus.DOUBTFUL;
    basionym.parsedName = new ParsedName();
    basionym.parsedName.setGenusOrAbove("?");
    basionym.parsedName.setSpecificEpithet(group.getEpithet());
    basionym.parsedName.setAuthorship(group.getAuthorship());
    basionym.parsedName.setYear(group.getYear());
    basionym.parsedName.setType(NameType.PLACEHOLDER);
    basionym.parsedName.setScientificName(basionym.parsedName.fullName());
    LOG.debug("creating basionym placeholder {} in family {}", basionym.parsedName.canonicalNameComplete(), family.parsedName.canonicalName());
    return db.addUsage(family, basionym);
  }

  private void addKingdoms() {
    try (Transaction tx = db.beginTx()) {
      LOG.info("Adding kingdom");
      currSrc = new ClbSource(null, Constants.NUB_DATASET_KEY, "Backbone kingdoms");
      for (Kingdom k : Kingdom.values()) {
        NubUsage ku = new NubUsage();
        ku.usageKey = k.nubUsageID();
        ku.kingdom = k;
        ku.datasetKey = Constants.NUB_DATASET_KEY;
        ku.origin = Origin.SOURCE;
        ku.rank = Rank.KINGDOM;
        ku.status = TaxonomicStatus.ACCEPTED;
        ku.parsedName = new ParsedName();
        ku.parsedName.setType(NameType.SCIENTIFIC);
        ku.parsedName.setGenusOrAbove(k.scientificName());
        ku.parsedName.setScientificName(k.scientificName());
        // treat incertae sedis placeholder kingdom different
        if (k == Kingdom.INCERTAE_SEDIS) {
          ku.status = TaxonomicStatus.DOUBTFUL;
          ku.parsedName.setType(NameType.PLACEHOLDER);
        }
        db.addRoot(ku);
      }
      tx.success();
    }
  }


  /**
   * TODO: to be implemented.
   * Now clb still dynamically retrieves extension data from all checklists, but in the future we like to control
   * which extension record is attached to a backbone usage.
   * Adds all extension data, e.g. vernacular names, to the backbone directly.
   * TODO:
   * - build map from source usage key to nub node id
   * - stream (jdbc copy) through all extension data in postgres and attach to relevant nub node
   */
  private void addExtensionData() {
    LOG.warn("NOT IMPLEMENTED: Copy extension data to backbone");
    //Joiner commaJoin = Joiner.on(", ").skipNulls();
    //for (Node n : IteratorUtil.loop(db.allTaxa())) {
    //    NubUsage nub = read(n);
    //    if (!nub.sourceIds.isEmpty()) {
    //        LOG.debug("Add extension data from source ids {}", commaJoin.join(nub.sourceIds));
    //    }
    //}
  }

  /**
   * Flags emtpy genera or removes them if they have an IMPLICIT origin
   */
  private void flagEmptyGenera() {
    LOG.info("flag empty genera as doubtful");
    try (Transaction tx = db.beginTx()) {
      for (Node gen : IteratorUtil.loop(db.dao().allGenera())) {
        if (!gen.hasRelationship(RelType.PARENT_OF, Direction.OUTGOING)) {
          NubUsage nub = read(gen);
          if (nub.origin == Origin.IMPLICIT_NAME) {
            // remove this genus as it was created by the nub builder as an implicit genus name for a species we seem to have moved or deleted since
            if (removeTaxonIfEmpty(nub)) {
              continue;
            }
          }
          if (!nub.status.isSynonym()) {
            nub.issues.add(NameUsageIssue.NO_SPECIES);
            if (TaxonomicStatus.ACCEPTED == nub.status) {
              nub.status = TaxonomicStatus.DOUBTFUL;
            }
            db.store(nub);
          }
        }
      }
      tx.success();
    }
  }

  /**
   * Updates implicit names to be accepted (not doubtful) and removes implicit taxa with no children if configured to do so.
   */
  private void cleanImplicitTaxa() {
    LOG.info("Clean implicit taxa");
    try (Transaction tx = db.beginTx()) {
      for (Node n : IteratorUtil.loop(db.dao().allImplicitNames())) {
        NubUsage nub = read(n);
        if (!cfg.keepEmptyImplicitNames) {
          if (removeTaxonIfEmpty(nub)) {
            nub = null;
          }
        }
        // update status if still existing
        if (nub != null) {
          if (nub.status == TaxonomicStatus.DOUBTFUL) {
            nub.status = TaxonomicStatus.ACCEPTED;
            db.store(nub);
          }
        }
      }
      tx.success();
    }
  }

  /**
   * Goes through all accepted species and infraspecies and makes sure the name matches the genus, species classification.
   * For example an accepted species Picea alba with a parent genus of Abies is taxonomic nonsense.
   * Badly classified names are assigned the doubtful status and an NameUsageIssue.NAME_PARENT_MISMATCH is flagged
   */
  private void flagParentMismatch() {
    LOG.info("flag classification name mismatches");
    try (Transaction tx = db.beginTx()) {
      for (Node gn : IteratorUtil.loop(db.dao().allGenera())) {
        if (!gn.hasLabel(Labels.SYNONYM)) {
          NubUsage gen = read(gn);
          if (gen.kingdom == Kingdom.VIRUSES) {
            // virus names are unparsable...
            continue;
          }
          if (gen.parsedName == null || gen.parsedName.getGenusOrAbove() == null) {
            LOG.warn("Genus {} without genus name part: {} {}", gn, gen.rank, NeoProperties.getScientificName(gn));
            continue;
          }
          String genus = gen.parsedName.getGenusOrAbove();

          // flag non matching names
          for (Node spn : Traversals.CHILDREN.traverse(gn).nodes()) {
            NubUsage sp = db.dao().readNub(spn);
            if (sp.rank != Rank.SPECIES) {
              LOG.warn("Genus child {} is not a species: {} {}", spn, sp.rank, NeoProperties.getScientificName(spn));
              continue;
            }
            if (sp.parsedName == null || sp.parsedName.getGenusOrAbove() == null) {
              LOG.warn("Genus child {} without genus name part: {} {}", spn, sp.rank, NeoProperties.getScientificName(spn));
              continue;
            }
            if (!genus.equals(sp.parsedName.getGenusOrAbove())){
              sp.issues.add(NameUsageIssue.NAME_PARENT_MISMATCH);
            }
            db.store(sp);

            // check infraspecific names
            String species = sp.parsedName.getSpecificEpithet();
            for (Node ispn : Traversals.CHILDREN.traverse(spn).nodes()) {
              NubUsage isp = db.dao().readNub(ispn);
              if (isp.parsedName.getInfraSpecificEpithet() == null) {
                LOG.warn("Species child {} without an infraspecific epithet: {} {}", ispn, isp.rank, NeoProperties.getScientificName(ispn));
                continue;
              }
              if (!genus.equals(isp.parsedName.getGenusOrAbove()) || !species.equals(isp.parsedName.getInfraSpecificEpithet())){
                isp.issues.add(NameUsageIssue.NAME_PARENT_MISMATCH);
                db.store(isp);
              }
            }
          }
        }
      }
    }
  }

  private void addDatasets() {
    LOG.info("Start adding backbone sources");
    for (NubSource src : sources) {
      try {
        addDataset(src);
      } catch (Exception e) {
        LOG.error("Error processing source {}", src.name, e);
      } finally {
        Stopwatch sw = Stopwatch.createStarted();
        src.close();
        LOG.debug("Closing source {} took {}ms", src.name, sw.elapsed(TimeUnit.MILLISECONDS));
      }
    }
  }

  private void addDataset(NubSource source) {
    LOG.info("Adding {}th source {}", datasetCounter++, source.name);
    currSrc = source;
    priorities.put(source.key, ++maxPriority);
    // clear dataset wide caches
    parents.clear();
    basionymRels.clear();
    src2NubKey.clear();
    allowedRanks.clear();
    // prepare set of allowed ranks for this source
    for (Rank r : Rank.values()) {
      if (NUB_RANKS.contains(r) && r.ordinal() >= source.ignoreRanksAbove.ordinal()) {
        allowedRanks.add(r);
      }
    }
    int start = sourceUsageCounter;

    // do transactions in batches to dont slow down neo too much
    int batchCounter = 1;
    // makes sure to close the iterator - important for releasing neo resources, slows down considerably otherwise!
    try (CloseableIterator<SrcUsage> iter = source.iterator()) {
      UnmodifiableIterator<List<SrcUsage>> batchIter = Iterators.partition(iter, cfg.neo.batchSize);
      while (batchIter.hasNext()) {
        try (Transaction tx = db.beginTx()) {
          List<SrcUsage> batch = batchIter.next();
          LOG.debug("process batch {} with {} usages", batchCounter++, batch.size());
          for (SrcUsage u : batch) {
            // catch errors processing individual records too
            try {
              LOG.debug("process {} {} {}", u.status, u.rank, u.scientificName);
              sourceUsageCounter++;
              parents.add(u);

              NubUsage parent = parents.nubParent();
              // replace accepted taxa with doubtful ones for all nomenclators and for synonym parents
              // http://dev.gbif.org/issues/browse/POR-2780
              if (TaxonomicStatus.ACCEPTED == u.status && (currSrc.nomenclator || parent.status.isSynonym())) {
                u.status = TaxonomicStatus.DOUBTFUL;
              }
              if (parent.status.isSynonym()) {
                // use accepted instead
                parent = db.parent(parent);
              }
              NubUsage nub = processSourceUsage(u, Origin.SOURCE, parent);
              if (nub != null) {
                parents.put(nub);
              }
            } catch (IgnoreSourceUsageException e) {
              LOG.debug("Ignore usage {} >{}< {}", u.key, u.scientificName, e.getMessage());

            } catch (StackOverflowError e) {
              // if this happens its time to fix some code!
              LOG.error("CODE BUG: StackOverflowError processing {} from source {}", u.scientificName, source.name, e);
              LOG.error("CAUSE: {}", u.parsedName);

            } catch (RuntimeException e) {
              LOG.error("RuntimeException processing {} from source {}", u.scientificName, source.name, e);
            }
          }
          tx.success();
        }
      }
    } catch (Exception e) {
      Throwables.propagate(e);
    }

    // process explicit basionym relations
    processExplicitBasionymRels();

    LOG.info("Processed {} source usages for {}", sourceUsageCounter - start, source.name);
  }

  private void processExplicitBasionymRels() {
    try (Transaction tx = db.beginTx()) {
      LOG.info("Processing {} explicit basionym relations from {}", basionymRels.size(), currSrc.name);
      for (LongIntCursor c : basionymRels) {
        Node n = db.getNode(c.key);
        Node bas = db.getNode(src2NubKey.get(c.value));
        // find basionym node by sourceKey
        if (n != null && bas != null) {
          // basionym has not been verified yet, make sure its of rank <= genus and its name type is no placeholder
          NubUsage basUsage = read(bas);
          if (!basUsage.rank.isSpeciesOrBelow()) {
            LOG.warn("Ignore explicit basionym {} of rank {}", basUsage.parsedName.getScientificName(), basUsage.rank);
            continue;
          }
          if (!basUsage.parsedName.getType().isBackboneType()) {
            LOG.warn("Ignore explicit basionym {} with name type {}", basUsage.parsedName.getScientificName(), basUsage.parsedName.getType());
            continue;
          }
          if (!createBasionymRelationIfNotExisting(bas, n)) {
            LOG.warn("Nub usage {} already contains a contradicting basionym relation. Ignore basionym {} from source {}", n.getProperty(NeoProperties.SCIENTIFIC_NAME, n.getId()), bas.getProperty(NeoProperties.SCIENTIFIC_NAME, bas.getId()), currSrc.name);
          }
        } else {
          LOG.warn("Could not resolve basionym relation for nub {} to source usage {}", c.key, c.value);
        }
      }
      tx.success();
    }
  }

  /**
   * Looks for all implicit names and tries to find a match with author to replace them.
   * This is done after we added an entire dataset so we can prefer accepted names over doubtful ones.
   * If we instead would do this on the fly as we add new names we might prefer a doubtful name.
   */
  private void replaceImplicitNames() {
    LOG.info("Replace implicit names for dataset {}", currSrc.name);
    try (Transaction tx = db.beginTx()) {
      for (Node n : IteratorUtil.loop(db.dao().allImplicitNames())) {
        NubUsage nub = read(n);
        try {
          NubUsageMatch match = db.findNubUsage(nub.datasetKey, nub.parsedName, nub.rank, nub.status, nub.kingdom, db.parent(nub));
          if (match.isMatch()) {
            // replace implicit name with match
            LOG.debug("Replace implicit name {} with {}", nub.parsedName.fullName(), match.usage.parsedName.fullName());
            //db.store(nub);
          }
        } catch (IgnoreSourceUsageException e) {
          LOG.warn(e.name, e);
        }
      }
      tx.success();
    }
  }

  private void swapName(NubUsage target, NubUsage source) {
    db.transferChildren(target, source);
  }


  /**
   * @return true if basionym relationship was created
   */
  private boolean createBasionymRelationIfNotExisting(Node basionym, Node n) {
    if (!basionym.equals(n) && !n.hasRelationship(RelType.BASIONYM_OF, Direction.BOTH)) {
      basionym.createRelationshipTo(n, RelType.BASIONYM_OF);
      basionym.addLabel(Labels.BASIONYM);
      return true;
    }
    return false;
  }

  private NubUsage processSourceUsage(SrcUsage u, Origin origin, NubUsage parent) throws IgnoreSourceUsageException {
    Preconditions.checkNotNull(u.status);
    Preconditions.checkArgument(parent.status.isAccepted());
    // try to parse name
    addParsedNameIfNull(u);
    // match to existing usages
    NubUsageMatch match = db.findNubUsage(currSrc.key, u, parents.nubKingdom(), parent);

    // process only usages not to be ignored and with desired ranks
    if (!match.ignore && u.rank != null && allowedRanks.contains(u.rank)) {
      // from now on a rank is guaranteed!

      if (!match.isMatch()) {

        // remember if we had a doubtful match
        NubUsage doubtful = match.doubtfulUsage;
        // persistent new nub usage if there wasnt any yet
        match = createNubUsage(u, origin, parent);
        // check if we had a doubtful or implicit & accepted name match
        if (doubtful != null && u.status == TaxonomicStatus.ACCEPTED) {
          db.transferChildren(match.usage, doubtful);
        }

      } else {
        if (origin == Origin.IMPLICIT_NAME) {
          // do not update or change usages with implicit names
          return match.usage;
        }

        Equality authorEq = authorComparator.compare(match.usage.parsedName, u.parsedName);

        if (match.usage.status.isSynonym() == u.status.isSynonym()) {
          // update nub usage if status matches
          updateNub(match.usage, u, origin, parent);

        } else if (Equality.DIFFERENT == authorEq) {
          // persistent new nub usage with different status and authorship as before
          match = createNubUsage(u, origin, parent);

        } else if (fromCurrentSource(match.usage) && !u.status.isSynonym()) {
          // prefer accepted over synonym if from the same source
          LOG.debug("prefer accepted {} over synonym usage from the same source", u.scientificName);
          delete(match.usage);
          match = createNubUsage(u, origin, parent);

        } else if (fromCurrentSource(match.usage) && u.parsedName.hasAuthorship() && Equality.EQUAL != authorEq) {
          // allow new synonyms with non equal authorship to be created
          match = createNubUsage(u, origin, parent);

        } else if (currSrc.nomenclator) {
          updateNomenclature(match.usage, u);

        } else {
          LOG.debug("Ignore source usage. Status {} is different from nub ({}): {}", u.status, match.usage.status, u.scientificName);
        }
      }

      if (match.isMatch()) {
        if (u.key != null) {
          // remember all original source usage key to nub id mappings per dataset
          src2NubKey.put(u.key, match.usage.node.getId());
        }
        if (u.originalNameKey != null) {
          // remember basionym relation.
          // Basionyms do not follow the taxnomic hierarchy, so we might not have seen some source keys yet
          // we will process all basionyms at the end of each source dataset
          basionymRels.put(match.usage.node.getId(), u.originalNameKey);
        }
      }
    } else {
      LOG.debug("Ignore {} source usage: {}", u.rank, u.scientificName);
    }
    return match.usage;
  }

  private void delete(NubUsage nub) {
    for (IntCursor sourceId : nub.sourceIds) {
      src2NubKey.remove(sourceId.value);
    }
    basionymRels.remove(nub.node.getId());
    db.dao().delete(nub);
  }

  /**
   * Removes a taxon if it has no accepted children or synonyms
   * @return true if usage was deleted
   */
  private boolean removeTaxonIfEmpty(NubUsage u) {
    if (u != null &&
        !u.node.hasRelationship(Direction.INCOMING, RelType.SYNONYM_OF, RelType.PROPARTE_SYNONYM_OF) &&
        !u.node.hasRelationship(Direction.OUTGOING, RelType.PARENT_OF)
        ) {
      delete(u);
      return true;
    }
    return false;
  }

  private NubUsageMatch createNubUsage(SrcUsage u, Origin origin, NubUsage p) throws IgnoreSourceUsageException {
    addParsedNameIfNull(u);
    // if this is a synonym but the parent is not part of the nub (e.g. cause its a placeholder name) ignore it!
    // http://dev.gbif.org/issues/browse/POR-2990
    if (u.status.isSynonym() && !parents.parentInNub()) {
      throw new IgnoreSourceUsageException("Ignoring synonym as accepted parent is not part of the nub", u.scientificName);
    }
    // ignore synonyms of low rank for higher taxa
    // http://dev.gbif.org/issues/browse/POR-3169
    if (u.status.isSynonym() && !u.rank.higherThan(Rank.GENUS) && p.rank.higherThan(Rank.FAMILY)) {
      String message = String.format("Ignoring %s synonym %s for %s %s", u.rank, u.scientificName, p.rank, p.parsedName.fullName());
      throw new IgnoreSourceUsageException(message, u.scientificName);
    }
    // make sure parent is accepted
    if (p.status.isSynonym()) {
      LOG.warn("Parent {} of {} is a synonym", p.parsedName.canonicalNameComplete(), u.parsedName.canonicalNameComplete());
      throw new IllegalStateException("Parent is a synonym"+u.scientificName);
    }

    // make sure we have a parsed genus to deal with implicit names and the kingdom is not viruses as these have no structured name
    if (p.kingdom != Kingdom.VIRUSES) {

      if (u.status.isAccepted()) {
        // skip badly organized rank hierarchies
        if (!p.rank.higherThan(u.rank)) {
          LOG.warn("Source {} {} with inversed parent {} {}", u.rank, u.scientificName, p.rank, p.parsedName.canonicalNameComplete());
          throw new IgnoreSourceUsageException("Ignoring source with inverted rank order", u.scientificName);
        }

        // we want the parent of any infraspecies ranks to be the species
        if (p.rank.isInfraspecific()) {
          p = findParentSpecies(p);
        }

        // check if implicit species or genus parents are needed
        SrcUsage implicit = new SrcUsage();
        NubUsage implicitParent = null;
        try {
          if (u.parsedName.getGenusOrAbove() != null) {
            if (u.rank == Rank.SPECIES && p.rank != Rank.GENUS) {
              implicit.rank = Rank.GENUS;
              implicit.scientificName = u.parsedName.getGenusOrAbove();
              implicit.status = TaxonomicStatus.DOUBTFUL;
              implicitParent = processSourceUsage(implicit, Origin.IMPLICIT_NAME, p);

            } else if (u.rank.isInfraspecific() && p.rank != Rank.SPECIES) {
              implicit.rank = Rank.SPECIES;
              implicit.scientificName = u.parsedName.canonicalSpeciesName();
              implicit.status = TaxonomicStatus.DOUBTFUL;
              implicitParent = processSourceUsage(implicit, Origin.IMPLICIT_NAME, p);
            }
          } else {
            LOG.warn("Missing genus in parsed name for {}", u.scientificName);
          }

        } catch (IgnoreSourceUsageException e) {
          LOG.warn("Ignore implicit {} {}", implicit.rank, implicit.scientificName);

        } catch (Exception e) {
          LOG.error("Failed to persistent implicit {} {}", implicit.rank, implicit.scientificName, e);
        }

        if (implicitParent != null) {
          // in case the implicit parent species is a synonym turn the infraspecies also into a synonym
          if (implicitParent.status.isSynonym() && implicitParent.rank == Rank.SPECIES) {
            // http://dev.gbif.org/issues/browse/POR-2780
            u.status = TaxonomicStatus.SYNONYM;
            p = db.parent(implicitParent);
          } else {
            // use the implicit parent
            p = implicitParent;
          }
        }

      } else {
        // a synonym
        // avoid cases where synonyms for a binnomial are monomials of rank genus or even higher!
        if (p.parsedName.isBinomial() && u.rank.ordinal() < Rank.INFRAGENERIC_NAME.ordinal()) {
          LOG.warn("Source synonym {} {} with accepted binomial name {} {}", u.rank, u.scientificName, p.rank, p.parsedName.canonicalNameComplete());
          throw new IgnoreSourceUsageException("Ignoring source with inverted rank order", u.scientificName);
        }

      }
    }
    // add to nub db
    return NubUsageMatch.match(db.addUsage(p, u, origin, currSrc.key));
  }

  /**
   * moves up the parent_of rels to the species or first taxon above.
   * Returns original usage in case rank was at species level or above already
   */
  private NubUsage findParentSpecies(NubUsage p) {
    while (p.rank.isInfraspecific()) {
      p = db.parent(p);
    }
    return p;
  }

  private void addParsedNameIfNull(SrcUsage u) throws IgnoreSourceUsageException {
    if (u.parsedName == null) {
      try {
        u.parsedName = parser.parse(u.scientificName, u.rank);
        // avoid indet names
        if (ignoredNameTypes.contains(u.parsedName.getType())) {
          throw new IgnoreSourceUsageException("Ignore " + u.parsedName.getType() + " name", u.scientificName);
        }
        // avoid incomplete names
        if ((!Strings.isBlank(u.parsedName.getInfraSpecificEpithet()) && Strings.isBlank(u.parsedName.getSpecificEpithet()))
            || !Strings.isBlank(u.parsedName.getSpecificEpithet()) && Strings.isBlank(u.parsedName.getGenusOrAbove())) {
          throw new IgnoreSourceUsageException("Ignore incomplete name", u.scientificName);
        }
        // avoid taxon concept names
        if (!Strings.isBlank(u.parsedName.getSensu())) {
          throw new IgnoreSourceUsageException("Ignore taxon concept names", u.scientificName);
        }
        // avoid names with nulls in epithets
        if ("null".equals(u.parsedName.getSpecificEpithet()) || "null".equals(u.parsedName.getInfraSpecificEpithet())) {
          throw new IgnoreSourceUsageException("Ignore names with null epithets", u.scientificName);
        }
        // consider parsed rank only for bi/trinomials
        Rank pRank = u.parsedName.isBinomial() ? u.parsedName.getRank() : null;
        if (pRank != null && pRank != u.rank && !pRank.isUncomparable()) {
          if (u.rank == null) {
            LOG.debug("Use parsed rank {}", pRank);
            u.rank = pRank;
          } else if (u.rank.isUncomparable()) {
            LOG.debug("Prefer parsed rank {} over {}", pRank, u.rank);
            u.rank = pRank;
          } else {
            LOG.debug("Rank {} does not match parsed rank {}. Ignore {}", u.rank, pRank, u.scientificName);
            throw new IgnoreSourceUsageException("Parsed rank mismatch", u.scientificName);
          }
        } else if (Rank.INFRAGENERIC_NAME ==  u.rank && u.parsedName.isBinomial()) {
          // this is an aggregate species rank as we have a binomial & rank=INFRAGENERIC - treat as a species!
          u.rank = Rank.SPECIES;
          LOG.debug("Treat infrageneric name {} as species", u.scientificName);
        }

        // strip author names from higher taxa
        if (u.rank != null && u.rank.higherThan(Rank.GENUS)) {
          clearAuthorship(u.parsedName);
        }

        //TODO: rebuild name in canonical form - e.g. removes subgenus references
        //u.parsedName.setScientificName(u.parsedName.canonicalNameComplete());


      } catch (UnparsableException e) {
        // allow virus names in the nub
        if (e.type == NameType.VIRUS) {
          u.parsedName = new ParsedName();
          u.parsedName.setScientificName(u.scientificName);
          u.parsedName.setType(e.type);
        } else {
          throw new IgnoreSourceUsageException("Unparsable " + e.type, u.scientificName);
        }
      }
    }
  }

  private void clearAuthorship(ParsedName pn) {
    pn.setAuthorship(null);
    pn.setYear(null);
    pn.setBracketAuthorship(null);
    pn.setBracketYear(null);
  }

  private void updateNomenclature(NubUsage nub, SrcUsage u) {
    LOG.debug("Updating nomenclature for {} from source {}", nub.parsedName.getScientificName(), u.parsedName.getScientificName());
    // authorship
    if (!u.parsedName.authorshipComplete().isEmpty() && (nub.parsedName.authorshipComplete().isEmpty() || currSrc.nomenclator)) {
      nub.parsedName.setAuthorship(u.parsedName.getAuthorship());
      nub.parsedName.setYear(u.parsedName.getYear());
      nub.parsedName.setBracketAuthorship(u.parsedName.getBracketAuthorship());
      nub.parsedName.setBracketYear(u.parsedName.getBracketYear());
      nub.parsedName.setAuthorsParsed(true);
      nub.parsedName.setScientificName(u.parsedName.canonicalNameComplete());
    }

    // publishedIn
    if (u.publishedIn != null && (nub.publishedIn == null || currSrc.nomenclator)) {
      nub.publishedIn = u.publishedIn;
    }

    // nom status
    if (u.nomStatus != null && u.nomStatus.length > 0 && (nub.nomStatus.isEmpty() || currSrc.nomenclator)) {
      nub.nomStatus = Sets.newHashSet(u.nomStatus);
    }
  }

  private void updateNub(NubUsage nub, SrcUsage u, Origin origin, NubUsage parent) {
    LOG.debug("Updating {} from source {}", nub.parsedName.getScientificName(), u.parsedName.getScientificName());
    NubUsage currNubParent = db.parent(nub);

    // update nomenclature and status only from source usages
    if (u.key != null) {
      nub.sourceIds.add(u.key);
      // update author, publication and nom status
      updateNomenclature(nub, u);
      // prefer accepted version over doubtful if its coming from the same dataset!
      if (nub.status == TaxonomicStatus.DOUBTFUL && u.status == TaxonomicStatus.ACCEPTED && fromCurrentSource(nub)) {
        nub.status = u.status;
        if (isNewParentApplicable(nub, currNubParent, parent) && !db.existsInClassification(currNubParent.node, parent.node, false)) {
          // current classification doesnt have that parent yet, lets apply it
          LOG.debug("Update doubtful {} classification with new parent {} {}", nub.parsedName.getScientificName(), parent.rank, parent.parsedName.getScientificName());
          db.createParentRelation(nub, parent);
        }
      }
      if (origin == Origin.SOURCE) {
        // only override original origin value if we update from a true source
        nub.origin = Origin.SOURCE;
      }
    }

    if (nub.status.isSynonym()) {
      // maybe we have a proparte synonym from the same dataset?
      if (fromCurrentSource(nub) && !parent.node.equals(currNubParent.node)) {
        nub.status = TaxonomicStatus.PROPARTE_SYNONYM;
        // persistent new pro parte relation
        LOG.debug("New accepted name {} found for pro parte synonym {}", parent.parsedName.getScientificName(), nub.parsedName.getScientificName());
        db.setSingleFromRelationship(nub.node, parent.node, RelType.PROPARTE_SYNONYM_OF);

      } else {
        // this might be a more exact kind of synonym status
        if (nub.status == TaxonomicStatus.SYNONYM) {
          nub.status = u.status;
        }
      }

    } else {
      // ACCEPTED
      if (isNewParentApplicable(nub, currNubParent, parent) &&
          (currNubParent.kingdom == Kingdom.INCERTAE_SEDIS
            || db.existsInClassification(parent.node, currNubParent.node, false)  && currNubParent.rank != parent.rank
          )
        ) {
        LOG.debug("Update {} classification with new parent {} {}", nub.parsedName.getScientificName(), parent.rank, parent.parsedName.getScientificName());
        db.createParentRelation(nub, parent);
      }
    }
    db.store(nub);
  }

  private boolean isNewParentApplicable(NubUsage nub, NubUsage currParent, NubUsage newParent) {
    return newParent != null
        && !currParent.equals(newParent)
        && (currParent.rank.higherThan(newParent.rank) || currParent.rank == newParent.rank)
        && newParent.rank.higherThan(nub.rank);
  }

  private boolean fromCurrentSource(NubUsage nub) {
    return nub.datasetKey.equals(currSrc.key);
  }

  private void groupByBasionym() {
    if (cfg.groupBasionyms) {
      LOG.info("Start basionym consolidation");
      verifyBasionyms();
      detectBasionyms();
      consolidateBasionymGroups();
    } else {
      LOG.info("Skip basionym consolidation");
    }
  }

  /**
   * Verifies existing basionyms by checking that the basionym does not have an original author
   */
  private void verifyBasionyms() {
    LOG.info("Verify existing basionyms - TO BE IMPLEMENTED!");
  }

  /**
   * Make sure we only have at most one accepted name for each homotypical basionym group!
   * An entire group can consist of synonyms without a problem, but they must all refer to the same accepted name.
   * If a previously accepted name needs to be turned into a synonym it will be of type homotypical_synonym.
   *
   * As we merge names from different taxonomies it is possible there are multiple accepted names (maybe via a synonym relation) in such a group.
   * We always stick to the first combination with the highest priority and make all others
   * a) synonyms of this if it is accepted
   * b) synonyms of the primary's accepted name if it was a synonym itself
   *
   * In case of conflicting accepted names we also flag these names with CONFLICTING_BASIONYM_COMBINATION
   */
  private void consolidateBasionymGroups() {
    int counter = 0;
    int counterModified = 0;
    // first load all basionym node ids into a set so we can process them individually in separate transactions
    LongHashSet basIds = new LongHashSet();
    try (Transaction tx = db.beginTx()) {
      for (Node bas : IteratorUtil.loop(db.dao().allBasionyms())) {
        basIds.add(bas.getId());
      }
      LOG.info("Found {} basionyms to consolidate", basIds.size());
    }
    // now consolidate each basionym group in its own transaction
    for (LongCursor basCursor : basIds) {
      try (Transaction tx = db.beginTx()) {
        Node bas = db.getNode(basCursor.value);

        counter++;
        // sort all usage by source dataset priority, placing doubtful names last
        List<NubUsage> group = db.listBasionymGroup(bas);
        if (group.size() > 1) {
          // we stick to the first combination with the highest priority and make all others
          //  a) synonyms of this if it is accepted
          //  b) synonyms of the primary's accepted name if it was a synonym itself
          // if there are several usages with the same priority select one according to some defined rules
          final NubUsage primary = findPrimaryUsage(group);
          // get the accepted usage in case of synonyms
          final NubUsage accepted = primary.status.isSynonym() ? db.parent(primary) : primary;
          final TaxonomicStatus synStatus = primary.status.isSynonym() ? primary.status : TaxonomicStatus.HOMOTYPIC_SYNONYM;
          Set<Node> parents = ImmutableSet.copyOf(db.parents(accepted.node));

          LOG.debug("Consolidating basionym group with {} primary usage {}: {}", primary.status, primary.parsedName.canonicalNameComplete(), names(group));
          int modified = 0;
          for (NubUsage u : group) {
            if (u.equals(primary)) continue;
            if (parents.contains(u.node)) {
              LOG.debug("Exclude parent {} from basionym consolidation of {}", u.parsedName.canonicalNameComplete(), primary.parsedName.canonicalNameComplete());

            } else if (!hasAccepted(u, accepted)) {
              modified++;
              NubUsage previousParent = db.parent(u);
              if (previousParent != null) {
                u.addRemark(String.format("Originally found in sources as %s %s %s", u.status.toString().toLowerCase().replaceAll("_", " "),
                    u.status.isSynonym() ? "of" : "taxon within", previousParent.parsedName.canonicalNameComplete())
                );
              }
              db.convertToSynonym(u, accepted, synStatus, NameUsageIssue.CONFLICTING_BASIONYM_COMBINATION);
            }
          }
          counterModified = counterModified + modified;
        }
        tx.success();

      } catch (NotFoundException e) {
        LOG.info("Basionym {} was removed. Ignore for consolidation", basCursor.value, e);
      }
    }
    LOG.info("Consolidated {} usages from {} basionyms in total", counterModified, counter);
  }

  private int priority(NubUsage usage) {
    return priorities.containsKey(usage.datasetKey) ? priorities.get(usage.datasetKey) : maxPriority + 1;
  }

  /**
   * From a list of equally trusted usages of a basionym group select one primary usage to trust most.
   * Selection follows the order:
   * 1) the accepted usage which most usages link to (from a synonym)
   * 2) status synonym > accepted > doubtful
   * 3) highest rank
   */
  private NubUsage findPrimaryUsage(List<NubUsage> basionymGroup) {
    if (basionymGroup == null || basionymGroup.isEmpty()) {
      return null;
    }
    // a single usage only
    if (basionymGroup.size() == 1) {
      return basionymGroup.get(0);
    }
    // keep shringing this list until we get one!
    List<NubUsage> candidates = Lists.newArrayList();

    // 1. by dataset priority
    int highestPriority = Integer.MAX_VALUE;
    for (NubUsage u : basionymGroup) {
      int datasetPriority = priority(u);
      if (datasetPriority < highestPriority) {
        highestPriority = datasetPriority;
      }
    }
    for (NubUsage u : basionymGroup) {
      if (priority(u) == highestPriority) {
        candidates.add(u);
      }
    }

    if (candidates.size() > 1) {
      // 2. accepted with most usages
      Map<NubUsage, NubUsage> u2acc= Maps.newHashMap();
      Map<NubUsage, Integer> accCounts = Maps.newHashMap();
      for (NubUsage u : candidates) {
        final NubUsage accepted = u.status.isSynonym() ? db.parent(u) : u;
        u2acc.put(u, accepted);
        if (!accCounts.containsKey(accepted)) {
          accCounts.put(accepted, 1);
        } else {
          accCounts.put(accepted, accCounts.get(accepted)+1);
        }
      }
      int maxCount = MapUtils.sortByValue(accCounts, Ordering.<Integer>natural().reverse()).values().iterator().next();
      Iterator<NubUsage> iter = candidates.iterator();
      while (iter.hasNext()) {
        NubUsage u = iter.next();
        if (accCounts.get(u2acc.get(u)) != maxCount) {
          iter.remove();
        }
      }

      if (candidates.size() > 1) {
        // 3. by status: syn > acc > doubtful
        Map<NubUsage, Integer> score = Maps.newHashMap();
        for (NubUsage u : candidates) {
          score.put(u, MapUtils.getOrDefault(STATUS_ORDER, u.status, 10));
        }
        int maxScore = MapUtils.sortByValue(score).values().iterator().next();
        iter = candidates.iterator();
        while (iter.hasNext()) {
          NubUsage u = iter.next();
          if (score.get(u) != maxScore) {
            iter.remove();
          }
        }

        if (candidates.size() > 1) {
          // 4. by higher rank
          List<Rank> ranks = Lists.newArrayList();
          for (NubUsage u : candidates) {
            ranks.add(u.rank);
          }
          Collections.sort(ranks);
          Rank minRank = ranks.get(0);
          iter = candidates.iterator();
          while (iter.hasNext()) {
            NubUsage u = iter.next();
            if (minRank != u.rank) {
              iter.remove();
            }
          }
        }
      }
    }

    return candidates.get(0);
  }

  /**
   * @return true of the given usage u has a SYNONYM_OF relation to the given acc usage
   */
  private boolean hasAccepted(NubUsage u, NubUsage acc) {
    if (u.node.equals(acc.node)) return true;
    try (ResourceIterator<Node> iter = Traversals.ACCEPTED.traverse(u.node).nodes().iterator()) {
      while (iter.hasNext()) {
        if (iter.next().equals(acc.node)) {
          return true;
        }
      }
    }
    return false;
  }

  private String names(Collection<NubUsage> usages) {
    return SEMICOLON_JOIN.join(Iterables.transform(usages, new Function<NubUsage, String>() {
      @Nullable
      @Override
      public String apply(NubUsage u) {
        return u.parsedName.fullName();
      }
    }));
  }

  /**
   * Assigns a unique usageKey to all nodes by matching a usage to the previous backbone to keep stable identifiers.
   */
  private void assignUsageKeys() {
    LOG.info("Assigning final clb ids to all nub usages...");
    for (Map.Entry<Long, NubUsage> entry : db.dao().nubUsages()) {
      NubUsage u = entry.getValue();
      if (u.rank != Rank.KINGDOM) {
        u.usageKey = idGen.issue(u.parsedName.canonicalName(), u.parsedName.getAuthorship(), u.parsedName.getYear(), u.rank, u.kingdom, false);
        db.dao().update(entry.getKey(), u);
      }
    }

    // for pro parte synonyms we need to assign extra keys, one per relation!
    // http://dev.gbif.org/issues/browse/POR-2872
    try (Transaction tx = db.beginTx()) {
      try (ResourceIterator<Relationship> rels = db.dao().listAllRelationships(RelType.PROPARTE_SYNONYM_OF)) {
        while (rels.hasNext()) {
          Relationship rel = rels.next();
          NubUsage u = db.dao().readNub(rel.getStartNode());
          int ppKey = idGen.issue(u.parsedName.canonicalName(), u.parsedName.getAuthorship(), u.parsedName.getYear(), u.rank, u.kingdom, true);
          LOG.debug("Assign extra id {} for pro parte relation of primary usage {}", ppKey, u.usageKey);
          rel.setProperty(NeoProperties.USAGE_KEY, ppKey);
        }
      }
      tx.success();
    }
  }

  private void builtUsageMetrics() {
    LOG.info("Walk all accepted taxa and build usage metrics");
    UsageMetricsHandler metricsHandler = new UsageMetricsHandler(db.dao());
    // TaxonWalker deals with transactions
    TreeWalker.walkAcceptedTree(db.dao().getNeo(), metricsHandler);
    NormalizerStats normalizerStats = metricsHandler.getStats(0, null);
    LOG.info("Walked all taxa (root={}, total={}, synonyms={}) and built usage metrics", normalizerStats.getRoots(), normalizerStats.getCount(), normalizerStats.getSynonyms());
  }

}
