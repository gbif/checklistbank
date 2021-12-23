package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeDesignationType;

import java.text.ParseException;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TypeSpecimenServiceMyBatisTestIT extends MyBatisServiceITBase {

  private final TypeSpecimenService service;

  private final Integer USAGE_ID = 100000006;

  @Autowired
  public TypeSpecimenServiceMyBatisTestIT(
      DataSource dataSource, TypeSpecimenService typeSpecimenService) {
    super(dataSource);
    this.service = typeSpecimenService;
  }

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
    PagingResponse<TypeSpecimen> typeSpecimens = service.listByUsage(USAGE_ID, null);
    assertEquals(1, typeSpecimens.getResults().size());
    assertTrue(typeSpecimens.isEndOfRecords());
    verify8(typeSpecimens.getResults().get(0));

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    TypeSpecimen d1 = service.listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, typeSpecimens.getResults().get(0));

    page = new PagingRequest(1, 1);
    assertTrue(service.listByUsage(USAGE_ID, page).getResults().isEmpty());
  }
}
