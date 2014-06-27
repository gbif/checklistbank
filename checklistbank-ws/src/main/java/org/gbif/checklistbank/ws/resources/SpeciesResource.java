package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.TableOfContents;
import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.MultimediaService;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.service.checklistbank.ReferenceService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Species resource.
 */
@Path("/species")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
public class SpeciesResource {

  private static final Logger LOG = LoggerFactory.getLogger(SpeciesResource.class);
  private static final String DATASET_KEY = "datasetKey";

  private final NameUsageService nameUsageService;
  private final VernacularNameService vernacularNameService;
  private final TypeSpecimenService typeSpecimenService;
  private final SpeciesProfileService speciesProfileService;
  private final ReferenceService referenceService;
  private final MultimediaService imageService;
  private final DescriptionService descriptionService;
  private final DistributionService distributionService;

  @Inject
  public SpeciesResource(NameUsageService nameUsageService, VernacularNameService vernacularNameService,
    TypeSpecimenService typeSpecimenService, SpeciesProfileService speciesProfileService,
    ReferenceService referenceService, MultimediaService imageService, DescriptionService descriptionService,
    DistributionService distributionService) {
    this.nameUsageService = nameUsageService;
    this.vernacularNameService = vernacularNameService;
    this.typeSpecimenService = typeSpecimenService;
    this.speciesProfileService = speciesProfileService;
    this.referenceService = referenceService;
    this.imageService = imageService;
    this.descriptionService = descriptionService;
    this.distributionService = distributionService;
  }

  /**
   * This retrieves a list of all NameUsage from ChecklistBank.
   *
   * @param locale      identifier for a region
   * @param datasetKeys the optional checklist keys to limit paging to
   * @param page        the limit, offset paging information
   *
   * @return requested list of NameUsage or an empty list if none could be found
   */
  @GET
  public PagingResponse<NameUsage> list(@Context Locale locale, @QueryParam(DATASET_KEY) Set<UUID> datasetKeys,
    @QueryParam("sourceId") String sourceId, @QueryParam("name") String canonicalName, @Context Pageable page) {
    LOG.debug("Request all usages: [pageSize({}) offset({})]", page.getLimit(), page.getOffset());
    if (datasetKeys == null) {
      datasetKeys = ImmutableSet.of();
    }
    if (Strings.isNullOrEmpty(canonicalName)) {
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
   * @param usageKey  NameUsage key
   * @param locale    identifier for a region
   *
   * @return requested NameUsage or null if none could be found. List of NameUsage in case of a search.
   *
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
    LOG.debug("Request ParsedName for usage [{}]:", usageKey);
    return nameUsageService.getParsedName(usageKey);
  }

  @GET
  @Path("{id}/verbatim")
  @NullToNotFound
  public VerbatimNameUsage getVerbatim(@PathParam("id") int usageKey) {
    LOG.debug("Request VerbatimNameUsage for usage [{}]:", usageKey);
    return nameUsageService.getVerbatim(usageKey);
  }

  /**
   * This retrieves a list of children NameUsage for a parent NameUsage from ChecklistBank.
   *
   * @param parentKey parent NameUsage key
   * @param locale    identifier for a region
   * @param page      the limit, offset paging information
   *
   * @return requested list of NameUsage or an empty list if none could be found
   *
   * @see NameUsageService#listChildren(int, Locale, Pageable)
   */
  @GET
  @Path("{id}/children")
  public PagingResponse<NameUsage> listChildren(@PathParam("id") int parentKey, @Context Locale locale,
    @Context Pageable page) {
    LOG.debug("Request children usages for parent NameUsage [{}]: [pageSize({}) offset({})]",
      new Object[] {parentKey, page.getLimit(), page.getOffset()});
    return nameUsageService.listChildren(parentKey, locale, page);
  }

  /**
   * This retrieves a list of synonym NameUsage for a NameUsage from ChecklistBank.
   *
   * @param usageKey parent NameUsage key
   * @param locale   identifier for a region
   * @param page     the limit, offset, and count paging information
   *
   * @return requested list of NameUsage or an empty list if none could be found
   *
   * @see NameUsageService#listChildren(int, Locale, Pageable)
   */
  @GET
  @Path("{id}/synonyms")
  public PagingResponse<NameUsage> listSynonyms(@PathParam("id") int usageKey, @Context Locale locale,
    @Context Pageable page) {
    LOG.debug("Request synonym usages for NameUsage [{}]: [pageSize({}) offset({})]",
      new Object[] {usageKey, page.getLimit(), page.getOffset()});
    return nameUsageService.listSynonyms(usageKey, locale, page);
  }


  /**
   * This retrieves all VernacularNames for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   *
   * @return a list of all VernacularNames
   *
   * @see VernacularNameService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/vernacularNames")
  public PagingResponse<VernacularName> listVernacularNamesByNameUsage(@PathParam("id") int usageKey,
    @Context Pageable page) {
    LOG.debug("Request all VernacularNames for NameUsage [{}]: [pageSize({}) offset({})]",
      new Object[] {usageKey, page.getLimit(), page.getOffset()});
    return vernacularNameService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all TypeSpecimens for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   *
   * @return a list of all TypeSpecimens
   *
   * @see TypeSpecimenService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/typeSpecimens")
  public PagingResponse<TypeSpecimen> listTypeSpecimensByNameUsage(@PathParam("id") int usageKey,
    @Context Pageable page) {
    LOG.debug("Request all TypeSpecimens for NameUsage [{}]: [pageSize({}) offset({})]",
      new Object[] {usageKey, page.getLimit(), page.getOffset()});
    return typeSpecimenService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all SpeciesProfiles for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   *
   * @return a list of all SpeciesProfiles
   *
   * @see SpeciesProfileService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/speciesProfiles")
  public PagingResponse<SpeciesProfile> listSpeciesProfilesByNameUsage(@PathParam("id") int usageKey,
    @Context Pageable page) {
    LOG.debug("Request all SpeciesProfiles for NameUsage [{}]: [pageSize({}) offset({})]",
      new Object[] {usageKey, page.getLimit(), page.getOffset()});
    return speciesProfileService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all References for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   *
   * @return a list of all References
   *
   * @see ReferenceService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/references")
  public PagingResponse<Reference> listReferencesByNameUsage(@PathParam("id") int usageKey, @Context Pageable page) {
    LOG.debug("Request all References for NameUsage [{}]: [pageSize({}) offset({})]",
      new Object[] {usageKey, page.getLimit(), page.getOffset()});
    return referenceService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all multimedia objects for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   *
   * @return a list of all Media objects
   */
  @GET
  @Path("{id}/media")
  public PagingResponse<NameUsageMediaObject> listImagesByNameUsage(@PathParam("id") int usageKey, @Context Pageable page) {
    LOG.debug("Request all Images for NameUsage [{}]: [pageSize({}) offset({})]",
      new Object[] {usageKey, page.getLimit(), page.getOffset()});
    return imageService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all Descriptions for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   *
   * @return a list of all Descriptions
   *
   * @see DescriptionService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/descriptions")
  public PagingResponse<Description> listDescriptionsByNameUsage(@PathParam("id") int usageKey,
    @Context Pageable page) {
    LOG.debug("Request all Descriptions for NameUsage [{}]: [pageSize({}) offset({})]",
      new Object[] {usageKey, page.getLimit(), page.getOffset()});
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
   *
   * @return a list of all Distributions
   *
   * @see DistributionService#listByUsage(int, Pageable)
   */
  @GET
  @Path("{id}/distributions")
  public PagingResponse<Distribution> listDistributionsByNameUsage(@PathParam("id") int usageKey,
    @Context Pageable page) {
    LOG.debug("Request all Distributions for NameUsage [{}]: [pageSize({}) offset({})]",
      new Object[] {usageKey, page.getLimit(), page.getOffset()});
    return distributionService.listByUsage(usageKey, page);
  }

  /**
   * This retrieves all related Usages for a NameUsage from ChecklistBank.
   *
   * @param usageKey    NameUsage key
   * @param datasetKeys The optional list of dataset keys to filter related usages
   *
   * @return a list of all Related usages
   *
   * @see NameUsageService#listRelated(int, Locale, UUID[])
   */
  @GET
  @Path("{id}/related")
  public List<NameUsage> listRelatedByNameUsage(@PathParam("id") int usageKey, @Context Locale locale,
    @QueryParam(DATASET_KEY) Set<UUID> datasetKeys) {
    LOG.debug("Request all Related usages for NameUsage [{}] in checklists {}", usageKey, datasetKeys);
    return nameUsageService.listRelated(usageKey, locale, datasetKeys.toArray(new UUID[datasetKeys.size()]));
  }

  /**
   * This retrieves all Parents for a NameUsage from ChecklistBank.
   *
   * @param usageKey NameUsage key
   * @param page     The page and offset and count information
   *
   * @return a list of all Parents
   *
   * @see NameUsageService#listParents(int, Locale)
   */
  @GET
  @Path("{id}/parents")
  public List<NameUsage> listParentsByNameUsage(@PathParam("id") int usageKey, @Context Locale locale,
    @Context Pageable page) {
    LOG.debug("Request all parents for a NameUsage [{}]: [pageSize({}) offset({})]", usageKey, page.getLimit(), page.getOffset());
    return nameUsageService.listParents(usageKey, locale);
  }

  /**
   * This retrieves a list of root NameUsage for a Checklist from ChecklistBank.
   *
   * @param datasetKey UUID or case insensitive shortname of the Checklist to retrieve
   * @param locale     identifier for a region
   * @param page       the limit, offset, and count paging information
   *
   * @return requested list of NameUsage or an empty list if none could be found
   *
   * @see NameUsageService#listRoot(UUID, Locale, Pageable)
   */
  @GET
  @Path("root/{datasetKey}")
  public PagingResponse<NameUsage> listRootUsages(@PathParam(DATASET_KEY) UUID datasetKey, @Context Locale locale,
    @Context Pageable page) {
    LOG.debug("Request root usages for Checklist [{}]: [pageSize({}) offset({})]",datasetKey, page.getLimit(), page.getOffset());
    return nameUsageService.listRoot(datasetKey, locale, page);
  }
}
