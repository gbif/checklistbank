package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;
import org.gbif.ws.client.BaseWsGetClient;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the ChecklistUsageService.
 */
public class NameUsageWsClient extends BaseWsGetClient<NameUsage, Integer> implements NameUsageService {

  protected final GenericType<PagingResponse<NameUsage>> tPage = new GenericType<PagingResponse<NameUsage>>() {
  };
  private final GenericType<List<NameUsage>> list = new GenericType<List<NameUsage>>() {
  };
  private final GenericType<ParsedName> pname = new GenericType<ParsedName>() {
  };
  private final GenericType<NameUsageMetrics> metrics = new GenericType<NameUsageMetrics>() {
  };
  private final GenericType<VerbatimNameUsage> tVerbatim = new GenericType<VerbatimNameUsage>() {
  };

  @Inject
  public NameUsageWsClient(@ChecklistBankWs WebResource resource) {
    super(NameUsage.class, resource.path(Constants.SPECIES_PATH), null);
  }


  /**
   * Gets a NameUsage by its key from the Web service.
   *
   * @param usageKey key of NubUsage to get the list the synonyms of
   * @param locale   identifier for a region
   *
   * @return requested NameUsage or null if none couldn't be found
   */
  @Override
  public NameUsage get(int usageKey, Locale locale) {
    return get(locale, String.valueOf(usageKey));
  }

  @Override
  public VerbatimNameUsage getVerbatim(int usageKey) {
    return get(tVerbatim, String.valueOf(usageKey), Constants.VERBATIM_PATH);
  }

  @Override
  public ParsedName getParsedName(int usageKey) {
    return get(pname, String.valueOf(usageKey), Constants.PARSED_NAME_PATH);
  }

  @Nullable
  @Override
  public NameUsageMetrics getMetrics(int usageKey) {
    return get(metrics, String.valueOf(usageKey), Constants.METRICS_PATH);
  }

  @Override
  public PagingResponse<NameUsage> list(Locale locale, @Nullable UUID datasetKey, @Nullable String sourceId,
    @Nullable Pageable page) {
    return get(tPage, locale,
      QueryParamBuilder.create(Constants.DATASET_KEY, datasetKey, Constants.SOURCE_ID, sourceId).build(), page);
  }

  @Override
  public PagingResponse<NameUsage> listByCanonicalName(Locale locale, String canonicalName, @Nullable Pageable page,
    @Nullable UUID... datasetKey) {
    return get(tPage, locale,
      QueryParamBuilder.create(Constants.DATASET_KEY, datasetKey, Constants.CANONICAL_NAME, canonicalName).build(), page);
  }

  /**
   * Gets a list of parents for a NameUsage from the web service.
   * This calls the URL {@code /species/<k1>/parents}.
   *
   * @param usageKey key of the NameUsage to get its parents for
   * @param locale   identifier for a region
   *
   * @return requested list of NameUsage parents or an empty list if none could be found
   */
  @Override
  public List<NameUsage> listParents(int usageKey, Locale locale) {
    return get(list, locale, null, null, String.valueOf(usageKey), Constants.PARENTS_PATH);
  }

  /**
   * Gets a list of root NameUsage for a Checklist from the Web service.
   * This calls the URL {@code /checklist/<k1>/usages}.
   *
   * @param datasetKey key of Checklist to get root NameUsage for
   * @param locale     identifier for a region
   * @param page       paging parameters or null for first page with default size
   *
   * @return requested list of NameUsage or an empty list if none could be found
   */
  @Override
  public PagingResponse<NameUsage> listRoot(UUID datasetKey, Locale locale, @Nullable Pageable page) {
    return get(tPage, locale, null, page, Constants.ROOT_USAGES_PATH, datasetKey.toString());
  }


  /**
   * Gets a list of relates NameUsages for a NameUsage from the web service.
   * This calls the URL {@code /species/<k1>/related}.
   *
   * @param usageKey key of the NameUsage to get its related usages for
   * @param locale   identifier for a region
   *
   * @return requested list of related NameUsages or an empty list if none could be found
   */
  @Override
  public List<NameUsage> listRelated(int usageKey, Locale locale, @Nullable UUID... datasetKey) {
    return get(list, locale, QueryParamBuilder.create(Constants.DATASET_KEY, datasetKey).build(), null,
      String.valueOf(usageKey), Constants.RELATED_PATH);
  }

  /**
   * Gets a list of children NameUsage for a parent NameUsage from the Web service.
   * This calls the URL {@code /species/<k1>/children}.
   *
   * @param parentKey key of NameUsage to get children NameUsage for
   * @param locale    identifier for a region
   * @param page      paging parameters or null for first page with default size
   *
   * @return requested list of NameUsage or an empty list if none could be found
   */
  @Override
  public PagingResponse<NameUsage> listChildren(int parentKey, Locale locale, @Nullable Pageable page) {
    return get(tPage, locale, null, page, String.valueOf(parentKey), Constants.CHILDREN_PATH);
  }

  /**
   * Gets a list of synonym NameUsage for a NameUsage from the Web service.
   * This calls the URL {@code /species/<k1>/synonyms}.
   *
   * @param usageKey key of NameUsage to get synonym NameUsage for
   * @param locale   identifier for a region
   * @param page     paging parameters or null for first page with default size
   *
   * @return requested list of NameUsage or an empty list if none could be found
   */
  @Override
  public PagingResponse<NameUsage> listSynonyms(int usageKey, Locale locale, @Nullable Pageable page) {
    return get(tPage, locale, null, page, String.valueOf(usageKey), Constants.SYNONYMS_PATH);
  }

}
