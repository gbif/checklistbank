package org.gbif.checklistbank.nub;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.authorship.BasionymGroup;
import org.gbif.checklistbank.authorship.BasionymSorter;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.TaxonomicOrderExpander;
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
import org.gbif.checklistbank.utils.NameFormatter;
import org.gbif.checklistbank.utils.SciNameNormalizer;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.utils.collection.MapUtils;
import org.gbif.utils.file.FileUtils;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.helpers.collection.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * We use Origin.VERBATIM_ACCEPTED here to mark excluded homonyms that have not been updated with other sources.
 * In the future consider to add a new origin value to the API vocabulary.
 */
public class NubBuilder implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(NubBuilder.class);
  private static final Joiner SEMICOLON_JOIN = Joiner.on("; ").skipNulls();
  static final Set<Origin> IGNORABLE_ORIGINS = Sets.newHashSet(Origin.IMPLICIT_NAME, Origin.VERBATIM_ACCEPTED);
  public static final Set<Rank> NUB_RANKS;
  private static final ClbSource NUB_SRC = new ClbSource(null, null, Constants.NUB_DATASET_KEY, "Backbone algorithm", null);

  static {
    List<Rank> ranks = Lists.newArrayList(Rank.LINNEAN_RANKS);
    ranks.add(Rank.SUBSPECIES);
    ranks.add(Rank.VARIETY);
    ranks.add(Rank.FORM);
    ranks.remove(Rank.KINGDOM); // we only use kingdoms from our enum
    NUB_RANKS = ImmutableSet.copyOf(ranks);
  }

  private final Set<NameType> ignoredNameTypes = Sets.newHashSet(
      NameType.CANDIDATUS, NameType.CULTIVAR, NameType.INFORMAL, NameType.NO_NAME, NameType.PLACEHOLDER
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
  private final AuthorNormalizer authorNorm = AuthorNormalizer.create();
  private final NubDb db;
  private final NubSourceList sources;
  private final NubConfiguration cfg;
  private boolean closeDao = true;
  private NubSource currSrc;
  private ParentStack parents;
  private int sourceUsageCounter = 0;
  private final AuthorComparator authorComparator;
  private final IdGenerator idGen;
  private final Int2LongMap src2NubKey = new Int2LongOpenHashMap();
  private final Long2IntMap basionymRels = new Long2IntOpenHashMap(); // node.id -> src.usageKey
  private final Map<UUID, Integer> priorities = Maps.newHashMap();
  private Integer maxPriority = 0;
  private int datasetCounter = 1;

  private NubBuilder(UsageDao dao, NubSourceList sources, IdLookup idLookup, AuthorComparator authorComparator, int newIdStart, NubConfiguration cfg) {
    db = NubDb.create(dao, authorComparator);
    this.sources = sources;
    this.authorComparator = authorComparator;
    idGen = new IdGenerator(idLookup, newIdStart);
    this.cfg = cfg;
    cfg.normalizeConfigs();
  }

  public static NubBuilder create(NubConfiguration cfg) {
    UsageDao dao = UsageDao.persistentDao(cfg.neo, Constants.NUB_DATASET_KEY, null, true);
    try {
      IdLookupImpl idLookup = IdLookupImpl.temp().load(cfg.clb, true);
      return new NubBuilder(dao, ClbSourceList.create(cfg), idLookup, idLookup.getAuthorComparator(), idLookup.getKeyMax() + 1, cfg);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load existing backbone ids", e);
    }
  }

  /**
   * @param dao the dao to persistent the nub.
   */
  public static NubBuilder create(UsageDao dao, NubSourceList sources, IdLookup idLookup, int newIdStart, NubConfiguration cfg) {
    return new NubBuilder(dao, sources, idLookup, idLookup.getAuthorComparator(), newIdStart, cfg);
  }

  public void setCloseDao(boolean closeDao) {
    this.closeDao = closeDao;
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
      currSrc = NUB_SRC;

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

    } catch (AssertionError e) {
      LOG.error("Backbone invalid, build failed!", e);
      throw e;

    } catch (Throwable e) {
      LOG.error("Fatal error. Backbone build failed!", e);
      //db.dao().consistencyNubReport();
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
   * <p>
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
    for (Node n : Iterators.loop(iter)) {
      NubUsage nub = read(n);
      if (!StringUtils.isBlank(nub.parsedName.getAuthorship())) {
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
   * <p>
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
      for (Node gn : Iterators.loop(db.dao().allGenera())) {
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
    for (Node n : Iterators.loop(iter)) {
      if (!n.hasLabel(Labels.SYNONYM)) {
        NubUsage u = read(n);
        String normedName = db.dao().canonicalOrScientificName(u.parsedName);
        if (!StringUtils.isBlank(normedName)) {
          if (names.containsKey(normedName)) {
            u.issues.add(NameUsageIssue.ORTHOGRAPHIC_VARIANT);
            u.addRemark("Possible variant of " + names.get(normedName));
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
    Object2LongMap<String> names = new Object2LongOpenHashMap<>();
    for (Node n : nodes) {
      if (!n.hasLabel(Labels.SYNONYM)) {
        NubUsage u = read(n);
        String name = NameFormatter.canonicalOrScientificName(u.parsedName);
        if (u.status == TaxonomicStatus.ACCEPTED && !StringUtils.isBlank(name)) {
          // prefix with rank ordinal to become unique across ranks (ordinal is shorter than full name to save mem)
          String indexedName = u.rank.ordinal() + name;
          if (names.containsKey(indexedName)) {
            // duplicate accepted canonical name. Check which has priority
            Node n1 = db.getNode(names.get(indexedName));
            NubUsage u1 = read(n1);

            int p1 = priorities.get(u1.datasetKey);
            int p2 = priorities.get(u.datasetKey);

            if (p1==p2) {
              // both from the same source.
              // Check if one of them could be the type genus and prefer that, i.e. a similar named parent
              p1 = -wordStemSize(u1);
              p2 = -wordStemSize(u);
              LOG.info("Homonym from the same source {}, prefer {} over {}", u.datasetKey, p2<p1 ? u:u1, p2<p1 ? u1:u);
            }

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

  private int wordStemSize(NubUsage u){
    NubUsage p = db.parent(u);
    String common = StringUtils.getCommonPrefix(u.parsedName.getScientificName(), p.parsedName.getScientificName());
    return common.length();
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
        for (Node n : Iterators.loop(db.dao().allAutonyms())) {
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
      for (Node n : Iterators.loop(db.dao().allInfraSpecies())) {
        if (!n.hasLabel(Labels.SYNONYM)) {
          NubUsage u = read(n);
          // check for autonyms excluding virus names or unparsed names (e.g. OTU)
          if (u.kingdom != Kingdom.VIRUSES && u.parsedName.isParsableType() && u.parsedName.isParsed() && !u.parsedName.isAutonym()) {
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
      families = Iterators.asList(db.dao().allFamilies());
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
              // ignore all supra specific names, autonyms and unparsed OTUs
              if (nub.rank.isSpeciesOrBelow() && !c.hasLabel(Labels.AUTONYM) && nub.parsedName.isParsableType() && nub.parsedName.isParsed()) {
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
      currSrc = NUB_SRC;
      for (Kingdom k : Kingdom.values()) {
        NubUsage ku = new NubUsage();
        ku.usageKey = idGen.reissue(k.nubUsageKey());
        ku.sourceIds.add(ku.usageKey);
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
    //for (Node n : Iterators.loop(db.allTaxa())) {
    //    NubUsage nub = read(n);
    //    if (!nub.sourceIds.isEmpty()) {
    //        LOG.debug("Add extension data from source ids {}", commaJoin.join(nub.sourceIds));
    //    }
    //}
  }

  /**
   * Flags emtpy genera or removes them if they have an ignorable origin
   */
  private void flagEmptyGenera() {
    LOG.info("flag empty genera as doubtful");
    try (Transaction tx = db.beginTx()) {
      for (Node gen : Iterators.loop(db.dao().allGenera())) {
        if (!gen.hasRelationship(RelType.PARENT_OF, Direction.OUTGOING)) {
          NubUsage nub = read(gen);
          if (IGNORABLE_ORIGINS.contains(nub.origin)) {
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
      for (Node n : Iterators.loop(db.dao().allImplicitNames())) {
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
      for (Node gn : Iterators.loop(db.dao().allGenera())) {
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
            // OTU names are special...
            if (sp.parsedName != null && NameType.OTU == sp.parsedName.getType()) {
              continue;
            }
            if (sp.rank != Rank.SPECIES) {
              LOG.warn("Genus child {} is not a species: {} {}", spn, sp.rank, NeoProperties.getScientificName(spn));
              continue;
            }
            if (sp.parsedName == null || sp.parsedName.getGenusOrAbove() == null) {
              LOG.warn("Genus child {} without genus name part: {} {}", spn, sp.rank, NeoProperties.getScientificName(spn));
              continue;
            }
            if (!genus.equals(sp.parsedName.getGenusOrAbove())) {
              sp.issues.add(NameUsageIssue.NAME_PARENT_MISMATCH);
            }
            db.store(sp);

            // check infraspecific names
            String species = sp.parsedName.getSpecificEpithet();
            for (Node ispn : Traversals.CHILDREN.traverse(spn).nodes()) {
              NubUsage isp = db.dao().readNub(ispn);
              // OTU names are special...
              if (isp.parsedName != null && NameType.OTU == isp.parsedName.getType()) {
                continue;
              }
              if (isp.parsedName.getInfraSpecificEpithet() == null) {
                LOG.warn("Species child {} without an infraspecific epithet: {} {}", ispn, isp.rank, NeoProperties.getScientificName(ispn));
                continue;
              }
              if (!genus.equals(isp.parsedName.getGenusOrAbove()) || !species.equals(isp.parsedName.getInfraSpecificEpithet())) {
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

      } catch (RuntimeException e) {
        LOG.error("Error processing source {}", src.name);
        throw e;

      } catch (Exception e) {
        throw new RuntimeException("Error processing source " + src.name, e);

      } finally {
        src.close();
      }
    }
    LOG.info("Remove solitary, excluded homonyms");
    int counter = 0;
    try (Transaction tx = db.beginTx()) {
      for (Kingdom k : Kingdom.values()) {
        NubUsage ku = db.kingdom(k);
        for (Node n : Traversals.CHILDREN.traverse(ku.node).nodes()) {
          NubUsage u = db.dao().readNub(n);
          if (u.origin == Origin.VERBATIM_ACCEPTED) {
            counter++;
            delete(u);
            LOG.info("Remove excluded homonym {}", u);
          }
        }
        tx.success();
      }
      LOG.info("Removed {} excluded homonyms", counter);
    }
  }

  private void addDataset(NubSource source) throws Exception {
    LOG.info("Adding {}th source {} {}. Allow suprageneric homonyms={}", datasetCounter++, source.key, source.name, source.supragenericHomonymSource);
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
      UnmodifiableIterator<List<SrcUsage>> batchIter = com.google.common.collect.Iterators.partition(iter, cfg.neo.batchSize);
      while (batchIter.hasNext()) {
        try (Transaction tx = db.beginTx()) {
          List<SrcUsage> batch = batchIter.next();
          LOG.debug("process batch {} with {} usages", batchCounter++, batch.size());
          for (SrcUsage u : batch) {
            // catch errors processing individual records too
            try {
              // make rank non null
              if (u.rank == null) u.rank = Rank.UNRANKED;

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

            } catch (RuntimeException e) {
              LOG.error("Error processing usage {} >{}< {}", u.key, u.scientificName, e.getMessage());
              throw e;
            }
          }
          tx.success();
        }
      }
    }

    // process explicit basionym relations
    processExplicitBasionymRels();

    LOG.info("Processed {} source usages for {}", sourceUsageCounter - start, source.name);
  }

  private void processExplicitBasionymRels() {
    try (Transaction tx = db.beginTx()) {
      LOG.info("Processing {} explicit basionym relations from {}", basionymRels.size(), currSrc.name);
      for (Long2IntMap.Entry entry : basionymRels.long2IntEntrySet()) {
        // is the basionym excluded from the backbone? e.g. unincluded rank
        if (!src2NubKey.containsKey(entry.getIntValue())) {
          LOG.warn("Ignore explicit basionym with source ID={} cause its ignored in the Backbone", entry.getIntValue());
          continue;
        }

        // find basionym node by sourceKey
        Node bas = db.getNode(src2NubKey.get(entry.getIntValue()));
        Node n = db.getNode(entry.getLongKey());
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
      }
      tx.success();
    }
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
  
  /**
   * Only checks if the usage is one of a known homonym pair, but does not assert it needs to be removed.
   */
  private boolean isExcludedHomonym(SrcUsage u) {
    Set<String> keys = Sets.newHashSet(u.scientificName, u.parsedName.canonicalName());
    for (String name : keys) {
      if (name != null && cfg.homonymExclusions.containsKey(name)) {
        return parents.parentsContain(cfg.homonymExclusions.get(name));
      }
    }
    return false;
  }


  private NubUsage processSourceUsage(SrcUsage u, Origin origin, NubUsage parent) throws IgnoreSourceUsageException {
    Preconditions.checkArgument(parent.status.isAccepted());
    Preconditions.checkNotNull(u.status);
    Preconditions.checkNotNull(u.rank);
    Preconditions.checkNotNull(u.parsedName);

    // filter out various unwanted names
    filterUsage(u);

    // check excluded homonyms
    // If so, place below kingdom for further rematching and mark children doubtful in parent stack!
    if (isExcludedHomonym(u)) {
      if (u.status.isSynonym()) {
        throw new IgnoreSourceUsageException("Ignore excluded homonym synonym ", u.scientificName);
      }
      Kingdom k = parents.nubKingdom();
      LOG.info("Move excluded homonym {} to kingdom {} and mark descendants doubtful", u, k);
      parents.markSubtreeAsDoubtful();
      parent = db.kingdom(k);
      origin = Origin.VERBATIM_ACCEPTED; // we hijack origin.verbatim_parent to mark homonym
      if (u.parsedName.isParsed()) {
        u.parsedName.setAuthorship(null);
        u.parsedName.setYear(null);
        u.parsedName.setBracketAuthorship(null);
        u.parsedName.setBracketYear(null);
        u.scientificName = u.parsedName.canonicalName();
        u.parsedName.setScientificName(u.scientificName);
      }
    }

    // mark doubtful cause source parent was marked, e.g. excluded homonym?
    if (u.status.isAccepted() && parents.isDoubtful()) {
      u.status = TaxonomicStatus.DOUBTFUL;
    }

    // match to existing usages
    NubUsageMatch match = db.findNubUsage(currSrc.key, u, parents.nubKingdom(), parent);

    // process only usages not to be ignored and with desired ranks
    if (!match.ignore && (allowedRanks.contains(u.rank) || NameType.OTU == u.parsedName.getType() && Rank.UNRANKED == u.rank)) {

      if (!match.isMatch() || (
              fromCurrentSource(match.usage) && currSrc.supragenericHomonymSource &&
                      !IGNORABLE_ORIGINS.contains(origin) && !IGNORABLE_ORIGINS.contains(match.usage.origin)
      )) {

        // remember if we had a doubtful match
        NubUsage doubtful = match.doubtfulUsage;
        // persistent new nub usage if there wasnt any yet
        match = createNubUsage(u, origin, parent);
        // check if we had a doubtful or implicit & accepted name match
        if (doubtful != null && u.status == TaxonomicStatus.ACCEPTED) {
          db.transferChildren(match.usage, doubtful);
        }

      } else {
        if (IGNORABLE_ORIGINS.contains(origin)) {
          // do not update or change usages with implicit names or excluded homonyms
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
          src2NubKey.put((int) u.key, match.usage.node.getId());
        }
        if (u.originalNameKey != null) {
          // remember basionym relation.
          // Basionyms do not follow the taxnomic hierarchy, so we might not have seen some source keys yet
          // we will process all basionyms at the end of each source dataset
          basionymRels.put(match.usage.node.getId(), (int) u.originalNameKey);
        }
      }
    } else {
      LOG.debug("Ignore {} {}", u.rank, u.scientificName);
    }
    return match.usage;
  }

  private void delete(NubUsage nub) {
    for (int sourceId : nub.sourceIds) {
      src2NubKey.remove(sourceId);
    }
    basionymRels.remove(nub.node.getId());
    db.dao().delete(nub);
  }

  /**
   * Removes a taxon if it has no accepted children or synonyms
   *
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
    Preconditions.checkNotNull(u.parsedName);
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
      throw new IllegalStateException("Parent is a synonym" + u.scientificName);
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
        try {
          if (u.parsedName.isParsableType() && u.parsedName.isParsed() && u.parsedName.isBinomial()) {
            SrcUsage implicit = new SrcUsage();
            implicit.status = TaxonomicStatus.DOUBTFUL;
            implicit.parsedName = new ParsedName();
            implicit.parsedName.setType(NameType.SCIENTIFIC);
            implicit.parsedName.setGenusOrAbove(u.parsedName.getGenusOrAbove());
            if (u.parsedName.getInfraSpecificEpithet() == null) {
              implicit.rank = Rank.GENUS;
            } else {
              implicit.rank = Rank.SPECIES;
              implicit.parsedName.setSpecificEpithet(u.parsedName.getSpecificEpithet());
            }
            if (p.rank.higherThan(implicit.rank)) {
              implicit.scientificName = implicit.parsedName.canonicalName();
              implicit.parsedName.setScientificName(implicit.scientificName);
              implicit.parsedName.setRank(implicit.rank);
              NubUsage implicitParent = processSourceUsage(implicit, Origin.IMPLICIT_NAME, p);
              // in case the implicit parent species is a synonym, better ignore the infraspecies alltogether!
              // http://dev.gbif.org/issues/browse/POR-2780
              if (implicitParent == null) {
                LOG.debug("No implicit name {} {}", implicit.rank, implicit.scientificName);

              } else if (implicitParent.status.isSynonym()) {
                throw new IgnoreSourceUsageException("Ignoring implicit synonym", implicitParent.parsedName.getScientificName());

              } else {
                // use the implicit parent
                LOG.debug("Implicit parent {} {} for usage {} {}", implicit.rank, implicit.scientificName, u.rank, u.scientificName);
                p = implicitParent;
              }
            }
          }

        } catch (IgnoreSourceUsageException e) {
          LOG.debug("Ignore implicit {}: {}", e.name, e.getMessage());
          // now also ignore this source usage
          throw new IgnoreSourceUsageException("Ignoring source with ignored implicit name " + e.name, u.scientificName);
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
    // normalize authors in the Backbone
    authorNorm.normalize(u);
    
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

  private boolean blacklisted(SrcUsage u) {
    return u.parsedName.getType() == NameType.BLACKLISTED ||
        cfg.isBlacklisted(u.scientificName) ||
        (u.parsedName.canonicalName() != null && cfg.isBlacklisted(u.parsedName.canonicalName()));
  }

  /**
   * Filters out vertain kind of names that we do not want in the backbone
   * and throws a IgnoreSourceUsageException in such cases.
   *
   * @throws IgnoreSourceUsageException
   */
  private void filterUsage(SrcUsage u) throws IgnoreSourceUsageException {
    // avoid unwanted types of names, e.g. indet names
    // Expect & filter out informal names here for: indetermined, abbreviated or incomplete
    if (ignoredNameTypes.contains(u.parsedName.getType())) {
      throw new IgnoreSourceUsageException("Ignore " + u.parsedName.getType() + " name", u.scientificName);
    }
    // avoid unparsed names for parsable types
    if (u.parsedName.getType().isParsable() && !u.parsedName.isParsed()) {
      throw new IgnoreSourceUsageException("Ignore unparsable " + u.parsedName.getType() + ": ", u.scientificName);
    }
    // reject partially parsed monomial
    if (u.parsedName.isParsedPartially() && !u.parsedName.isBinomial()) {
      throw new IgnoreSourceUsageException("Ignore partially parsed " + u.parsedName.getRank() + " ", u.scientificName);
    }
    // check blacklist
    if (blacklisted(u)) {
      throw new IgnoreSourceUsageException("Ignore blacklisted name", u.scientificName);
    }
    // avoid taxon concept names
    if (!StringUtils.isBlank(u.parsedName.getSensu())) {
      throw new IgnoreSourceUsageException("Ignore taxon concept names", u.scientificName);
    }
    // avoid names with nulls in epithets
    if ("null".equals(u.parsedName.getSpecificEpithet()) || "null".equals(u.parsedName.getInfraSpecificEpithet())) {
      throw new IgnoreSourceUsageException("Ignore names with null epithets", u.scientificName);
    }
    // consider infraspecific names subspecies
    if (u.parsedName.getRank() == Rank.INFRASPECIFIC_NAME && u.parsedName.isBinomial() && u.parsedName.getInfraSpecificEpithet() != null) {
      u.parsedName.setRank(Rank.SUBSPECIES);
    }
    // consider parsed rank only for bi/trinomials
    Rank pRank = u.parsedName.isBinomial() ? u.parsedName.getRank() : null;
    if (pRank != null && pRank != u.rank && !pRank.isUncomparable()) {
      if (u.rank == Rank.UNRANKED) {
        LOG.debug("Use parsed rank {}", pRank);
        u.rank = pRank;
      } else if (u.rank.isUncomparable()) {
        LOG.debug("Prefer parsed rank {} over {}", pRank, u.rank);
        u.rank = pRank;
      } else {
        LOG.debug("Rank {} does not match parsed rank {}. Ignore {}", u.rank, pRank, u.scientificName);
        throw new IgnoreSourceUsageException("Parsed rank mismatch", u.scientificName);
      }

    } else if (Rank.SPECIES_AGGREGATE == u.rank && u.parsedName.isBinomial() && u.parsedName.getInfraSpecificEpithet() == null) {
      // this is an aggregate species rank and we have a binomial - treat as a species in the backbone!
      u.rank = Rank.SPECIES;
      LOG.debug("Treat species aggregate {} as species", u.scientificName);
    }

    // avoid indet names after rank has been finalized
    if (u.parsedName.isIndetermined()) {
      throw new IgnoreSourceUsageException("Ignore indetermined name", u.scientificName);
    }
  }

  private void updateNomenclature(NubUsage nub, SrcUsage u) {
    LOG.debug("Updating nomenclature for {} from source {}", nub.parsedName.getScientificName(), u.parsedName.getScientificName());
    // authorship
    if (u.parsedName.hasAuthorship() && (!nub.parsedName.hasAuthorship() || currSrc.nomenclator)) {
      nub.parsedName.setAuthorship(u.parsedName.getAuthorship());
      nub.parsedName.setYear(u.parsedName.getYear());
      nub.parsedName.setBracketAuthorship(u.parsedName.getBracketAuthorship());
      nub.parsedName.setBracketYear(u.parsedName.getBracketYear());
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
              || db.existsInClassification(parent.node, currNubParent.node, false) && currNubParent.rank != parent.rank
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
   * <p>
   * As we merge names from different taxonomies it is possible there are multiple accepted names (maybe via a synonym relation) in such a group.
   * We always stick to the first combination with the highest priority and make all others
   * a) synonyms of this if it is accepted
   * b) synonyms of the primary's accepted name if it was a synonym itself
   * <p>
   * In case of conflicting accepted names we also flag these names with CONFLICTING_BASIONYM_COMBINATION
   */
  private void consolidateBasionymGroups() {
    int counter = 0;
    int counterModified = 0;
    // first load all basionym node ids into a set so we can process them individually in separate transactions
    LongSet basIds = new LongOpenHashSet();
    try (Transaction tx = db.beginTx()) {
      for (Node bas : Iterators.loop(db.dao().allBasionyms())) {
        basIds.add(bas.getId());
      }
      LOG.info("Found {} basionyms to consolidate", basIds.size());
    }
    // now consolidate each basionym group in its own transaction
    for (long basId : basIds) {
      try (Transaction tx = db.beginTx()) {
        Node bas = db.getNode(basId);

        counter++;
        // sort all usage by source dataset priority, placing doubtful names last
        List<NubUsage> group = db.listBasionymGroup(bas);
        if (group.size() > 1) {
          // we stick to the first combination with the highest priority and make all others
          //  a) synonyms of this if it is accepted
          //  b) synonyms of the primary's accepted name if it was a synonym itself
          // if there are several usages with the same priority select one according to some defined rules
          final NubUsage primary = findPrimaryUsage(group);
          if (primary==null) {
            // we did not find a usage to trust. skip
            continue;
          }

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
        LOG.info("Basionym {} was removed. Ignore for consolidation", basId, e);
      }
    }
    LOG.info("Consolidated {} usages from {} basionyms in total", counterModified, counter);
  }

  private int priority(NubUsage usage) {
    return priorities.containsKey(usage.datasetKey) ? priorities.get(usage.datasetKey) : maxPriority + 1;
  }

  /**
   * From a list of usages believed to be homotypic select the most trusted usage.
   * If there are multiple usages from the most trusted source:
   *  a) selected a random first one if they all point to the same accepted usage
   *  b) return NULL if the source contains multiple accepted usages - this is either a bad taxonomy in the source
   *  or we did a bad basionym detection and we would wrongly lump names.
   */
  private NubUsage findPrimaryUsage(List<NubUsage> basionymGroup) {
    if (basionymGroup == null || basionymGroup.isEmpty()) {
      return null;
    }
    // a single usage only
    if (basionymGroup.size() == 1) {
      return basionymGroup.get(0);
    }
    // keep shrinking this list until we get one!
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

    // now all usages originate from the same dataset!
    if (candidates.size() > 1) {
      final UUID datasetKey = candidates.get(0).datasetKey;
      // if all remaining usages point to the same accepted taxon it does not matter which we pick
      // otherwise log warning and do not group names further - we risk to have badly detected basionyms
      Map<NubUsage, Integer> accCounts = Maps.newHashMap();
      for (NubUsage u : candidates) {
        final NubUsage accepted = u.status.isSynonym() ? db.parent(u) : u;
        if (!accCounts.containsKey(accepted)) {
          accCounts.put(accepted, 1);
        } else {
          accCounts.put(accepted, accCounts.get(accepted) + 1);
        }
      }
      if (accCounts.size()>1) {
        // the same dataset contains multiple accepted. It is either:
        // a) taxonomically inconsistent
        // b) has pro parte synonyms
        // c) we did some bad basionym detection - better back off
        // d) there are doubtfully accepted taxa which we should maybe ignore
        NubUsage primary = findSingleAccepted(accCounts.keySet());
        if (primary != null) {
          LOG.debug("Prefer single accepted {} in basionym group with {} additional doubtful names out of {} usages from the most trusted dataset {}",
              primary.parsedName.getScientificName(), accCounts.size()-1, candidates.size(), datasetKey);
          return primary;
        }
        LOG.info("Skip basionym group {} with {} accepted names out of {} usages from the most trusted dataset {}",
            candidates.get(0).parsedName.getScientificName(), accCounts.size(), candidates.size(), datasetKey);
        return null;
      }
    }

    return candidates.get(0);
  }

  private NubUsage findSingleAccepted(Collection<NubUsage> usages) {
    NubUsage acc = null;
    for (NubUsage nu : usages) {
      if (nu.status == TaxonomicStatus.ACCEPTED) {
        if (acc != null) {
          // we had another accepted before, return none
          return null;
        }
        acc = nu;
      }
    }
    return acc;
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
    return SEMICOLON_JOIN.join(usages.stream().map(u -> u.parsedName.fullName()).iterator());
  }

  /**
   * Assigns a unique usageKey to all nodes by matching a usage to the previous backbone to keep stable identifiers.
   * Implements a three pass strategy of keys with
   *  - first pass only reassigns existing ids with full, exact name matches
   *  - second pass matches just remaining accepted names
   *  - third pass matches all remaining ones
   */
  private void assignUsageKeys() {
    LOG.info("Assigning final clb ids to usages with authorship using exact matches to previous ids ...");
    for (Map.Entry<Long, NubUsage> entry : db.dao().nubUsages()) {
      NubUsage u = entry.getValue();
      if (u.rank != Rank.KINGDOM && u.parsedName.isParsed() && u.parsedName.hasAuthorship()) {
        u.usageKey = idGen.reissue(u.parsedName, u.rank, u.kingdom);
        if (u.usageKey != 0) {
          db.dao().update(entry.getKey(), u);
        }
      }
    }

    LOG.info("Assigning final clb ids to accepted usages ...");
    assignUsageKeys(u -> u.status == TaxonomicStatus.ACCEPTED);

    LOG.info("Assigning final clb ids to all remaining usages ...");
    assignUsageKeys(u -> true);

    // for pro parte synonyms we need to assign extra keys, one per relation!
    // http://dev.gbif.org/issues/browse/POR-2872
    try (Transaction tx = db.beginTx()) {
      try (ResourceIterator<Relationship> rels = db.dao().listAllRelationships(RelType.PROPARTE_SYNONYM_OF)) {
        while (rels.hasNext()) {
          Relationship rel = rels.next();
          NubUsage u = db.dao().readNub(rel.getStartNode());
          NubUsage acc = db.dao().readNub(rel.getEndNode());
          if (acc.usageKey <= 0) {
            LOG.warn("No usage key assigned to {}", acc);
          }
          int ppKey = idGen.issue(NameFormatter.canonicalOrScientificName(u.parsedName), u.parsedName.getAuthorship(), u.parsedName.getYear(), u.rank, u.status, u.kingdom, acc.usageKey);
          LOG.debug("Assign id {} for pro parte relation of primary usage {} {}", ppKey, u.usageKey, u.parsedName.getScientificName());
          rel.setProperty(NeoProperties.USAGE_KEY, ppKey);
        }
      }
      tx.success();
    }
  }

  private void assignUsageKeys(Predicate<NubUsage> filter){
    for (Map.Entry<Long, NubUsage> entry : db.dao().nubUsages()) {
      NubUsage u = entry.getValue();
      if (u.rank != Rank.KINGDOM && u.usageKey==0 && filter.test(u)) {
        u.usageKey = idGen.issue(NameFormatter.canonicalOrScientificName(u.parsedName), u.parsedName.getAuthorship(), u.parsedName.getYear(), u.rank, u.status, u.kingdom);
        db.dao().update(entry.getKey(), u);
      }
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
