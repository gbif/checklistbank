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

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TypeSpecimenServiceMyBatisTestIT {

  private final Integer USAGE_ID = 100000006;

  @Rule
  public DatabaseDrivenChecklistBankTestRule<TypeSpecimenService> ddt =
    new DatabaseDrivenChecklistBankTestRule<TypeSpecimenService>(TypeSpecimenService.class);

  public void verify15(TypeSpecimen typeSpecimen) throws ParseException {

    assertNull(typeSpecimen.getSourceTaxonKey());
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
  public void testListByChecklistUsage() throws ParseException {
    List<TypeSpecimen> typeSpecimens = ddt.getService().listByUsage(USAGE_ID, null).getResults();
    assertEquals(2, typeSpecimens.size());
    verify15(typeSpecimens.get(0));

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    TypeSpecimen d1 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    TypeSpecimen d2 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, typeSpecimens.get(0));
    assertEquals(d2, typeSpecimens.get(1));
  }

}
