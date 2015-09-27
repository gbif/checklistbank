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
import org.gbif.checklistbank.model.Equality;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NodeProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.TaxonWalker;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.neo.traverse.UsageMetricsHandler;
import org.gbif.checklistbank.nub.lookup.IdLookup;
import org.gbif.checklistbank.nub.lookup.IdLookupImpl;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.nub.source.NubSource;
import org.gbif.checklistbank.nub.source.SourceIterable;
import org.gbif.checklistbank.nub.source.UsageSource;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.utils.SciNameNormalizer;
import org.gbif.common.parsers.KingdomParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Maps;
import com.carrotsearch.hppc.IntLongHashMap;
import com.carrotsearch.hppc.IntLongMap;
import com.carrotsearch.hppc.LongIntHashMap;
import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.cursors.LongIntCursor;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.helpers.Strings;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubBuilder implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(NubBuilder.class);
    private static final Joiner SEMICOLON_JOIN = Joiner.on("; ").skipNulls();
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
    private final boolean closeDao;
    private final UsageSource usageSource;
    private final NameParser parser = new NameParser();
    private NormalizerStats normalizerStats;
    private boolean assertionsPassed;
    private final boolean verifyBackbone;
    private NubSource currSrc;
    private ParentStack parents;
    private int sourceUsageCounter = 0;
    private final KingdomParser kingdomParser = KingdomParser.getInstance();
    private final Map<Kingdom, NubUsage> kingdoms = Maps.newHashMap();
    private final AuthorComparator authorComparator;
    private final IdGenerator idGen;
    private final int newIdStart;
    private final IntLongMap src2NubKey = new IntLongHashMap();
    private final LongIntMap basionymRels = new LongIntHashMap(); // node.id -> src.usageKey
    private final Map<UUID, Integer> priorities = Maps.newHashMap();
    private Integer maxPriority = 0;

    private final Ordering priorityStatusOrdering = Ordering.natural().onResultOf(new Function<NubUsage, Integer>() {
        @Nullable
        @Override
        public Integer apply(@Nullable NubUsage u) {
            int doubtfulScore = TaxonomicStatus.DOUBTFUL == u.status ? 100000 : 0;
            int statusScore = u.status == null ? 10 : u.status.ordinal();
            int datasetPriority = priorities.containsKey(u.datasetKey) ? priorities.get(u.datasetKey) : maxPriority+1;
            return doubtfulScore + statusScore + 10 * datasetPriority;
        }
    });

    private NubBuilder(UsageDao dao, UsageSource usageSource, IdLookup idLookup, AuthorComparator authorComparator, int newIdStart, File reportDir,
                       boolean closeDao, boolean verifyBackbone) {
        db = NubDb.create(dao, 1000);
        this.usageSource = usageSource;
        this.authorComparator = authorComparator;
        idGen = new IdGenerator(idLookup, newIdStart, reportDir);
        this.newIdStart = newIdStart;
        this.closeDao = closeDao;
        this.verifyBackbone = verifyBackbone;
    }

    public static NubBuilder create(NubConfiguration cfg) {
        UsageDao dao = UsageDao.persistentDao(cfg.neo, Constants.NUB_DATASET_KEY, null, true);
        try {
            IdLookupImpl idLookup = new IdLookupImpl(cfg.clb);
            // load highest nub id from clb:
            Injector inj = Guice.createInjector(cfg.clb.createServiceModule());
            Integer newIdStart = inj.getInstance(UsageService.class).maxUsageKey(Constants.NUB_DATASET_KEY) + 1;;
            return new NubBuilder(dao, cfg.usageSource(), idLookup, idLookup.getAuthorComparator(), newIdStart == null ? 1000 : newIdStart, cfg.neo.nubReportDir(), true, cfg.autoImport);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load existing backbone ids", e);
        }
    }

    /**
     * @param dao the dao to create the nub. Will be left open after run() is called.
     */
    public static NubBuilder create(UsageDao dao, UsageSource usageSource, IdLookup idLookup, int newIdStart) {
        return new NubBuilder(dao, usageSource, idLookup, AuthorComparator.createWithoutAuthormap(), newIdStart, null, false, false);
    }

    /**
     * Builds a new neo4j based backbone with metrics and stable ids already mapped.
     * The DAO is kept open if you provided it explicitly, otherwise its being closed.
     */
    @Override
    public void run() {
        try {
            addKingdoms();
            parents = new ParentStack(kingdoms.get(Kingdom.INCERTAE_SEDIS));
            addDatasets();
            groupByBasionym();
            verifyAcceptedSpecies();
            flagEmptyGroups();
            flagSimilarNames();
            createAutonyms();
            addPublicationDois();
            addExtensionData();
            assignUsageKeys();
            if (verifyBackbone){
                verifyBackbone();
            }
            if (!verifyBackbone || assertionsPassed){
                db.dao.convertNubUsages();
                builtUsageMetrics();
            }
            LOG.info("New backbone built");
        } finally {
            if (closeDao) {
                db.dao.close();
                LOG.info("DAO closed");
            }
        }
    }

    /**
     * Goes through all accepted species and infraspcecies and flags suspicous similar names.
     * Adds a NameUsageIssue.POTENTIAL_DUPLICATE to all similar names.
     */
    private void flagSimilarNames() {
        LOG.info("Start flagging similar names");
        try {
            db.openTx();
            flagSimilarNames(db.dao.allSpecies());
            flagSimilarNames(db.dao.allInfraSpecies());
        } finally {
            db.closeTx();
        }
    }

    private void flagSimilarNames(ResourceIterator<Node> iter) {
        Set<String> names = Sets.newHashSet();
        for (Node n : IteratorUtil.loop(iter)) {
            if (!n.hasLabel(Labels.SYNONYM)) {
                NubUsage u = db.dao.readNub(n);
                String name = SciNameNormalizer.normalize(db.dao.canonicalOrScientificName(u.parsedName, false));
                if (!Strings.isBlank(name)) {
                    if (names.contains(name)) {
                        u.issues.add(NameUsageIssue.ORTHOGRAPHIC_VARIANT);
                        db.store(u);
                    } else {
                        names.add(name);
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

    private void verifyBackbone() {
        NubAssertions assertions = new NubAssertions(db);
        this.assertionsPassed = assertions.verify();
    }

    /**
     * Goes through all accepted infraspecies and checks if a matching autonym exists,
     * creating missing autonyms where needed.
     * An autonym is an infraspecific taxon that has the same species and infraspecific epithet.
     *
     * We do this last to not create autonyms that we dont need after basionyms are grouped or status has changed for some other reason.
     */
    private void createAutonyms() {
        LOG.info("Start creating missing autonyms");
        try {
            db.openTx();
            int counter = 0;
            for (Node n : IteratorUtil.loop(db.dao.allInfraSpecies())) {
                if (!n.hasLabel(Labels.SYNONYM)) {
                    NubUsage u = db.dao.readNub(n);
                    // check for autonyms
                    if (!u.parsedName.isAutonym()) {
                        ParsedName pn = new ParsedName();
                        pn.setGenusOrAbove(u.parsedName.getGenusOrAbove());
                        pn.setSpecificEpithet(u.parsedName.getSpecificEpithet());
                        pn.setInfraSpecificEpithet(u.parsedName.getSpecificEpithet());
                        pn.setScientificName(pn.canonicalName());
                        pn.setRank(u.rank);

                        NubUsage auto = db.findNubUsage(pn.canonicalName(), u.rank);
                        if (auto == null) {
                            NubUsage parent = db.getParent(u);

                            SrcUsage autonym = new SrcUsage();
                            autonym.rank = u.rank;
                            autonym.scientificName = pn.canonicalName();
                            autonym.parsedName = pn;
                            autonym.status = TaxonomicStatus.ACCEPTED;
                            createNubUsage(autonym, Origin.AUTONYM, parent);
                            counter++;
                        }
                    }
                }
            }
            LOG.info("Created {} missing autonyms", counter);
        } finally {
            db.closeTx();
        }
    }

    /**
     * Goes through all accepted species and infraspecies and makes sure the name matches the genus, species classification.
     * For example an accepted species Picea alba with a parent genus of Abies is taxonomic nonsense.
     * Badly classified names are assigned the doubtful status and an issue is flagged
     */
    private void verifyAcceptedSpecies() {

    }

    /**
     * Goes thru all usages and tries to discover basionyms by comparing the specific or infraspecific epithet and the authorships within a family.
     */
    private void detectBasionyms() {
        LOG.info("Discover basionyms");
        int newBasionyms = 0;
        int newRelations = 0;
        final BasionymSorter basSorter = new BasionymSorter(authorComparator);
        // read all families into a list to avoid concurrent modifications
        // resulting in a "lucene.store.AlreadyClosedException: this IndexReader is closed"
        List<Node> families = IteratorUtil.asList(db.dao.allFamilies());
        for (Node n : families) {
            NubUsage fam = db.dao.readNub(n);
            if (!fam.status.isSynonym()) {
                Map<String, List<NubUsage>> epithets = Maps.newHashMap();
                Map<String, Set<String>> epithetBridges = Maps.newHashMap();
                LOG.debug("Discover basionyms in family {}", fam.parsedName.canonicalNameComplete());
                // key all names by their terminal epithet
                for (Node c : Traversals.DESCENDANTS.traverse(n).nodes()) {
                    NubUsage nub = db.dao.readNub(c);
                    // ignore all supra specific names
                    if (nub.rank.isSpeciesOrBelow()) {
                        String epithet = nub.parsedName.getTerminalEpithet();
                        if (!epithets.containsKey(epithet)) {
                            epithets.put(epithet, Lists.newArrayList(nub));
                        } else {
                            epithets.get(epithet).add(nub);
                        }
                        // now check if a basionym relation exists already that reaches out to some other epithet, e.g. due to gender changes
                        for (Node bg : Traversals.BASIONYM_GROUP.evaluator(Evaluators.excludeStartPosition()).traverse(c).nodes()) {
                            NubUsage bgu = db.dao.readNub(bg);
                            String epithet2 = bgu.parsedName.getTerminalEpithet();
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
                    // go through groups and create basionym relations where needed
                    for (BasionymGroup<NubUsage> group : groups) {
                        // we only need to process groups that contain recombinations
                        if (group.hasRecombinations()) {
                            // if we have a basionym creating relations is straight forward
                            NubUsage basionym = null;
                            if (group.hasBasionym()) {
                                basionym = group.getBasionym();

                            } else if (group.getRecombinations().size() > 1) {
                                // we need to create a placeholder basionym to group the 2 or more recombinations
                                newBasionyms++;
                                basionym = createBasionymPlaceholder(fam, group);
                            }
                            // create basionym relations
                            if (basionym != null) {
                                for (NubUsage u : group.getRecombinations()) {
                                    if (createBasionymRelationIfNotExisting(basionym.node, u.node)) {
                                        newRelations++;
                                        u.issues.add(NameUsageIssue.ORIGINAL_NAME_DERIVED);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        LOG.info("Discovered {} new basionym relations and created {} basionym placeholders", newRelations, newBasionyms);
    }

    private NubUsage createBasionymPlaceholder(NubUsage family, BasionymGroup group) {
        NubUsage basionym = new NubUsage();
        basionym.datasetKey = null;
        basionym.origin = Origin.BASIONYM_PLACEHOLDER;
        basionym.rank = Rank.SPECIES;
        basionym.status = TaxonomicStatus.DOUBTFUL;
        basionym.parsedName = new ParsedName();
        basionym.parsedName.setGenusOrAbove("?");
        basionym.parsedName.setSpecificEpithet(group.getEpithet());
        basionym.parsedName.setAuthorship(group.getAuthorship());
        basionym.parsedName.setYear(group.getYear());
        basionym.parsedName.setType(NameType.PLACEHOLDER);
        LOG.debug("creating basionym placeholder {} in family {}", basionym.parsedName.canonicalNameComplete(), family.parsedName.canonicalName());
        return db.addUsage(family, basionym);
    }

    private void addKingdoms() {
        LOG.info("Adding kingdom");
        currSrc = new NubSource();
        currSrc.key = Constants.NUB_DATASET_KEY;
        for (Kingdom k : Kingdom.values()) {
            NubUsage ku = new NubUsage();
            ku.usageKey = k.nubUsageID();
            ku.kingdom = k;
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
     * Adds all extension data, e.g. vernacular names, to the backbone directly.
     * TODO:
     * - build map from source usage key to nub node id
     * - stream (jdbc copy) through all extension data in postgres and attach to relevant nub node
     */
    private void addExtensionData() {
        LOG.warn("NOT IMPLEMENTED: Copy extension data to backbone");
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

    private void flagEmptyGroups() {
        LOG.info("flag empty genera as doubtful");
        try {
            db.openTx();
            for (Node gen : IteratorUtil.loop(db.dao.allGenera())) {
                if (!gen.hasRelationship(RelType.PARENT_OF, Direction.OUTGOING)) {
                    NubUsage nub = db.dao.readNub(gen);
                    if (!nub.status.isSynonym()) {
                        nub.issues.add(NameUsageIssue.NO_SPECIES);
                        if (TaxonomicStatus.ACCEPTED == nub.status) {
                            nub.status = TaxonomicStatus.DOUBTFUL;
                        }
                        db.store(nub);
                    }
                }
            }
        } finally {
            db.closeTx();
        }
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
        priorities.put(source.key, source.priority);
        maxPriority = source.priority;
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
        try (SourceIterable iter = usageSource.iterateSource(source)) {
            for (SrcUsage u : iter) {
                // catch errors processing individual records too
                try {
                    LOG.debug("process {} {} {}", u.status, u.rank, u.scientificName);
                    sourceUsageCounter++;
                    parents.add(u);
                    // replace accepted taxa with doubtful ones for all nomenclators
                    if (currSrc.nomenclator && TaxonomicStatus.ACCEPTED == u.status) {
                        u.status = TaxonomicStatus.DOUBTFUL;
                    }
                    NubUsage nub = processSourceUsage(u, Origin.SOURCE, parents.nubParent());
                    if (nub != null) {
                        parents.put(nub);
                    }
                } catch (IgnoreSourceUsageException e) {
                    LOG.error("Ignore usage {} {}", u.key, u.scientificName);

                } catch (StackOverflowError e) {
                    // if this happens its time to fix some code!
                    LOG.error("CODE BUG: StackOverflowError processing {} from source {}", u.scientificName, source.name, e);
                    LOG.error("CAUSE: {}", u.parsedName);

                } catch (RuntimeException e) {
                    LOG.error("RuntimeException processing {} from source {}", u.scientificName, source.name, e);
                }
            }

        } catch (Exception e) {
            LOG.error("Error processing source {}", source.name, e);
        }
        db.renewTx();

        // process explicit basionym relations
        LOG.info("Processing {} explicit basionym relations from {}", basionymRels.size(), source.name);
        for (LongIntCursor c : basionymRels) {
            Node n = db.getNode(c.key);
            Node bas = db.getNode(src2NubKey.get(c.value));
            // find basionym node by sourceKey
            if (n != null && bas != null) {
                if (!createBasionymRelationIfNotExisting(bas, n)) {
                    LOG.warn("Nub usage {} already contains a contradicting basionym relation. Ignore basionym {} from source {}", n.getProperty(NodeProperties.SCIENTIFIC_NAME, n.getId()), bas.getProperty(NodeProperties.SCIENTIFIC_NAME, bas.getId()), source.name);
                }
            } else {
                LOG.warn("Could not resolve basionym relation for nub {} to source usage {}", c.key, c.value);
            }
        }
        db.renewTx();

        LOG.info("Processed {} source usages for {}", sourceUsageCounter - start, source.name);
    }

    /**
     * @return true if basionym relationship was created
     */
    private boolean createBasionymRelationIfNotExisting(Node basionym, Node n) {
        if (!n.hasRelationship(RelType.BASIONYM_OF, Direction.BOTH)) {
            basionym.createRelationshipTo(n, RelType.BASIONYM_OF);
            basionym.addLabel(Labels.BASIONYM);
            return true;
        }
        return false;
    }

    private NubUsage processSourceUsage(SrcUsage u, Origin origin, NubUsage parent) throws IgnoreSourceUsageException {
        Preconditions.checkNotNull(u.status);
        // try to parse name
        addParsedNameIfNull(u);
        NubUsage nub = db.findNubUsage(currSrc.key, u, parents.nubKingdom(), parent);
        // try harder to match to a kingdom by also using the kingdom parser for a rank above kingdom
        if (nub == null && u.rank != null && u.rank.higherThan(Rank.PHYLUM)) {
            ParseResult<Kingdom> kResult = kingdomParser.parse(u.scientificName);
            if (kResult.isSuccessful()) {
                nub = kingdoms.get(kResult.getPayload());
            }
        }
        // process only usages with desired ranks
        if (u.rank != null && allowedRanks.contains(u.rank)) {
            if (nub == null) {
                // create new nub usage if there wasnt any yet
                nub = createNubUsage(u, origin, parent);

            } else {

                Equality authorEq = authorComparator.compare(nub.parsedName, u.parsedName);

                if (nub.status.isSynonym() == u.status.isSynonym()) {
                    // update nub usage if status matches
                    updateNub(nub, u, origin, parent);

                } else if (Equality.DIFFERENT == authorEq) {
                    // create new nub usage with different status and authorship as before
                    nub = createNubUsage(u, origin, parent);

                } else if (fromCurrentSource(nub) && !u.status.isSynonym()) {
                    // prefer accepted over synonym if from the same source
                    LOG.debug("prefer accepted {} over synonym usage from the same source", u.scientificName);
                    delete(nub);
                    nub = createNubUsage(u, origin, parent);

                } else if (fromCurrentSource(nub) && u.parsedName.hasAuthorship() && Equality.EQUAL != authorEq) {
                    // allow new synonyms with non equal authorship to be created
                    nub = createNubUsage(u, origin, parent);

                } else if (currSrc.nomenclator) {
                    updateNomenclature(nub, u);

                } else {
                    LOG.debug("Ignore source usage. Status {} is different from nub: {}", u.status, u.scientificName);
                }
            }

            if (nub != null) {
                if (u.key != null) {
                    // remember all original source usage key to nub id mappings per dataset
                    src2NubKey.put(u.key, nub.node.getId());
                }
                if (u.originalNameKey != null) {
                    // remember basionym relation.
                    // Basionyms do not follow the taxnomic hierarchy, so we might not have seen some source keys yet
                    // we will process all basionyms at the end of each source dataset
                    basionymRels.put(nub.node.getId(), u.originalNameKey);
                }
            }
        } else {
            LOG.debug("Ignore source usage with undesired rank {}: {}", u.rank, u.scientificName);
        }
        return nub;
    }

    private void delete(NubUsage nub) {
        for (int srcId : nub.sourceIds) {
            src2NubKey.remove(srcId);
        }
        basionymRels.remove(nub.node.getId());
        db.dao.delete(nub);
    }

    /**
     * Removes a taxon if it has no accepted children
     */
    public void removeTaxonIfEmpty(NubUsage u) {
        if (!u.node.hasRelationship(Direction.INCOMING, RelType.SYNONYM_OF, RelType.PROPARTE_SYNONYM_OF)
                && !u.node.hasRelationship(Direction.OUTGOING, RelType.PARENT_OF)) {
            delete(u);
        }
    }

    private NubUsage createNubUsage(SrcUsage u, Origin origin, NubUsage p) throws IgnoreSourceUsageException {
        addParsedNameIfNull(u);

        if (!u.status.isSynonym() && !p.status.isSynonym()) {
            // check if implicit species or genus parents are needed
            SrcUsage implicit = new SrcUsage();
            try {
                if (u.rank == Rank.SPECIES && p.rank != Rank.GENUS) {
                    implicit.rank = Rank.GENUS;
                    implicit.scientificName = u.parsedName.getGenusOrAbove();
                    implicit.status = u.status;
                    p = processSourceUsage(implicit, Origin.IMPLICIT_NAME, p);

                } else if (u.rank.isInfraspecific() && p.rank != Rank.SPECIES) {
                    implicit.rank = Rank.SPECIES;
                    implicit.scientificName = u.parsedName.canonicalSpeciesName();
                    implicit.status = u.status;
                    p = processSourceUsage(implicit, Origin.IMPLICIT_NAME, p);
                }
            } catch (IgnoreSourceUsageException e) {
                LOG.error("Failed to create implicit {} {}", implicit.rank, implicit.scientificName);
            }
        }
        // if parent is a synonym use accepted as parent and flag as doubtful
        // http://dev.gbif.org/issues/browse/POR-2780
        if (p.status.isSynonym()) {
            p = db.getParent(p);
            if (!u.status.isSynonym()) {
                u.status = TaxonomicStatus.DOUBTFUL;
                return db.addUsage(p, u, origin, currSrc.key, NameUsageIssue.NAME_PARENT_MISMATCH);
            }
        }
        // add to nub db
        return db.addUsage(p, u, origin, currSrc.key);
    }

    private void addParsedNameIfNull(SrcUsage u) throws IgnoreSourceUsageException {
        if (u.parsedName == null) {
            try {
                u.parsedName = parser.parse(u.scientificName, u.rank);
                // avoid taxon concept names
                if (!Strings.isBlank(u.parsedName.getSensu())) {
                    throw new IgnoreSourceUsageException("Ignore taxon concept names", u.key, u.scientificName);
                }
            } catch (UnparsableException e) {
                // allow virus names in the nub
                if (e.type == NameType.VIRUS) {
                    u.parsedName = new ParsedName();
                    u.parsedName.setScientificName(u.scientificName);
                    u.parsedName.setType(e.type);
                } else {
                    throw new IgnoreSourceUsageException(e.getMessage(), u.key, u.scientificName);
                }
            }
        }
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
        }

        // publishedIn
        if (u.publishedIn != null && (nub.publishedIn == null || currSrc.nomenclator)) {
            nub.publishedIn = u.publishedIn;
        }

        // nom status
        if (u.nomStatus != null && u.nomStatus.length > 0 && (nub.nomStatus.isEmpty() || currSrc.nomenclator)) {
            nub.nomStatus = Sets.newHashSet(u.nomStatus);
        }

        // remember all author spelling variations we come across for better subsequent comparisons
        String authors = u.parsedName.authorshipComplete();
        if (!Strings.isBlank(authors)) {
            nub.authors.add(authors);
        }
    }

    private void updateNub(NubUsage nub, SrcUsage u, Origin origin, NubUsage parent) {
        LOG.debug("Updating {} from source {}", nub.parsedName.getScientificName(), u.parsedName.getScientificName());
        nub.sourceIds.add(u.key);
        if (origin == Origin.SOURCE) {
            // only override original origin value if we update from a true source
            nub.origin = Origin.SOURCE;
        }

        // update author, publication and nom status
        updateNomenclature(nub, u);

        NubUsage currNubParent = db.getParent(nub);
        // prefer accepted version over doubtful if its coming from the same dataset!
        if (nub.status == TaxonomicStatus.DOUBTFUL && u.status == TaxonomicStatus.ACCEPTED && fromCurrentSource(nub)) {
            nub.status = u.status;
            if (parent != null && (currNubParent.rank.higherThan(parent.rank) || currNubParent.rank == parent.rank)) {
                if (db.existsInClassification(currNubParent.node, parent.node)) {
                    // current classification has this parent already in its parents list. No need to change anything
                } else {
                    // current classification doesnt have that parent, we need to apply it
                    updateParent(nub, parent);
                }
            }

        } else if (nub.status.isSynonym()) {
            // maybe we have a proparte synonym from the same dataset?
            if (fromCurrentSource(nub) && !parent.node.equals(currNubParent.node)) {
                nub.status = TaxonomicStatus.PROPARTE_SYNONYM;
                // create new pro parte relation
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
            if (parent != null && currNubParent.rank.higherThan(parent.rank)) {
                if (db.existsInClassification(parent.node, currNubParent.node)) {
                    updateParent(nub, parent);
                }
            }
        }
        db.store(nub);
    }

    private void updateParent(NubUsage child, NubUsage parent) {
        LOG.debug("Update {} classification with new parent {} {}", child.parsedName.getScientificName(), parent.rank, parent.parsedName.getScientificName());
        db.updateParentRel(child.node, parent.node);
    }

    private boolean fromCurrentSource(NubUsage nub) {
        if (nub.datasetKey.equals(currSrc.key)) {
            return true;
        }
        return false;
    }

    private void groupByBasionym() {
        LOG.info("Start basionym consolidation");
        try {
            db.openTx();
            detectBasionyms();
            consolidateBasionymGroups();
        } finally {
            db.closeTx();
        }
    }

    /**
     * Make sure we only have at most one accepted name for each homotypical basionym group!
     * An entire group can consist of synonyms without a problem, but they must all refer to the same accepted name.
     *
     * If a previously accepted name needs to be turned into a synonym it will be of type homotypical_synonym.
     */
    private void consolidateBasionymGroups() {
        int counter = 0;
        int counterModified = 0;
        for (Node bas : IteratorUtil.loop(db.dao.allBasionyms())) {
            counter++;
            // sort all usage by source dataset priority, placing doubtful names last
            List<NubUsage> group = priorityStatusOrdering.sortedCopy(db.listBasionymGroup(bas));
            if (group.size() > 1) {
                // we stick to the first combination with the highest priority and make all others
                // a) synonyms of this if it is accepted
                // b) synonyms of the primarys accepted name if it was a synonym itself
                final NubUsage primary = group.remove(0);
                final NubUsage accepted = primary.status.isSynonym() ? db.getParent(primary) : primary;
                final TaxonomicStatus synStatus = primary.status.isSynonym() ? primary.status : TaxonomicStatus.HOMOTYPIC_SYNONYM;
                LOG.debug("Found basionym group with {} primary usage {} in basionym group: {}", primary.status, primary.parsedName.canonicalNameComplete(), names(group));
                for (NubUsage u : group) {
                    if (!hasAccepted(u, accepted)) {
                        counterModified++;
                        NubUsage previousParent = db.getParent(u);
                        if (previousParent != null) {
                            u.addRemark( String.format("Originally found in sources as %s %s %s", u.status.toString().toLowerCase().replaceAll("_", " "),
                                    u.status.isSynonym() ? "of" : "taxon within", previousParent.parsedName.canonicalNameComplete())
                            );
                        }
                        // remember previous parent
                        NubUsage parent = db.getParent(u);
                        // convert to synonym, removing old parent relation
                        convertToSynonym(u, accepted, synStatus, NameUsageIssue.CONFLICTING_BASIONYM_COMBINATION);
                        // move any accepted children to new accepted parent
                        db.assignParentToChildren(u.node, accepted);
                        // move any synonyms to new accepted parent
                        db.assignAcceptedToSynonyms(u.node, accepted.node);
                        // remove parent if it has no children or synonyms
                        removeTaxonIfEmpty(parent);
                        // persist usage instance changes
                        db.store(u);
                    }
                }
            }
        }
        LOG.info("Consolidated {} usages from {} basionyms in total", counterModified, counter);
    }

    /**
     * @param u
     * @param acc
     * @return true of the given usage u has a SYNONYM_OF relation to the given acc usage
     */
    private boolean hasAccepted(NubUsage u, NubUsage acc) {
        try (ResourceIterator<Node> iter = Traversals.ACCEPTED.traverse(u.node).nodes().iterator()) {
            while (iter.hasNext()) {
                if (iter.next().equals(acc.node)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Converts an accepted or existing synonym usage to a synonym of a given accepted usage.
     * See http://dev.gbif.org/issues/browse/POR-398
     * @param u the usage to become the synonym
     * @param acc the accepted taxon the new synonym should point to
     */
    private void convertToSynonym(NubUsage u, NubUsage acc, TaxonomicStatus status, NameUsageIssue ... issues) {
        LOG.info("Convert {} into a {} of {}", u.parsedName.fullName(), status, acc.parsedName.fullName());
        // change status
        u.status = status;
        // add synonymOf relation and delete existing parentOf relations
        db.createSynonymRelation(u.node, acc.node);
        // flag issue
        for (NameUsageIssue issue : issues) {
            u.issues.add(issue);
        }
        // usage was changed, update it in kvp store
        db.store(u);
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
        for (NubUsage u : db.dao.nubUsages()) {
            if (u.rank != Rank.KINGDOM) {
                u.usageKey = idGen.issue(u.parsedName.canonicalName(), u.parsedName.getAuthorship(), u.parsedName.getYear(), u.rank, u.kingdom);
            }
        }
    }

    private void builtUsageMetrics() {
        LOG.info("Walk all accepted taxa and build usage metrics");
        UsageMetricsHandler metricsHandler = new UsageMetricsHandler(db.dao);
        TaxonWalker.walkAccepted(db.dao.getNeo(), null, metricsHandler);
        normalizerStats = metricsHandler.getStats(0, null);
        LOG.info("Walked all taxa (root={}, total={}, synonyms={}) and built usage metrics", normalizerStats.getRoots(), normalizerStats.getCount(), normalizerStats.getSynonyms());
    }

    public NormalizerStats getStats() {
        return normalizerStats;
    }

    public IdGenerator.Metrics idMetrics() {
        return idGen.metrics();
    }

    /**
     * @return the first newly generated id that did not exist in clb before
     */
    public int getNewIdStart() {
        return newIdStart;
    }

    /**
     * @return true if basic backbone assertions have passed sucessfully.
     */
    public boolean assertionsPassed() {
        return assertionsPassed;
    }
}
