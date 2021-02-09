package org.gbif.checklistbank.ws.resources;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.*;
import org.gbif.api.model.checklistbank.search.*;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.*;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.ThreatStatus;
import org.gbif.checklistbank.model.TreeContainer;
import org.gbif.checklistbank.model.UsageCount;
import org.gbif.checklistbank.service.mybatis.mapper.DistributionMapper;
import org.gbif.checklistbank.service.mybatis.mapper.UsageCountMapper;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.util.ExtraMediaTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Species resource.
 */
@Path("/species")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
public class SpeciesResource {

  private static final Logger LOG = LoggerFactory.getLogger(SpeciesResource.class);
  private static final String DATASET_KEY = "datasetKey";
  private static final int DEEP_PAGING_OFFSET_LIMIT = 100000;

  private final NameUsageService nameUsageService;
  private final VernacularNameService vernacularNameService;
  private final TypeSpecimenService typeSpecimenService;
  private final SpeciesProfileService speciesProfileService;
  private final ReferenceService referenceService;
  private final MultimediaService imageService;
  private final DescriptionService descriptionService;
  private final DistributionService distributionService;
  private final IdentifierService identifierService;
  private final NameUsageSearchService searchService;
  private final UsageCountMapper usageCountMapper;

  //Used instead of DistributionService to avoid upgrading GBIF API.
  private final DistributionMapper distributionMapper;


  @Inject
  public SpeciesResource(
      NameUsageService nameUsageService, VernacularNameService vernacularNameService,
      TypeSpecimenService typeSpecimenService, SpeciesProfileService speciesProfileService,
      ReferenceService referenceService, MultimediaService imageService, DescriptionService descriptionService,
      DistributionService distributionService, IdentifierService identifierService, NameUsageSearchService searchService,
      UsageCountMapper usageCountMapper,
      DistributionMapper distributionMapper) {
    this.nameUsageService = nameUsageService;
    this.vernacularNameService = vernacularNameService;
    this.typeSpecimenService = typeSpecimenService;
    this.speciesProfileService = speciesProfileService;
    this.referenceService = referenceService;
    this.imageService = imageService;
    this.descriptionService = descriptionService;
    this.distributionService = distributionService;
    this.identifierService = identifierService;
    this.searchService = searchService;
    this.usageCountMapper = usageCountMapper;
    this.distributionMapper = distributionMapper;
  }

  /**
   * This retrieves a list of all NameUsage from ChecklistBank.
   *
   * @param locale      identifier for a region
   * @param datasetKeys the optional checklist keys to limit paging to
   * @param page        the limit, offset paging information
   * @return requested list of NameUsage or an empty list if none could be found
   */
  @GET
  public PagingResponse<NameUsage> list(@Context Locale locale, @QueryParam(DATASET_KEY) Set<UUID> datasetKeys,
                                        @QueryParam("sourceId") String sourceId, @QueryParam("name") String canonicalName, @Context Pageable page) {

    // limit the maximum allowed offset
    checkDeepPaging(page);

    if (datasetKeys == null) {
      datasetKeys = ImmutableSet.of();
    }
    if (Strings.isNullOrEmpty(canonicalName)) {
      if (datasetKeys.size() > 1) {
        // https://github.com/gbif/checklistbank/issues/54
        throw new IllegalArgumentException("Multiple datasetKey parameters are not allowed");
      }
      return nameUsageService.list(locale,
          datasetKeys.isEmpty() ? null : datasetKeys.iterator().next(), sourceId, page);
    } else {
      return nameUsageService.listByCanonicalName(locale, canonicalName, page,
          datasetKeys.isEmpty() ? null : datasetKeys.toArray(new UUID[datasetKeys.size()]));
    }
  }

  /**
   * This retrieves a NameUsage by its key from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param locale   identifier for a region
   * @return requested NameUsage or null if none could be found. List of NameUsage in case of a search.
   * @see NameUsageService#get(int, Locale)
   */
  @GET
  @Path("{id}")
  @NullToNotFound
  public NameUsage get(@PathParam("id") int usageKey, @Context Locale locale) {
    return nameUsageService.get(usageKey, locale);
  }

  @GET
  @Path("{id}/metrics")
  @NullToNotFound
  public NameUsageMetrics getMetrics(@PathParam("id") int usageKey) {
    return nameUsageService.getMetrics(usageKey);
  }

  @GET
  @Path("{id}/name")
  @NullToNotFound
  public ParsedName getParsedName(@PathParam("id") int usageKey) {
    return nameUsageService.getParsedName(usageKey);
  }

  @GET
  @Path("{id}/verbatim")
  @NullToNotFound
  public VerbatimNameUsage getVerbatim(@PathParam("id") int usageKey) {
    return nameUsageService.getVerbatim(usageKey);
  }

  /**
   * This retrieves a list of children NameUsage for a parent NameUsage from ChecklistBank.
   *
   * @param parentKey parent NameUsage key
   * @param locale    identifier for a region
   * @param page      the limit, offset paging information
   * @return requested list of NameUsage or an empty list if none could be found
   * @see NameUsageService#listChildren(int, Locale, Pageable)
   */
  @GET
  @Path("{id}/children")
  public PagingResponse<NameUsage> listChildren(@PathParam("id") int parentKey, @Context Locale locale, @Context Pageable page) {
    return nameUsageService.listChildren(parentKey, locale, page);
  }

  @GET
  @Path("{id}/childrenAll")
  public List<UsageCount> listAllChildren(@PathParam("id") int parentKey) {
    return usageCountMapper.children(parentKey);
  }

  /**
   * This retrieves a list of synonym NameUsage for a NameUsage from ChecklistBank.
   *
   * @param usageKey parent NameUsage key
   * @param locale   identifier for a region
   * @param page     the limit, offset, and count paging information
   * @return requested list of NameUsage or an empty list if none could be found
   * @see NameUsageService#listChildren(int, Locale, Pageable)
   */
  @GET
  @Path("{id}/synonyms")
  public PagingResponse<NameUsage> listSynonyms(@PathParam("id") int usageKey, @Context Locale locale, @Context Pageable page) {
    return nameUsageService.listSynonyms(usageKey, locale, page);
  }


  /**
   * This retrieves all VernacularNames for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all VernacularNames
   * @see VernacularNameService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/vernacularNames")
  public PagingResponse<VernacularName> listVernacularNamesByNameUsage(@PathParam("id") int usageKey, @Context Pageable page) {
    return vernacularNameService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all TypeSpecimens for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all TypeSpecimens
   * @see TypeSpecimenService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/typeSpecimens")
  public PagingResponse<TypeSpecimen> listTypeSpecimensByNameUsage(@PathParam("id") int usageKey, @Context Pageable page) {
    return typeSpecimenService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all SpeciesProfiles for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all SpeciesProfiles
   * @see SpeciesProfileService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/speciesProfiles")
  public PagingResponse<SpeciesProfile> listSpeciesProfilesByNameUsage(@PathParam("id") int usageKey, @Context Pageable page) {
    return speciesProfileService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all References for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all References
   * @see ReferenceService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/references")
  public PagingResponse<Reference> listReferencesByNameUsage(@PathParam("id") int usageKey, @Context Pageable page) {
    return referenceService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all multimedia objects for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all Media objects
   */
  @GET
  @Path("{id}/media")
  public PagingResponse<NameUsageMediaObject> listImagesByNameUsage(@PathParam("id") int usageKey, @Context Pageable page) {
    return imageService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all Descriptions for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all Descriptions
   * @see DescriptionService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/descriptions")
  public PagingResponse<Description> listDescriptionsByNameUsage(@PathParam("id") int usageKey, @Context Pageable page) {
    return descriptionService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves a table of contents for all descriptions of a name usage from ChecklistBank.
   */
  @GET
  @Path("{id}/toc")
  @NullToNotFound
  public TableOfContents get(@PathParam("id") Integer key) {
    return descriptionService.getToc(key);
  }

  /**
   * This retrieves all Distributions for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all Distributions
   * @see DistributionService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/distributions")
  public PagingResponse<Distribution> listDistributionsByNameUsage(@PathParam("id") int usageKey, @Context Pageable page) {
    return distributionService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all Identifier for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all Identifier
   */
  @GET
  @Path("{id}/identifier")
  public PagingResponse<Identifier> listIdentifierByNameUsage(@PathParam("id") int usageKey, @Context Pageable page) {
    return identifierService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all related Usages for a NameUsage from ChecklistBank.
   *
   * @param usageKey    NameUsage key
   * @param datasetKeys The optional list of dataset keys to filter related usages
   * @return a list of all Related usages
   */
  @GET
  @Path("{id}/related")
  public PagingResponse<NameUsage> listRelatedByNameUsage(@PathParam("id") int usageKey, @Context Locale locale, @Context Pageable page,
                                                          @QueryParam(DATASET_KEY) Set<UUID> datasetKeys) {
    return nameUsageService.listRelated(usageKey, locale, page, datasetKeys.toArray(new UUID[datasetKeys.size()]));
  }

  @GET
  @Path("{id}/combinations")
  public List<NameUsage> listCombinations(@PathParam("id") int basionymKey, @Context Locale locale) {
    return nameUsageService.listCombinations(basionymKey, locale);
  }

  /**
   * This retrieves all Parents for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all Parents
   * @see NameUsageService#listParents(int, Locale)
   */
  @GET
  @Path("{id}/parents")
  public List<NameUsage> listParentsByNameUsage(@PathParam("id") int usageKey, @Context Locale locale, @Context Pageable page) {
    return nameUsageService.listParents(usageKey, locale);
  }

  /**
   * This retrieves the IUCN Redlist Category of a nub usage key.
   *
   * @param usageKey NameUsage key
   * @return the category or null if it doesn't have one
   */
  @GET
  @Path("{id}/iucnRedListCategory")
  public Map<String,String> getIucnRedListCategory(@PathParam("id") int usageKey) {
    String category = distributionMapper.getIucnRedListCategory(usageKey);
    Map<String,String> cat = new HashMap<>();
    if (category != null) {
      ThreatStatus status = ThreatStatus.valueOf(category);
      cat.put("category", status.name());
      cat.put("code", status.getCode());
    }
    return cat;
  }

  /**
   * This retrieves a list of root NameUsage for a Checklist from ChecklistBank.
   *
   * @param datasetKey UUID or case insensitive shortname of the Checklist to retrieve
   * @param locale     identifier for a region
   * @param page       the limit, offset, and count paging information
   * @return requested list of NameUsage or an empty list if none could be found
   * @see NameUsageService#listRoot(UUID, Locale, Pageable)
   */
  @GET
  @Path("root/{datasetKey}")
  public PagingResponse<NameUsage> listRootUsages(@PathParam(DATASET_KEY) UUID datasetKey, @Context Locale locale, @Context Pageable page) {
    return nameUsageService.listRoot(datasetKey, locale, page);
  }

  @GET
  @Path("rootAll/{datasetKey}")
  public List<UsageCount> root(@PathParam("datasetKey") UUID datasetKey) {
    return usageCountMapper.root(datasetKey);
  }

  @GET
  @Path("rootNub")
  public TreeContainer<UsageCount, Integer> rootNub() {
    TreeContainer<UsageCount, Integer> tree = new TreeContainer<>();
    // kingdoms
    tree.setRoot(usageCountMapper.root(Constants.NUB_DATASET_KEY));
    for (UsageCount k : tree.getRoot()) {
      // phyla ~140, classes ~350, orders ~1400, families are over 22.000 skip
      addChildrenRecursively(tree, k.getKey(), 0, Rank.PHYLUM, Rank.CLASS, Rank.ORDER);
    }
    return tree;
  }

  private void addChildrenRecursively(TreeContainer<UsageCount, Integer> tree, int parent, int rankIdx, Rank... ranks) {
    List<UsageCount> children = usageCountMapper.childrenUntilRank(parent, ranks[rankIdx]);
    if (!children.isEmpty()) {
      tree.getChildren().put(parent, children);
      if (++rankIdx < ranks.length) {
        for (UsageCount c : children) {
          addChildrenRecursively(tree, c.getKey(), rankIdx, ranks);
        }
      }
    }
  }

  @GET
  @Path("search")
  public SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> search(@Context NameUsageSearchRequest searchRequest) {
    // POR-2801
    // protect SOLR against deep paging requests which blow heap
    checkDeepPaging(searchRequest);
    return searchService.search(searchRequest);
  }

  @Path("suggest")
  @GET
  public List<NameUsageSuggestResult> suggest(@Context NameUsageSuggestRequest searchSuggestRequest) {
    // POR-2801
    // protect SOLR against deep paging requests which blow heap
    checkDeepPaging(searchSuggestRequest);
    return searchService.suggest(searchSuggestRequest);
  }

  /**
   * POR-2801
   *
   * @throws java.lang.IllegalArgumentException if the offset is considered too high
   */
  private static void checkDeepPaging(Pageable page) {
    if (page.getOffset() > DEEP_PAGING_OFFSET_LIMIT) {
      throw new IllegalArgumentException("Offset is limited for this operation to " + DEEP_PAGING_OFFSET_LIMIT);
    }
  }
}
