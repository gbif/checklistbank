package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.IdentifierService;
import org.gbif.api.service.checklistbank.MultimediaService;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.service.checklistbank.ReferenceService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.api.vocabulary.TypeStatus;
import org.gbif.checklistbank.service.mybatis.mapper.DistributionMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NubRelMapper;
import org.gbif.checklistbank.service.mybatis.mapper.UsageCountMapper;

import java.util.List;
import java.util.Locale;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpeciesResourceTest {

  private NameUsageService mockNameUsageService;
  private VernacularNameService mockVernacularNameService;
  private TypeSpecimenService mockTypeSpecimenService;
  private SpeciesProfileService mockSpeciesProfileService;
  private ReferenceService mockReferenceService;
  private MultimediaService mockImageService;
  private DescriptionService mockDescriptionService;
  private DistributionService mockDistributionService;
  private IdentifierService mockIdentifierService;
  private NameUsageSearchService mockSearchService;
  private UsageCountMapper mockUsageCountMapper;
  private DistributionMapper mockDistributionMapper;
  private NubRelMapper mockNubRelMapper;

  private SpeciesResource resource;

  private NameUsage nameUsage;
  private Locale locale;
  private static final int NAME_USAGE_KEY = 103350120;
  private Pageable page;
  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> searchResponse;

  @Before
  public void setUp() throws Exception {
    mockNameUsageService = mock(NameUsageService.class);
    mockVernacularNameService = mock(VernacularNameService.class);
    mockTypeSpecimenService = mock(TypeSpecimenService.class);
    mockSpeciesProfileService = mock(SpeciesProfileService.class);
    mockReferenceService = mock(ReferenceService.class);
    mockImageService = mock(MultimediaService.class);
    mockDescriptionService = mock(DescriptionService.class);
    mockDistributionService = mock(DistributionService.class);
    mockIdentifierService = mock(IdentifierService.class);
    mockSearchService = mock(NameUsageSearchService.class);
    mockUsageCountMapper = mock(UsageCountMapper.class);
    mockDistributionMapper = mock(DistributionMapper.class);
    mockNubRelMapper = mock(NubRelMapper.class);

    searchResponse = new SearchResponse<NameUsageSearchResult, NameUsageSearchParameter>(0, 20);
    searchResponse.setCount(1000L);

    resource = new SpeciesResource(mockNameUsageService, mockVernacularNameService, mockTypeSpecimenService,
      mockSpeciesProfileService, mockReferenceService, mockImageService, mockDescriptionService,
      mockDistributionService, mockIdentifierService, mockSearchService, mockUsageCountMapper,
      mockDistributionMapper, mockNubRelMapper);

    locale = Locale.US;
    nameUsage = new NameUsage();
    nameUsage.setKey(NAME_USAGE_KEY);

    page = new PagingRequest(0, 1);
  }

  @Test
  public void testGet() throws Exception {
    when(mockNameUsageService.get(NAME_USAGE_KEY, locale)).thenReturn(nameUsage);
    NameUsage nameUsage = resource.get(NAME_USAGE_KEY, locale);
    assertEquals(String.valueOf(NAME_USAGE_KEY), nameUsage.getKey().toString());
  }

  @Test
  public void testListTypeSpecimensByNameUsage() {
    TypeSpecimen ts = new TypeSpecimen();
    ts.setSourceTaxonKey(NAME_USAGE_KEY);
    ts.setTypeStatus(TypeStatus.PARATYPE);

    List<TypeSpecimen> list = Lists.newLinkedList();
    list.add(ts);
    PagingResponse<TypeSpecimen> response = new PagingResponse<TypeSpecimen>(0, 1, 1l, list);

    when(mockTypeSpecimenService.listByUsage(NAME_USAGE_KEY, page)).thenReturn(response);

    PagingResponse<TypeSpecimen> result = resource.listTypeSpecimensByNameUsage(NAME_USAGE_KEY, page);
    assertEquals(result.getResults().get(0).getSourceTaxonKey().toString(), String.valueOf(NAME_USAGE_KEY));
  }

  @Test
  public void testSearch() throws Exception {
    when(mockSearchService.search(Matchers.any(NameUsageSearchRequest.class))).thenReturn(searchResponse);
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> searchResponse2 = resource.search(new NameUsageSearchRequest(1000L, 20));
    assertNotNull(searchResponse2);
  }
}
