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
import org.gbif.api.documentation.CommonParameters;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.*;
import org.gbif.api.model.checklistbank.search.*;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.*;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.model.IucnRedListCategory;
import org.gbif.checklistbank.model.NubMapping;
import org.gbif.checklistbank.model.TreeContainer;
import org.gbif.checklistbank.model.UsageCount;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.DistributionMapper;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.NubRelMapper;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.UsageCountMapper;

import java.lang.annotation.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

import static java.lang.annotation.ElementType.*;

/** Species resource. */
@OpenAPIDefinition(
  info = @Info(
    title = "Species API",
    version = "v1",
    description = "This API works against data kept in the GBIF Checklist Bank which taxonomically indexes all " +
      "registered [checklist datasets](https://www.gbif.org/dataset/search?type=CHECKLIST) in the GBIF network.\n\n" +
      "Internally we use a [Java web service client](https://github.com/gbif/checklistbank/tree/master/checklistbank-ws-client) " +
      "for the consumption of these HTTP-based, RESTful JSON web services.",
    termsOfService = "https://www.gbif.org/terms"),
  servers = {
    @Server(url = "https://api.gbif.org/v1/", description = "Production"),
    @Server(url = "https://api.gbif-uat.org/v1/", description = "User testing")
  },
  tags = {
    @Tag(name = "Species",
      description = "### Working with Name Usages.\n\n" +
        "A name usage is the usage of a scientific name according to one particular checklist including the " +
        "[GBIF Taxonomic Backbone](https://www.gbif.org/dataset/d7dddbf4-2cf0-4f39-9b2a-bb099caae36c) which is called " +
        "“NUB” in this API.\n\n" +
        "Backbone name usages have `key == nubKey`.  Name usages from other checklists with names that also exist " +
        "in the backbone will have a `taxonKey` that points to the related usage in the backbone.",
      extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0100"))
    ),
    @Tag(name = "Searching names",
      description = "GBIF provide four different ways of finding name usages. They differ in their matching behavior, " +
        "response format and also the actual content covered.",
      extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0200"))
    ),
    @Tag(name = "Name parser",
      description = "GBIF exposes its java based name parser library through our API. The service takes one or a list " +
        "of simple scientific name strings, parses each and returns a list of parsed names.",
      extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0300"))
    )
  })
@RestController
@RequestMapping(
    value = "/species",
    produces = {MediaType.APPLICATION_JSON_VALUE, "application/x-javascript"})
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

  // Used instead of DistributionService to avoid upgrading GBIF API.
  private final DistributionMapper distributionMapper;
  private final NubRelMapper nubRelMapper;

  /**
   * Stable name usage key for documentation.
   */
  @Target({PARAMETER, METHOD, ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Parameter(
    name = "usageKey",
    description = "Name usage key.",
    example = "5231190",
    schema = @Schema(implementation = Integer.class, minimum = "0"),
    in = ParameterIn.PATH)
  @interface NameUsagePathParameter {}

  /**
   * Accept-Language header documentation.
   */
  @Target({METHOD, ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Parameter(
    name = "Accept-Language",
    description = "Language for vernacular names, given as an ISO 639-1 **two-letter** code from our " +
      "[vocabulary](https://api.gbif.org/v1/enumeration/language).",
    example = "en",
    schema = @Schema(implementation = Language.class),
    in = ParameterIn.HEADER)
  @interface NameUsageLanguageParameter {}

  /**
   * A convenient meta-annotation for Swagger API responses.
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid query", content = @Content),
    @ApiResponse(responseCode = "404", description = "Name usage not found", content = @Content)})
  @interface DefaultBadResponses {}

  @Autowired
  public SpeciesResource(
      NameUsageService nameUsageService,
      VernacularNameService vernacularNameService,
      TypeSpecimenService typeSpecimenService,
      SpeciesProfileService speciesProfileService,
      ReferenceService referenceService,
      MultimediaService imageService,
      DescriptionService descriptionService,
      DistributionService distributionService,
      IdentifierService identifierService,
      NameUsageSearchService searchService,
      UsageCountMapper usageCountMapper,
      DistributionMapper distributionMapper,
      NubRelMapper nubRelMapper) {
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
   * @param datasetKeys the optional checklist keys to limit paging to
   * @param page the limit, offset paging information
   * @return requested list of NameUsage or an empty list if none could be found
   */
  @Operation(
    operationId = "listNames",
    summary = "List all name usages",
    description = "Lists all name usages across all checklists.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0100"))
  )
  @Tag(name = "Searching names")
  @Parameters(
    value = {
      @Parameter(
        name = DATASET_KEY,
        description = "A UUID of a checklist dataset."
      ),
      @Parameter(
        name = "sourceId",
        description = "Filters by the source identifier."
      ),
      @Parameter(
        name = "name",
        description = "A name without authorship, to match exactly."
      )
    }
  )
  @NameUsageLanguageParameter
  @ApiResponse(responseCode = "200", description = "Name usages found")
  @DefaultBadResponses
  @GetMapping
  public PagingResponse<NameUsage> list(
      @RequestParam(value = DATASET_KEY, required = false) Set<UUID> datasetKeys,
      @RequestParam(value = "sourceId", required = false) String sourceId,
      @RequestParam(value = "name", required = false) String canonicalName,
      Pageable page) {

    Locale locale = LocaleContextHolder.getLocale();

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
      return nameUsageService.list(
          locale, datasetKeys.isEmpty() ? null : datasetKeys.iterator().next(), sourceId, page);
    } else {
      return nameUsageService.listByCanonicalName(
          locale,
          canonicalName,
          page,
          datasetKeys.isEmpty() ? null : datasetKeys.toArray(new UUID[datasetKeys.size()]));
    }
  }

  /**
   * We can't remember why this was needed.  Performance is very poor for large datasets, so it's not part of the
   * public API.  It should probably be /mapping/{datasetKey} if it were to become supported.
   */
  @Hidden
  @GetMapping("mapping")
  @Transactional //required because nubRelMapper.process uses cursor
  public List<NubMapping> mappings(@RequestParam(value = DATASET_KEY) UUID datasetKey) {
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
   * @return requested NameUsage or null if none could be found. List of NameUsage in case of a
   *     search.
   * @see NameUsageService#get(int, Locale)
   */
  @Operation(
    operationId = "getNameUsage",
    summary = "Name usage by id",
    description = "Retrieves a single name usage.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0200"))
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @NameUsageLanguageParameter
  @ApiResponse(responseCode = "200", description = "Name usage found")
  @DefaultBadResponses
  @GetMapping("{usageKey}")
  @NullToNotFound("/species/{usageKey}")
  public NameUsage get(@PathVariable("usageKey") int usageKey) {
    return nameUsageService.get(usageKey, LocaleContextHolder.getLocale());
  }

  @Operation(
    operationId = "getNameUsageMetrics",
    summary = "Name usage metrics by id",
    description = "Retrieves metrics for a single name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @ApiResponse(responseCode = "200", description = "Name usage metrics found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/metrics")
  @NullToNotFound("/species/{usageKey}/metrics")
  public NameUsageMetrics getMetrics(@PathVariable("usageKey") int usageKey) {
    return nameUsageService.getMetrics(usageKey);
  }

  @Operation(
    operationId = "getNameUsageNameParsed",
    summary = "Parsed name usage by id",
    description = "Retrieves the parsed name for a single name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @ApiResponse(responseCode = "200", description = "Parsed name usage found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/name")
  @NullToNotFound("/species/{usageKey}/name")
  public ParsedName getParsedName(@PathVariable("usageKey") int usageKey) {
    return nameUsageService.getParsedName(usageKey);
  }

  /*
  Add to .components.schemas.VerbatimNameUsage
          "additionalProperties": {
            "type":"object",
            "description":"Verbatim terms etc etc"
        }

   */

  @Operation(
    operationId = "getNameUsageVerbatim",
    summary = "Verbatim name usage by id",
    description = "Retrieves a verbatim name usage.\n\n" +
      "The response object has JSON properties for each verbatim term property."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @ApiResponse(responseCode = "200", description = "Verbatim name usage found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/verbatim")
  @NullToNotFound("/species/{usageKey}/verbatim")
  public VerbatimNameUsage getVerbatim(@PathVariable("usageKey") int usageKey) {
    return nameUsageService.getVerbatim(usageKey);
  }

  /**
   * This retrieves a list of children NameUsage for a parent NameUsage from ChecklistBank.
   *
   * @param parentKey parent NameUsage key
   * @param page the limit, offset paging information
   * @return requested list of NameUsage or an empty list if none could be found
   * @see NameUsageService#listChildren(int, Locale, Pageable)
   */
  @Operation(
    operationId = "getNameUsageChildren",
    summary = "Name usage children by id",
    description = "Retrieves a list of child Name Usages for a parent Name Usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @NameUsageLanguageParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Name usage children found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/children")
  public PagingResponse<NameUsage> listChildren(
      @PathVariable("usageKey") int parentKey, Pageable page) {
    return nameUsageService.listChildren(parentKey, LocaleContextHolder.getLocale(), page);
  }

  @Operation(
    operationId = "getNameUsageChildrenAll",
    summary = "All name usage children by id",
    description = "Retrieves a brief list of all child Name Usages for a parent Name Usage."
  )
  @Tag(name = "Species")
  @ApiResponse(responseCode = "200", description = "Name usage children found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/childrenAll")
  public List<UsageCount> listAllChildren(@PathVariable("usageKey") int parentKey) {
    return usageCountMapper.children(parentKey);
  }

  /**
   * This retrieves a list of synonym NameUsage for a NameUsage from ChecklistBank.
   *
   * @param usageKey parent NameUsage key
   * @param page the limit, offset, and count paging information
   * @return requested list of NameUsage or an empty list if none could be found
   * @see NameUsageService#listChildren(int, Locale, Pageable)
   */
  @Operation(
    operationId = "getNameUsageSynonyms",
    summary = "Name usage synonyms by id",
    description = "Retrieves all synonyms for a name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @NameUsageLanguageParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Name usage synonyms found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/synonyms")
  public PagingResponse<NameUsage> listSynonyms(
      @PathVariable("usageKey") int usageKey, Pageable page) {
    return nameUsageService.listSynonyms(usageKey, LocaleContextHolder.getLocale(), page);
  }

  /**
   * This retrieves all VernacularNames for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page The page and offset and count information
   * @return a list of all VernacularNames
   * @see VernacularNameService#listByUsage(int, Pageable)
   */
  @Operation(
    operationId = "getNameUsageVernacularNames",
    summary = "Name usage vernacular names by id",
    description = "Retrieves all vernacular names for a name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Name usage vernacular names found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/vernacularNames")
  public PagingResponse<VernacularName> listVernacularNamesByNameUsage(
      @PathVariable("usageKey") int usageKey, Pageable page) {
    return vernacularNameService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all TypeSpecimens for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page The page and offset and count information
   * @return a list of all TypeSpecimens
   * @see TypeSpecimenService#listByUsage(int, Pageable)
   */
  @Operation(
    operationId = "getNameUsageTypeSpecimens",
    summary = "Name usage type specimens by id",
    description = "Retrieves partial type specimen information for a name usage.\n\n" +
      "The current Checklistbank only includes this information for some genus and family names, see " +
      "[limitations](https://github.com/gbif/portal-feedback/issues/1146#issuecomment-366260607)",
    deprecated = true
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Name usage type specimens found.")
  @DefaultBadResponses
  @GetMapping("{usageKey}/typeSpecimens")
  public PagingResponse<TypeSpecimen> listTypeSpecimensByNameUsage(
      @PathVariable("usageKey") int usageKey, Pageable page) {
    return typeSpecimenService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all SpeciesProfiles for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page The page and offset and count information
   * @return a list of all SpeciesProfiles
   * @see SpeciesProfileService#listByUsage(int, Pageable)
   */
  @Operation(
    operationId = "getNameUsageSpeciesProfiles",
    summary = "Name usage species profiles by id",
    description = "Retrieves all species profiles for a name usage.\n\n" +
      "Species profiles describe basic species characteristics.",
    externalDocs = @ExternalDocumentation(url = "https://rs.gbif.org/terms/1.0/SpeciesProfile", description = "Species profile extension definition")
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Name usage species profiles found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/speciesProfiles")
  public PagingResponse<SpeciesProfile> listSpeciesProfilesByNameUsage(
      @PathVariable("usageKey") int usageKey, Pageable page) {
    return speciesProfileService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all References for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page The page and offset and count information
   * @return a list of all References
   * @see ReferenceService#listByUsage(int, Pageable)
   */
  @Operation(
    operationId = "getNameUsageReferences",
    summary = "Name usage references by id",
    description = "Retrieves all references for a name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200",description = "Name usage references found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/references")
  public PagingResponse<Reference> listReferencesByNameUsage(
      @PathVariable("usageKey") int usageKey, Pageable page) {
    return referenceService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all multimedia objects for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page The page and offset and count information
   * @return a list of all Media objects
   */
  @Operation(
    operationId = "getNameUsageMedia",
    summary = "Name usage media by id",
    description = "Retrieves all media for a name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Name usage media found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/media")
  public PagingResponse<NameUsageMediaObject> listImagesByNameUsage(
      @PathVariable("usageKey") int usageKey, Pageable page) {
    return imageService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all Descriptions for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page The page and offset and count information
   * @return a list of all Descriptions
   * @see DescriptionService#listByUsage(int, Pageable)
   */
  @Operation(
    operationId = "getNameUsageDescriptions",
    summary = "Name usage descriptions by id",
    description = "Retrieves all descriptions for a name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Name usage descriptions found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/descriptions")
  public PagingResponse<Description> listDescriptionsByNameUsage(
      @PathVariable("usageKey") int usageKey, Pageable page) {
    return descriptionService.listByUsage(usageKey, page);
  }

  /** This retrieves a table of contents for all descriptions of a name usage from ChecklistBank. */
  @Operation(
    operationId = "getNameUsageDescriptionsTableOfContents",
    summary = "Name usage descriptions table of contents",
    description = "Retrieves a table of contents for all descriptions of a name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @ApiResponse(responseCode = "200", description = "Name usage description table of contents.")
  @DefaultBadResponses
  @GetMapping("{usageKey}/toc")
  @NullToNotFound("/species/{key}/toc")
  public TableOfContents get(@PathVariable("usageKey") Integer key) {
    return descriptionService.getToc(key);
  }

  /**
   * This retrieves all Distributions for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page The page and offset and count information
   * @return a list of all Distributions
   * @see DistributionService#listByUsage(int, Pageable)
   */
  @Operation(
    operationId = "getNameUsageDistributions",
    summary = "Name usage distributions by id",
    description = "Retrieves all distributions for a name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Name usage distributions found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/distributions")
  public PagingResponse<Distribution> listDistributionsByNameUsage(
      @PathVariable("usageKey") int usageKey, Pageable page) {
    return distributionService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all Identifier for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page The page and offset and count information
   * @return a list of all Identifier
   */
  @Operation(
    operationId = "getNameUsageIdentifiers",
    summary = "Name usage identifiers by id",
    description = "Retrieves all identifiers for a name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Name usage identifiers found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/identifier")
  public PagingResponse<Identifier> listIdentifierByNameUsage(
      @PathVariable("usageKey") int usageKey, Pageable page) {
    return identifierService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all related Usages for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param datasetKeys The optional list of dataset keys to filter related usages
   * @return a list of all Related usages
   */
  @Operation(
    operationId = "getNameUsageRelated",
    summary = "Related name usages by id",
    description = "Retrieves all related name usages for a name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @NameUsageLanguageParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Related name usages found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/related")
  public PagingResponse<NameUsage> listRelatedByNameUsage(
      @PathVariable("usageKey") int usageKey,
      Pageable page,
      @RequestParam(value = DATASET_KEY, required = false, defaultValue = "") Set<UUID> datasetKeys) {
    return nameUsageService.listRelated(
        usageKey, LocaleContextHolder.getLocale(), page, datasetKeys.toArray(new UUID[datasetKeys.size()]));
  }

  @Operation(
    operationId = "getNameUsageRecombinations",
    summary = "Name usage recombinations by id",
    description = "Lists all (re)combinations of a given basionym, excluding the basionym itself."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @NameUsageLanguageParameter
  @ApiResponse(responseCode = "200", description = "Name usage recombinations found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/combinations")
  public List<NameUsage> listCombinations(@PathVariable("usageKey") int basionymKey) {
    return nameUsageService.listCombinations(basionymKey, LocaleContextHolder.getLocale());
  }

  /**
   * This retrieves all Parents for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @return a list of all Parents
   * @see NameUsageService#listParents(int, Locale)
   */
  @Operation(
    operationId = "getNameUsageParents",
    summary = "Parent name usages by id",
    description = "Retrieves all parent name usages for a name usage."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @NameUsageLanguageParameter
  @ApiResponse(responseCode = "200", description = "Parent name usages found")
  @DefaultBadResponses
  @GetMapping("{usageKey}/parents")
  public List<NameUsage> listParentsByNameUsage(
      @PathVariable("usageKey") int usageKey) {
    return nameUsageService.listParents(usageKey, LocaleContextHolder.getLocale());
  }

  /**
   * This retrieves the IUCN Redlist Category for a NUB usage key.
   * If the matching IUCN usage does not contain a category not evaluated (NE) is returned.
   *
   * @param usageKey backbone NameUsage key
   * @return IUCN usage with a category, a NUB usage with NotEvaluated or null if it's not an animal, plant or fungi
   */
  @Operation(
    operationId = "getNameUsageIucnRedListCategory",
    summary = "IUCN Red List Category for a name usage",
    description = "Retrieves the IUCN Red List Category for a name usage.  If the matching IUCN usage does not contain " +
      "a category, Not Evaluated (NE) is returned."
  )
  @Tag(name = "Species")
  @NameUsagePathParameter
  @ApiResponse(responseCode = "200", description = "IUCN Red List category")
  @DefaultBadResponses
  @GetMapping("{usageKey}/iucnRedListCategory")
  public IucnRedListCategory getIucnRedListCategory(@PathVariable("usageKey") int usageKey) {
    IucnRedListCategory iucn = distributionMapper.getIucnRedListCategory(usageKey);
    if (iucn != null) {
      if (iucn.getCategory() == null) {
        iucn.setCategory(ThreatStatus.NOT_EVALUATED);
      }
      return iucn;
    }
    // all species that have no matching IUCN usage should become NE
    // if they are classified into a proper kingdom and are no OTU names
    // https://github.com/gbif/pipelines/issues/645
    NameUsage nub = nameUsageService.get(usageKey, Locale.US);
    if (nub != null && Rank.SPECIES.equals(nub.getRank())
        && !Objects.equals(Kingdom.INCERTAE_SEDIS.nubUsageKey(), nub.getKingdomKey())
        && nub.getNameType() == NameType.SCIENTIFIC) {
      iucn = new IucnRedListCategory();
      iucn.setCategory(ThreatStatus.NOT_EVALUATED);
      iucn.setScientificName(nub.getScientificName());
      iucn.setTaxonomicStatus(nub.getTaxonomicStatus());
      iucn.setAcceptedName(nub.getAccepted());
      return iucn;
    }
    return null;
  }

  /**
   * This retrieves a list of root NameUsage for a Checklist from ChecklistBank.
   *
   * @param datasetKey UUID or case-insensitive shortname of the Checklist to retrieve
   * @param page the limit, offset, and count paging information
   * @return requested list of NameUsage or an empty list if none could be found
   * @see NameUsageService#listRoot(UUID, Locale, Pageable)
   */
  @Operation(
    operationId = "getRootUsages",
    summary = "Root name usages of a dataset",
    description = "Retrieves root name usages for a checklist dataset."
  )
  @Tag(name = "Species")
  @Parameter(
    name = DATASET_KEY,
    description = "A UUID of a checklist dataset.",
    example = "d7dddbf4-2cf0-4f39-9b2a-bb099caae36c"
  )
  @NameUsageLanguageParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Root name usages found")
  @DefaultBadResponses
  @GetMapping("root/{datasetKey}")
  public PagingResponse<NameUsage> listRootUsages(
      @PathVariable(DATASET_KEY) UUID datasetKey, Pageable page) {
    return nameUsageService.listRoot(datasetKey, LocaleContextHolder.getLocale(), page);
  }

  @Operation(
    operationId = "getNameUsageRootAll",
    summary = "All root name usages of a dataset",
    description = "Retrieves a brief list of all root Name Usages for a checklist dataset."
  )
  @Tag(name = "Species")
  @Parameter(
    name = DATASET_KEY,
    description = "A UUID of a checklist dataset.",
    example = "d7dddbf4-2cf0-4f39-9b2a-bb099caae36c"
  )
  @ApiResponse(responseCode = "200", description = "Root name usages found")
  @DefaultBadResponses
  @Hidden
  @GetMapping("rootAll/{datasetKey}")
  public List<UsageCount> root(@PathVariable("datasetKey") UUID datasetKey) {
    return usageCountMapper.root(datasetKey);
  }

  /** Not sure why this exists */
  @Hidden
  @Deprecated
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

  private void addChildrenRecursively(
      TreeContainer<UsageCount, Integer> tree, int parent, int rankIdx, Rank... ranks) {
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

  /* Same parameters for search and suggest queries. */
  @Target({METHOD, ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Parameters(
    value = {
      @Parameter(
        name = DATASET_KEY,
        description = "A UUID of a checklist dataset.",
        schema = @Schema(implementation = UUID.class),
        example = "d7dddbf4-2cf0-4f39-9b2a-bb099caae36c",
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "constituentKey",
        description = "The (sub)dataset constituent key as a UUID. Useful to query larger assembled datasets such as " +
          "the GBIF Backbone or the Catalogue of Life",
        schema = @Schema(implementation = UUID.class),
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "rank",
        description = "Filters by taxonomic rank as given in our https://api.gbif.org/v1/enumeration/basic/Rank[Rank enum].",
        schema = @Schema(implementation = Rank.class),
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "higherTaxonKey",
        description = "Filters by any of the higher Linnean rank keys. Note this is within the respective checklist " +
          "and not searching NUB keys across all checklists.",
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "status",
        description = "Filters by the taxonomic status as given in our https://api.gbif.org/v1/enumeration/basic/TaxonomicStatus[TaxonomicStatus enum].",
        schema = @Schema(implementation = TaxonomicStatus.class),
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "isExtinct",
        description = "Filters by extinction status.",
        schema = @Schema(implementation = Boolean.class),
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "habitat",
        description = "Filters by the habitat. Currently only 3 major biomes are accepted in our https://api.gbif.org/v1/enumeration/basic/Habitat[Habitat enum].",
        schema = @Schema(implementation = Habitat.class),
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "threat",
        description = "Filters by the taxonomic threat status as given in our https://api.gbif.org/v1/enumeration/basic/ThreatStatus[ThreatStatus enum].",
        schema = @Schema(implementation = ThreatStatus.class),
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "nameType",
        description = "Filters by the name type as given in our https://api.gbif.org/v1/enumeration/basic/NameType[NameType enum].",
        schema = @Schema(implementation = NameType.class),
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "nomenclaturalStatus",
        description = "Filters by the nomenclatural status as given in our https://api.gbif.org/v1/enumeration/basic/NomenclaturalStatus[Nomenclatural Status enum].",
        schema = @Schema(implementation = NomenclaturalStatus.class),
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "origin",
        description = "Filters for name usages with a specific origin.",
        schema = @Schema(implementation = Origin.class),
        in = ParameterIn.QUERY
      ),
      @Parameter(
        name = "issue",
        description = "A specific indexing issue as defined in our https://api.gbif.org/v1/enumeration/basic/NameUsageIssue[NameUsageIssue enum].",
        schema = @Schema(implementation = NameUsageIssue.class),
        in = ParameterIn.QUERY
      )
    }
  )
  @interface NameUsageSearchParameters{}

  @Operation(
    operationId = "searchNames",
    summary = "Full text search over name usages",
    description = "Full-text search of name usages covering the scientific and vernacular names, the species " +
      "description, distribution and the entire classification across all name usages of all or some checklists.\n\n" +
      "Results are ordered by relevance as this search usually returns a lot of results.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0110"))
  )
  @Tag(name = "Searching names")
  @NameUsageSearchParameters
  @CommonParameters.QParameter
  @CommonParameters.HighlightParameter
  @Pageable.OffsetLimitParameters
  @FacetedSearchRequest.FacetParameters
  @Parameter(
    name = "searchRequest",
    hidden = true
  )
  @ApiResponse(responseCode = "200", description = "Name usages found")
  @DefaultBadResponses
  @GetMapping("search")
  public SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> search(
      NameUsageSearchRequest searchRequest) {
    // POR-2801
    // protect against deep paging requests which blow heap
    checkDeepPaging(searchRequest);
    return searchService.search(searchRequest);
  }

  @Operation(
    operationId = "suggestNames",
    summary = "Name autocomplete service",
    description = "A quick and simple autocomplete service that returns up to 20 name usages by doing prefix " +
      "matching against the scientific name.\n\n" +
      "Results are ordered by relevance.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0150"))
  )
  @Tag(name = "Searching names")
  @NameUsageSearchParameters
  @CommonParameters.QParameter
  @Parameter(
    name = "searchSuggestRequest",
    hidden = true
  )
  @ApiResponse(responseCode = "200", description = "Name usage suggestions found")
  @DefaultBadResponses
  @GetMapping("suggest")
  public List<NameUsageSuggestResult> suggest(NameUsageSuggestRequest searchSuggestRequest) {
    // POR-2801
    // protect against deep paging requests which blow heap
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
      throw new IllegalArgumentException(
          "Offset is limited for this operation to " + DEEP_PAGING_OFFSET_LIMIT);
    }
  }
}
