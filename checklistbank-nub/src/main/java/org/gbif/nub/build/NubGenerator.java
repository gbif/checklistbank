package org.gbif.nub.build;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.model.Usage;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.nub.lookup.NubIndexMutable;
import org.gbif.nub.lookup.NubMatchingModule;
import org.gbif.nub.lookup.NubMatchingServiceImpl;
import org.gbif.nub.utils.CacheUtils;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class that build a new taxonomic backbone based on various source checklists.
 * See build() methods for more.
 *
 * TODO:
 * - use wellformed name build from parsed name
 * - insert accordingTo citations for checklists
 */
public class NubGenerator {

  public static final UUID COL_KEY = UUID.fromString("7ddf754f-d193-4cc9-b351-99906754a03b");
  public static final String MT_NUB_PRIORITY_NAME = "nubPriority";
  public static final String MT_NAMESPACE = "clb.gbif.org";

  private static final Logger LOG = LoggerFactory.getLogger(NubGenerator.class);
  private final Set<UUID> higherTaxonomyChecklists = Sets.newHashSet();

  private static final int INCERTAE_SEDIS_NAME_ID = 9;
  //TODO: find real citation ids
  private final int CITATION_GBIF_NUB = -1;
  //private final int CITATION_NUB_GENERATOR_ID;
  //private final int CITATION_NUB_GENERATOR_CANONICAL_ID;
  //private final int CITATION_NUB_GENERATOR_AUTONYM_ID;

  private final UsageService uService;
  private final DatasetSearchService searchService;
  private final DatasetService datasetService;
  private final ParsedNameService nameService;
  private final NubReporter reporter;
  private final NubWriter writer;
  private Map<Integer, UUID> sourceDatasetKeys = Maps.newHashMap();
  private int currSourceDatasetIntKey = 0;
  private Dataset currDataset;
  private NubReporter.DatasetReport currReport;
  // keep an index of nub usages in order to clever merging of taxa
  private NubIndexMutable index;
  private NubMatchingServiceImpl internalMatcher;
  private NameUsageMatchingService previousNubMatcher;
  private ChecklistCache nubCache;
  private ChecklistCache srcCache;
  private Map<Integer, Integer> processed;
  private int nextNubKey;
  //
  private NubUsage incertaeSedis;


  /**
   *
   * @param service
   * @param searchService
   * @param datasetService
   * @param nameService
   * @param previousNubMatcher nub matcher that works on the existing nub. If null always new ids will be created.
   * @throws IOException
   */
  @Inject
  public NubGenerator(UsageService service, DatasetSearchService searchService, DatasetService datasetService,
    ParsedNameService nameService, @Nullable NameUsageMatchingService previousNubMatcher,
    NubWriter writer, NubReporter reporter) throws IOException {
    this.reporter = reporter;
    this.writer = writer;
    this.searchService = searchService;
    this.datasetService = datasetService;
    this.nameService = nameService;
    this.previousNubMatcher = previousNubMatcher;
    uService = service;
    index = NubIndexMutable.newNubIndex();
    srcCache = new ChecklistCacheHashMap();
    nubCache = new ChecklistCacheHashMap();
    internalMatcher = new NubMatchingServiceImpl(index, NubMatchingModule.provideSynonyms(), NubMatchingModule.provideParser());
    internalMatcher.setExternalResolver(nubCache);
  }

  /**
   * Builds a new nub with the Catalogue of Life used for the higher taxonomy.
   * Make sure at least the Catalogue of Life is existing!
   */
  public ChecklistCache build() throws IOException {
    return build(COL_KEY);
  }

  /**
   * Builds a new, complete nub checklist as a simple dwc archive.
   * The nub build will make use of the currently indexed checklists and not run any new imports or updates.
   *
   * The nub is generated by adding one checklist at a time to the nub.
   * The order of the checklists is determined by the integer clb:nubPriority machine tag, with smallest numbers
   * having highest priority.
   *
   * The higher taxonomy above family level is ignored by default unless the checklist is given in the
   * higherTaxonomyChecklists parameter.
   *
   * @param higherTaxonomyChecklists set of checklists where the higher taxonomy is not ignored but incorporated
   *                                 into the nub
   */
  public ChecklistCache build(UUID ... higherTaxonomyChecklists) throws IOException {
    LOG.info("Start building a new taxonomic backbone");

    LOG.info("Use the higher classification from checklists:\n " + Joiner.on("\n ").join(higherTaxonomyChecklists));
    for (UUID c : higherTaxonomyChecklists) {
      this.higherTaxonomyChecklists.add(c);
    }

    nextNubKey = uService.maxUsageKey(Constants.NUB_DATASET_KEY) + 1;
    LOG.info("New nub usages will start with key {}", nextNubKey);
    LOG.info("Adding kingdoms");
    addKingdoms();

    for (Dataset d : listChecklistsFaked()) {
      addChecklist(d, !this.higherTaxonomyChecklists.contains(d.getKey()));
    }

    reporter.close();

    writer.insertNubData(nubCache);
    writer.updateRelNub();

    return nubCache;
  }

  private void loadSrcCache(UUID datasetKey) {
    srcCache.reset();

    // load names
    LOG.info("Loading names into cache for checklist {}", currDataset.getTitle());
    PagingRequest page = new PagingRequest(0, 10000);
    PagingResponse<ParsedName> respNames;
    do {
      respNames = nameService.listNames(datasetKey, page);
      for (ParsedName n : respNames.getResults()) {
        srcCache.add(n);
      }
      page.nextPage();
    }
    while (!respNames.isEndOfRecords());

    // load usages
    LOG.info("Loading usages into cache for checklist {}", currDataset.getTitle());
    page = new PagingRequest(0, 10000);
    PagingResponse<Usage> respUsages;
    do {
      respUsages = uService.list(datasetKey, page);
      for (Usage u : respUsages.getResults()) {
        srcCache.add(u);
      }
      page.nextPage();
    }
    while (!respUsages.isEndOfRecords());

    // verify cache consistency
    srcCache.checkConsistency();
  }

  private void addChecklist(Dataset d, final boolean ignoreHigherTaxa) {
    LOG.info("Adding checklist {}, {} higher taxa", d.getTitle(), ignoreHigherTaxa ? "ignoring" : "using");
    currDataset = d;
    currReport = reporter.report(d.getKey());
    writer.startDataset(d.getKey());

    // TODO: get real currSourceDatasetKey value based on citation table
    sourceDatasetKeys.put(currSourceDatasetIntKey++, d.getKey());
    loadSrcCache(d.getKey());
    processed = Maps.newHashMap();

    Iterator<Usage> iter = srcCache.iterate();
    int counter = 1;
    while (iter.hasNext()) {
      if (counter % 10000 == 0){
        LOG.info("Processing {}th usage", counter);
      }
      Usage u = iter.next();
      LOG.debug("Process {} usage {}", u.rank, u.key);
      processSource(u, ignoreHigherTaxa);
      counter++;
    }
  }

  /**
   *
   * @param src
   * @param ignoreHigherTaxa
   * @return the nub usage for this source usage or the next highest parent. It is
   */
  private NubUsage processSource(Usage src, final boolean ignoreHigherTaxa) {
    if (processed.containsKey(src.key)) {
      return new NubUsage(nubCache.get(processed.get(src.key)), true, NameUsageMatch.MatchType.EXACT, 100);
    }

    // get some more details
    ParsedName srcName = srcCache.getName(src.nameKey);
    Rank rank = getNubRank(src, srcName);

    // match to existing nub
    NubUsage nub = findNubUsage(srcName.getScientificName(), src.rank, src.key);

    // IGNORE source for various reasons
    final boolean ignore = ignoreSrc(src, srcName, rank);
    if (ignore) {
      currReport.incrementIgnored();
      if (nub != null) {
        // we should ignore this source usage and there is a matching nub usage already.
        // Return that and stop
        return nub;
      } else {
        // otherwise return the closest nub usage from the parental hierarchy
        return getParentNubUsage(src, ignoreHigherTaxa);
      }
    }


    // If we reach here, incorporate source into nub!
    // get parental nub usage first to attach this usage
    final boolean isIgnoredHigherTaxon = ignoreHigherTaxa(src, rank, ignoreHigherTaxa);
    NubUsage parentNub = getParentNubUsage(src, ignoreHigherTaxa);

    if (nub != null) {
      updateNub(nub, src, srcName, parentNub, isIgnoredHigherTaxon);

    } else if (isIgnoredHigherTaxon) {
      currReport.incrementIgnored();
      return parentNub;

    } else {
      // only insert new higher taxa if it was requested
      nub = createNub(src, srcName, parentNub, rank);
    }

    return nub;
  }

  private boolean ignoreHigherTaxa(Usage src, Rank nubRank, final boolean ignoreHigherTaxa) {
      // ignore higher ranks if not asked for
    if (ignoreHigherTaxa && (nubRank.isSuprageneric() && Rank.FAMILY != nubRank)) {
      LOG.debug("Ignore {} taxon {}, higher ranks ignored", nubRank, CacheUtils.nameOf(src, srcCache));
      return true;
    }
    return false;
  }

  private boolean ignoreSrc(Usage src, ParsedName srcName, Rank nubRank) {
    // ignore usages without a rank
    if (Rank.UNRANKED == nubRank) {
      LOG.debug("Ignore usage {} of rank {}", srcName.getScientificName(), nubRank);
      return true;
    }

    // completely ignore blacklisted, informal (i.e. spec. or cf.) names or incertae sedis names
    if (NameType.BLACKLISTED == srcName.getType() || NameType.INFORMAL == srcName.getType()) {
      LOG.debug("Ignore blacklisted or informal usage {} {}", src.key, srcName.getScientificName());
      return true;
    }

    if (INCERTAE_SEDIS_NAME_ID == srcName.getKey()) {
      LOG.debug("Ignore incertae sedis usage {} {}", src.key, srcName.getScientificName());
      return true;
    }

    // ignore all cultivars and strains!
    // http://dev.gbif.org/issues/browse/CLB-117
    if (Rank.CULTIVAR == nubRank || Rank.CULTIVAR_GROUP == nubRank || Rank.STRAIN == nubRank){
      LOG.debug("Ignore {} name {}, usage {}", nubRank, srcName, src.key);
    }

    if (!rankMatchesName(nubRank, srcName)) {
      LOG.debug("Ignore usage {} with name {} contradicting rank {}", src.key, srcName.getScientificName(), nubRank);
      return true;
    }

    return false;
  }

  private Rank getNubRank(Usage src, ParsedName name) {
    Rank nubRank = Rank.inferRank(name.getGenusOrAbove(), name.getInfraGeneric(), name.getSpecificEpithet(), name.getRankMarker(), name.getInfraSpecificEpithet());
    if (src.rank != null && !src.rank.isUncomparable()) {
      nubRank = src.rank;
      if (nubRank != null && !nubRank.isUncomparable() && !src.rank.equals(nubRank)) {
        // warn if they are not the same
        LOG.warn("Inferred rank {} is different from given rank {} for usage {}", nubRank, src.rank, src.key);
      }
    }

    // For CoL convert generic infraspecific rank to subspecies - many names in CoL have this rank
    if (COL_KEY.equals(currDataset.getKey()) && Rank.INFRASPECIFIC_NAME == src.rank){
      nubRank = Rank.SUBSPECIES;
    }

    return nubRank;
  }

  protected static boolean rankMatchesName(Rank rank, ParsedName name) {
    // skip assertion for hybrids or virus names which dont have a parsed name.
    if (!name.getType().isParsable()){
      return true;
    }

    if (Rank.SPECIES == rank){
      // we require a genus & specific epithet and no more
      if (!Strings.isNullOrEmpty(name.getGenusOrAbove()) && !Strings.isNullOrEmpty(name.getSpecificEpithet()) && Strings.isNullOrEmpty(name.getInfraSpecificEpithet())){
        return true;
      }
    } else if (rank.isInfraspecific()){
      // we require a genus and both epitheta
      if (!Strings.isNullOrEmpty(name.getGenusOrAbove()) && !Strings.isNullOrEmpty(name.getSpecificEpithet()) && !Strings.isNullOrEmpty(name.getInfraSpecificEpithet())){
        if (rank.isUncomparable()){
          return true;
        }
        // check if rank marker indicates a specific rank
        Rank markerRank = Rank.inferRank(name.getRankMarker());
        if (markerRank == null || markerRank==rank){
          return true;
        }
      }

    } else if (Rank.GENUS == rank || rank.isSuprageneric()) {
      // avoid epitheta
      if (!Strings.isNullOrEmpty(name.getGenusOrAbove()) && Strings.isNullOrEmpty(name.getSpecificEpithet()) && Strings.isNullOrEmpty(name.getInfraSpecificEpithet())){
        return true;
      }

    }
    return false;
  }

  private NubUsage getParentNubUsage(Usage child, boolean ignoreHigherTaxa) {
    if (child.hasParent()) {
      Usage parentSrc = srcCache.get(child.parentKey);
      NubUsage nub = processSource(parentSrc, ignoreHigherTaxa);
      nub.directParentMatch = false;
      return nub;
    } else {
      return incertaeSedis;
    }
  }

  /**
   * Try to match an existing nub usage using the internal matching service
   * running on the currently created nub usages.
   *
   * @param srcUsageKey a source usage key used insert a LinneanClassification for comparison
   * @return the corresponding nub usage or null. Never a MatchType.NONE
   */
  private NubUsage findNubUsage(String scientificName, Rank rank, int srcUsageKey) {
    LinneanClassification cl = srcCache.getClassification(srcUsageKey);
    NameUsageMatch match = tightMatch(internalMatcher.match(scientificName, rank, cl, true, false));
    if (match != null) {
      return new NubUsage(nubCache.get(match.getUsageKey()), true, match.getMatchType(), match.getConfidence());
    }
    return null;
  }

  /**
   * @return the given match or null if the match was not good enough for nub building purposes.
   */
  private NameUsageMatch tightMatch(NameUsageMatch match) {
    if (NameUsageMatch.MatchType.NONE != match.getMatchType()) {
      // for monomials only allow exact matches for now - fuzzy ones are sometimes wrong and would result
      // in false merged taxa
      if (NameUsageMatch.MatchType.EXACT == match.getMatchType() || (match.getRank().isSpeciesOrBelow() && match.getConfidence() > 90)) {
        return match;
      }
    }
    return null;
  }

  private NubUsage createNub(Usage src, ParsedName srcName, NubUsage nubParent, Rank nubRank) {
    if (src.status != null && src.status.isSynonym() && !nubParent.directParentMatch) {
      LOG.info("Synonym {} to be created based on usage {}, but accepted parent is not in the nub", srcName.getScientificName(), src.key);
    }

    // potentially insert missing genus & species usages
    if (nubRank.isSpeciesOrBelow()) {
      if (nubParent.usage.rank.isSuprageneric()) {
        nubParent = createOrFindImplicitUsage(srcName.getGenusOrAbove(), Rank.GENUS, nubParent.usage.key, Origin.IMPLICIT_NAME);
      }
      if (nubRank.isInfraspecific() && nubParent.usage.rank != Rank.SPECIES) {
        nubParent = createOrFindImplicitUsage(srcName.canonicalSpeciesName(), Rank.SPECIES, nubParent.usage.key, Origin.IMPLICIT_NAME);
      }
    }

    // insert an autonym (BotanicalCode) / nominotypical subspecies (ZooCode) ?
    // http://en.wikipedia.org/wiki/Autonym_(botany)
    // http://en.wikipedia.org/wiki/Subspecies#Nominotypical_subspecies_and_subspecies_autonyms
    //
    // all autonyms created here for subspecies do NOT include a rank marker as the ZoologicalCode specifies.
    // TODO: for botanical, fungi and bacterial names this should be changed at the very end
    if (nubRank.isInfraspecific() && !srcName.isAutonym()) {
      final String originalEpithet = srcName.getInfraSpecificEpithet();
      srcName.setInfraSpecificEpithet(srcName.getSpecificEpithet());
      srcName.setRank(nubRank);
      createOrFindImplicitUsage(srcName.canonicalNameWithMarker(), nubRank, nubParent.usage.key, Origin.AUTONYM);
      srcName.setInfraSpecificEpithet(originalEpithet);
    }


    // insert requested usage
    NubUsage nub = new NubUsage();
    nub.usage.parentKey = nubParent.usage.key;
    nub.usage.nameKey = srcName.getKey();
    nub.usage.status = src.status == null ? TaxonomicStatus.ACCEPTED : src.status;
    nub.usage.rank = nubRank;
    nub.usage.accordingToKey = 0;
    nub.usage.origin = Origin.SOURCE;
    nub.usage.sourceKey = src.key;
    nub.usage.namePublishedInKey = src.namePublishedInKey;

    nub.usage.key = getNubUsageId(nub, srcName);

    currReport.incrementCreated();
    add(srcName);
    add(nub.usage);

    LOG.debug("Nub {} {} created: {}", nub.usage.rank, nub.usage.key, srcName.getScientificName());
    return nub;
  }

  private NubUsage createOrFindImplicitUsage(String implicitName, Rank rank, int parentKey, Origin origin) {

    // see if a nub usage with this name already exists
    NubUsage nub = findNubUsage(implicitName, rank, parentKey);

    if (nub == null) {
      // insert new usage
      ParsedName pn = parseAndAdd(implicitName);
      nub = new NubUsage();
      nub.usage.parentKey = parentKey;
      nub.usage.nameKey = pn.getKey();
      nub.usage.status = TaxonomicStatus.ACCEPTED;
      nub.usage.rank = rank;
      nub.usage.accordingToKey = CITATION_GBIF_NUB;
      nub.usage.origin = origin;

      nub.usage.key = getNubUsageId(nub, pn);

      LOG.debug("Implicit nub {} {} created: {}", nub.usage.rank, nub.usage.key, implicitName);
      currReport.incrementCreated();
      add(nub.usage);
    }

    return nub;
  }

  private void updateNub(NubUsage nub, Usage src, ParsedName srcName, NubUsage srcNubParent, boolean isIgnoredHigherTaxon) {
    currReport.incrementUpdated();
    processed.put(src.key, nub.usage.key);

    if (nub.matchType != NameUsageMatch.MatchType.EXACT) {
      LOG.info("Update nub {} {} with {} matching {} {}, confidence={}", nub.usage.rank, CacheUtils.nameOf(nub.usage,nubCache), nub.matchType, src.rank, srcName.getScientificName(), nub.confidence);
    } else {
      LOG.debug("Update nub {} {} with {} matching {} {}, confidence={}", nub.usage.rank, CacheUtils.nameOf(nub.usage,nubCache), nub.matchType, src.rank, srcName.getScientificName(), nub.confidence);
    }

    if (!isIgnoredHigherTaxon) {
      //TODO: implement updates
      // if the nub usage to be updated was created in the same checklist, allow taxonomic status changes!
      if (processed.containsKey(nub.usage.sourceKey)) {

      }
    }

    // all of the following updates are also applied to higher taxa
    if (nub.usage.namePublishedInKey <= 0) {
      nub.usage.namePublishedInKey = src.namePublishedInKey;
    }

    // update classification?
    if (srcNubParent != null) {
      if (classificationUpdateable(nub.usage.parentKey, srcNubParent.usage) ) {
        LOG.info("Classification update of {} from {} to {}", CacheUtils.nameOf(nub.usage, nubCache), CacheUtils.nameOf(nubCache.get(nub.usage.parentKey), nubCache), CacheUtils.nameOf(srcNubParent.usage, nubCache));
        nub.usage.parentKey = srcNubParent.usage.key;
      }
    }
  }

  /**
   * Verifies that the old parent nub key also appears in the new, more detailed classification.
   * @param oldParentKey the old nub parent key
   * @param newParent  the proposed new nub parent
   * @return true if the new parent can be used instead of the old parent key without altering the taxonomic concept
   */
  private boolean classificationUpdateable(int oldParentKey, Usage newParent) {
    if (oldParentKey == newParent.key) {
      return false;
    }
    if (oldParentKey == incertaeSedis.usage.key) {
      return true;
    }

    // if the old parentKey exists in the hierarchy of the new one, its all good
    Usage p = newParent;
    do {
      p = nubCache.get(p.parentKey);
      if (p.parentKey == oldParentKey) {
        return true;
      }
    } while (p.parentKey > 0);

    return false;
  }
  /**
   * Finds an existing nub id or assigns a new identifier that has never been used before in the nub.
   * Existing ids are detected by matching the new nub usage to the old nub via the external matching webservice.
   * @return the nub id to be used for this nub usage
   */
  private int getNubUsageId(NubUsage nub, ParsedName nubName) {
    if (previousNubMatcher != null) {
      LinneanClassification lc = nubCache.getClassification(nub.usage.key);
      NameUsageMatch match = tightMatch(previousNubMatcher.match(nubName.getScientificName(), nub.usage.rank, lc, true, false));
      if (match != null) {
        // verify that we have not used this id before in the nub build
        Usage otherNub = nubCache.get(match.getUsageKey());
        if (otherNub == null) {
          return match.getUsageKey();
        } else {
          LOG.warn("The same previous nub id {} matches 2 usages in the new nub: {} and {} with new id {}", CacheUtils.nameOf(otherNub, nubCache), nubName.getScientificName(), nextNubKey);
        }
      }
    }
    currReport.newId(nextNubKey, nubName.getScientificName());
    return nextNubKey++;
  }

  private List<Dataset> listChecklistsFaked() {
    List<Dataset> datasets = Lists.newArrayList();
    datasets.add(datasetService.get(COL_KEY));
    datasets.add(datasetService.get(UUID.fromString("714c64e3-2dc1-4bb7-91e4-54be5af4da12")));
    datasets.add(datasetService.get(UUID.fromString("672aca30-f1b5-43d3-8a2b-c1606125fa1b")));
    datasets.add(datasetService.get(UUID.fromString("d9f426e7-845c-4a63-be0d-18506f235357")));
    datasets.add(datasetService.get(UUID.fromString("bb5f507f-f7de-4a5a-ae7f-ad8abbe68bef")));
    datasets.add(datasetService.get(UUID.fromString("26bca1b5-3ef6-4e97-9672-0058c79185fb")));
    datasets.add(datasetService.get(UUID.fromString("c696e5ee-9088-4d11-bdae-ab88daffab78")));
    datasets.add(datasetService.get(UUID.fromString("3f8a1297-3259-4700-91fc-acc4170b27ce")));
    return datasets;
  }
  /**
   * @return list of checklists used to build the nub. Order and selection is based on the clb:nubPriority tag
   */
  private List<Dataset> listChecklists() {
    DatasetSearchRequest req = new DatasetSearchRequest();
    req.addTypeFilter(DatasetType.CHECKLIST);
    req.setLimit(1000);

    SortedMap<String, Dataset> nubSources = Maps.newTreeMap();
    for (DatasetSearchResult res : searchService.search(req).getResults()) {
      Dataset d = datasetService.get(res.getKey());
      for (MachineTag mt : d.getMachineTags()) {
        if (mt.getNamespace().equals(MT_NAMESPACE) && mt.getName().equals(MT_NUB_PRIORITY_NAME)) {
          nubSources.put(mt.getValue(), d);
        }
      }
    }
    LOG.info("Found {} tagged checklists in registry to build a new nub from", nubSources.size());

    return Lists.newArrayList(nubSources.values());
  }

  private void addKingdoms() {
    for (Kingdom k : Kingdom.values()){
      ParsedName pn = new ParsedName();
      pn.setScientificName(k.scientificName());
      pn.setGenusOrAbove(k.scientificName());
      pn.setKey(k.clbNameID());
      add(pn);

      Usage u = new Usage();
      u.key=k.nubUsageID();
      u.status = TaxonomicStatus.ACCEPTED;
      u.rank=Rank.KINGDOM;
      u.accordingToKey = CITATION_GBIF_NUB;
      u.nameKey = k.clbNameID();
      u.origin = Origin.DENORMED_CLASSIFICATION;
      add(u);
      // remember incertae sedis kingdom for last resorting parent in the nub
      if (Kingdom.INCERTAE_SEDIS == k) {
        incertaeSedis = new NubUsage(u, false, NameUsageMatch.MatchType.EXACT, 100);
      }
    }
  }

  private void add(ParsedName name) {
    nubCache.add(name);
  }

  private ParsedName parseAndAdd(String name) {
    ParsedName pn = nameService.createOrGet(name);
    add(pn);
    return pn;
  }

  private void add(Usage u) {
    if (u.sourceKey > 0){
      processed.put(u.sourceKey, u.key);
    }
    // insert to nub cache
    nubCache.checkConsistency(u);
    nubCache.add(u);
    // update internal matcher.
    // The matcher uses the nubCache as the classification resolver, so only the usage name is important here!
    try {
      index.addNameUsage(u, CacheUtils.canonicalNameOf(u, nubCache));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to add usage to internal index", e);
    }

    // can be null when adding kingdoms
    if (currReport != null) {
      writer.mapSource(u.sourceKey, u.key);
      currReport.incrementCreated();
    }
  }

  public NubReporter getReporter() {
    return reporter;
  }

  public static void main(String[] args) throws IOException {
    Properties p = PropertiesUtil.loadProperties("nub.properties");
    NubBuildModule module = new NubBuildModule(p);
    Injector inj = Guice.createInjector(module);
    NubGenerator gen = inj.getInstance(NubGenerator.class);
    gen.build();
  }
}
