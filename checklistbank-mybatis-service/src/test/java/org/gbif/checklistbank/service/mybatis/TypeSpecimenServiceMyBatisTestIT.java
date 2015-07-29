package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeDesignationType;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.text.ParseException;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TypeSpecimenServiceMyBatisTestIT {

  private final Integer USAGE_ID = 100000006;

  @Rule
  public DatabaseDrivenChecklistBankTestRule<TypeSpecimenService> ddt = DatabaseDrivenChecklistBankTestRule.squirrels(TypeSpecimenService.class);

  public void verify8(TypeSpecimen typeSpecimen) throws ParseException {
    assertNull(typeSpecimen.getSourceTaxonKey());
    assertEquals("Sciurus pusillus Desmarest, 1817", typeSpecimen.getScientificName());
    assertEquals("Thomas", typeSpecimen.getTypeDesignatedBy());
    assertEquals(Rank.SPECIES, typeSpecimen.getTaxonRank());
    assertEquals("Forestry Commission - Red Squirrels", typeSpecimen.getSource());
    assertEquals(TypeDesignationType.ORIGINAL_DESIGNATION, typeSpecimen.getTypeDesignationType());
  }

  @Test
  public void testListByChecklistUsage() throws ParseException {
    PagingResponse<TypeSpecimen> typeSpecimens = ddt.getService().listByUsage(USAGE_ID, null);
    assertEquals(1, typeSpecimens.getResults().size());
    assertTrue(typeSpecimens.isEndOfRecords());
    verify8(typeSpecimens.getResults().get(0));

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    TypeSpecimen d1 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, typeSpecimens.getResults().get(0));

    page = new PagingRequest(1, 1);
    assertTrue(ddt.getService().listByUsage(USAGE_ID, page).getResults().isEmpty());
  }

}
