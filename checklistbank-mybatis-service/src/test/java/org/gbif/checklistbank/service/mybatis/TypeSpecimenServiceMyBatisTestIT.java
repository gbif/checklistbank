package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeDesignationType;
import org.gbif.api.vocabulary.TypeStatus;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TypeSpecimenServiceMyBatisTestIT {

  private final Integer USAGE_ID = 100000006;
  private final UUID SQUIRREL_DATASET = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");

  @Rule
  public DatabaseDrivenChecklistBankTestRule<TypeSpecimenService> ddt =
    new DatabaseDrivenChecklistBankTestRule<TypeSpecimenService>(TypeSpecimenService.class);

  @Test
  public void testGet() throws ParseException {

    TypeSpecimen typeSpecimen = ddt.getService().get(15);
    assertEquals(USAGE_ID, typeSpecimen.getUsageKey());
    assertEquals(SQUIRREL_DATASET, typeSpecimen.getDatasetKey());
    assertEquals(TypeStatus.TYPE_SPECIES, typeSpecimen.getTypeStatus());
    assertEquals("Raciurus pusillus", typeSpecimen.getScientificName());
    assertEquals("Thomas", typeSpecimen.getTypeDesignatedBy());
    assertEquals("Country File, BBC, 28. 89. 2008", typeSpecimen.getCitation());
    assertEquals(Rank.SPECIES, typeSpecimen.getTaxonRank());
    assertEquals("555", typeSpecimen.getOccurrenceId());
    assertEquals("BGBM", typeSpecimen.getInstitutionCode());
    assertEquals("Squirrels", typeSpecimen.getCollectionCode());
    assertEquals("Z22", typeSpecimen.getCatalogNumber());
    assertEquals("French Guiana, Cayenne", typeSpecimen.getLocality());
    assertEquals("Forestry Commission - Red Squirrels", typeSpecimen.getSource());
    assertEquals("Charles III", typeSpecimen.getRecordedBy());
    assertEquals("Around summer 2000", typeSpecimen.getVerbatimEventDate());
    assertEquals("label", typeSpecimen.getVerbatimLabel());
    assertEquals("55 degrees", typeSpecimen.getVerbatimLongitude());
    assertEquals("10 degrees", typeSpecimen.getVerbatimLatitude());
    assertEquals(TypeDesignationType.ORIGINAL_DESIGNATION, typeSpecimen.getTypeDesignationType());
  }

  @Test
  public void testListByChecklistUsage() {
    List<TypeSpecimen> typeSpecimens = ddt.getService().listByUsage(USAGE_ID, null).getResults();
    assertEquals(2, typeSpecimens.size());
    assertEquals((Integer) 15, typeSpecimens.get(0).getKey());

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    TypeSpecimen d1 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    TypeSpecimen d2 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, typeSpecimens.get(0));
    assertEquals(d2, typeSpecimens.get(1));
  }

}
