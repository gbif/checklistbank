/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.ws.resources;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.*;
import org.gbif.api.model.checklistbank.search.*;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.*;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.model.IucnRedListCategory;
import org.gbif.checklistbank.model.NubMapping;
import org.gbif.checklistbank.model.TreeContainer;
import org.gbif.checklistbank.model.UsageCount;
import org.gbif.checklistbank.service.mybatis.mapper.DistributionMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NubRelMapper;
import org.gbif.checklistbank.service.mybatis.mapper.UsageCountMapper;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;


/**
 * Species resource.
 */
@RestController
@RequestMapping(
  value = "/species",
  produces = {MediaType.APPLICATION_JSON_VALUE, "application/x-javascript"}
)
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
  private final NubRelMapper nubRelMapper;


  @Autowired
  public SpeciesResource(
      NameUsageService nameUsageService, VernacularNameService vernacularNameService,
      TypeSpecimenService typeSpecimenService, SpeciesProfileService speciesProfileService,
      ReferenceService referenceService, MultimediaService imageService, DescriptionService descriptionService,
      DistributionService distributionService, IdentifierService identifierService, NameUsageSearchService searchService,
      UsageCountMapper usageCountMapper,
      DistributionMapper distributionMapper, NubRelMapper nubRelMapper) {
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
    this.nubRelMapper = nubRelMapper;
  }

  /**
   * This retrieves a list of all NameUsage from ChecklistBank.
   *
   * @param locale      identifier for a region
   * @param datasetKeys the optional checklist keys to limit paging to
   * @param page        the limit, offset paging information
   * @return requested list of NameUsage or an empty list if none could be found
   */
  @GetMapping
  public PagingResponse<NameUsage> list(Locale locale, @RequestParam(DATASET_KEY) Set<UUID> datasetKeys,
                                        @RequestParam("sourceId") String sourceId, @RequestParam("name") String canonicalName, @RequestParam Pageable page) {

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

  @GetMapping("mapping")
  public List<NubMapping> mappings(@RequestParam(DATASET_KEY) UUID datasetKey) {
    if (datasetKey == null) {
      throw new IllegalArgumentException("DatasetKey is a required parameter");
    }
    List<NubMapping> result = new ArrayList<>();
    nubRelMapper.process(datasetKey).forEach(result::add);
    return result;
  }

  /**
   * This retrieves a NameUsage by its key from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param locale   identifier for a region
   * @return requested NameUsage or null if none could be found. List of NameUsage in case of a search.
   * @see NameUsageService#get(int, Locale)
   */
  @GetMapping("{id}")
  @NullToNotFound
  public NameUsage get(@PathVariable("id") int usageKey, Locale locale) {
    return nameUsageService.get(usageKey, locale);
  }

  @GetMapping("{id}/metrics")
  @NullToNotFound
  public NameUsageMetrics getMetrics(@RequestParam("id") int usageKey) {
    return nameUsageService.getMetrics(usageKey);
  }

  @GetMapping("{id}/name")
  @NullToNotFound
  public ParsedName getParsedName(@RequestParam("id") int usageKey) {
    return nameUsageService.getParsedName(usageKey);
  }

  @GetMapping("{id}/verbatim")
  @NullToNotFound
  public VerbatimNameUsage getVerbatim(@RequestParam("id") int usageKey) {
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
  @GetMapping("{id}/children")
  public PagingResponse<NameUsage> listChildren(@RequestParam("id") int parentKey, Locale locale, Pageable page) {
    return nameUsageService.listChildren(parentKey, locale, page);
  }

  @GetMapping("{id}/childrenAll")
  public List<UsageCount> listAllChildren(@RequestParam("id") int parentKey) {
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
  @GetMapping("{id}/synonyms")
  public PagingResponse<NameUsage> listSynonyms(@RequestParam("id") int usageKey, Locale locale, Pageable page) {
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
  @GetMapping("{id}/vernacularNames")
  public PagingResponse<VernacularName> listVernacularNamesByNameUsage(@RequestParam("id") int usageKey, Pageable page) {
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
  @GetMapping("{id}/typeSpecimens")
  public PagingResponse<TypeSpecimen> listTypeSpecimensByNameUsage(@PathVariable("id") int usageKey, Pageable page) {
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
  @GetMapping("{id}/speciesProfiles")
  public PagingResponse<SpeciesProfile> listSpeciesProfilesByNameUsage(@PathVariable("id") int usageKey, Pageable page) {
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
  @GetMapping("{id}/references")
  public PagingResponse<Reference> listReferencesByNameUsage(@PathVariable("id") int usageKey, Pageable page) {
    return referenceService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all multimedia objects for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all Media objects
   */
  @GetMapping("{id}/media")
  public PagingResponse<NameUsageMediaObject> listImagesByNameUsage(@PathVariable("id") int usageKey, Pageable page) {
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
  @GetMapping("{id}/descriptions")
  public PagingResponse<Description> listDescriptionsByNameUsage(@PathVariable("id") int usageKey, Pageable page) {
    return descriptionService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves a table of contents for all descriptions of a name usage from ChecklistBank.
   */
  @GetMapping("{id}/toc")
  @NullToNotFound
  public TableOfContents get(@PathVariable("id") Integer key) {
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
  @GetMapping("{id}/distributions")
  public PagingResponse<Distribution> listDistributionsByNameUsage(@PathVariable("id") int usageKey, Pageable page) {
    return distributionService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all Identifier for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   * @return a list of all Identifier
   */
  @GetMapping("{id}/identifier")
  public PagingResponse<Identifier> listIdentifierByNameUsage(@PathVariable("id") int usageKey, Pageable page) {
    return identifierService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all related Usages for a NameUsage from ChecklistBank.
   *
   * @param usageKey    NameUsage key
   * @param datasetKeys The optional list of dataset keys to filter related usages
   * @return a list of all Related usages
   */
  @GetMapping("{id}/related")
  public PagingResponse<NameUsage> listRelatedByNameUsage(@PathVariable("id") int usageKey, Locale locale, Pageable page,
                                                          @RequestParam(DATASET_KEY) Set<UUID> datasetKeys) {
    return nameUsageService.listRelated(usageKey, locale, page, datasetKeys.toArray(new UUID[datasetKeys.size()]));
  }

  @GetMapping("{id}/combinations")
  public List<NameUsage> listCombinations(@PathVariable("id") int basionymKey, Locale locale) {
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
  @GetMapping("{id}/parents")
  public List<NameUsage> listParentsByNameUsage(@PathVariable("id") int usageKey, Locale locale, Pageable page) {
    return nameUsageService.listParents(usageKey, locale);
  }

  /**
   * This retrieves the IUCN Redlist Category of a nub usage key.
   * If the matching IUCN usage does not contain a category null is returned.
   *
   * @param usageKey NameUsage key
   * @return the category or null if it doesn't have one
   */
  @GetMapping("{id}/iucnRedListCategory")
  public IucnRedListCategory getIucnRedListCategory(@PathVariable("id") int usageKey) {
    IucnRedListCategory iucn = distributionMapper.getIucnRedListCategory(usageKey);
    return iucn == null || iucn.getCategory() == null ? null : iucn;
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
  @GetMapping("root/{datasetKey}")
  public PagingResponse<NameUsage> listRootUsages(@PathVariable(DATASET_KEY) UUID datasetKey, Locale locale, Pageable page) {
    return nameUsageService.listRoot(datasetKey, locale, page);
  }

  @GetMapping("rootAll/{datasetKey}")
  public List<UsageCount> root(@PathVariable("datasetKey") UUID datasetKey) {
    return usageCountMapper.root(datasetKey);
  }

  @GetMapping("rootNub")
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

  @GetMapping("search")
  public SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> search(NameUsageSearchRequest searchRequest) {
    // POR-2801
    // protect SOLR against deep paging requests which blow heap
    checkDeepPaging(searchRequest);
    return searchService.search(searchRequest);
  }

  @GetMapping("suggest")
  public List<NameUsageSuggestResult> suggest(NameUsageSuggestRequest searchSuggestRequest) {
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
