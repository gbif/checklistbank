package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.vocabulary.Language;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DescriptionServiceMyBatisIT {

  private final Integer USAGE_ID = 100000040;

  @Rule
  public DatabaseDrivenChecklistBankTestRule<DescriptionService> ddt =
    new DatabaseDrivenChecklistBankTestRule<DescriptionService>(DescriptionService.class);

  @Test
  public void testGet() {
    Description description = ddt.getService().get(26);
    assertEquals(USAGE_ID, description.getUsageKey());
    assertEquals(
      "The Caucasian squirrel (or Persian squirrel) is a tree squirrel in the genus Sciurus endemic to Armenia, Azerbaijan, Georgia, Greece, Iran, Iraq, Israel, Jordan, Lebanon, Syria, and Turkey. Its natural habitat is temperate broadleaf and mixed forests.[1]",
      description.getDescription());
    assertEquals(Language.ENGLISH, description.getLanguage());
    assertEquals("general", description.getType());
    assertNull(description.getSource());
    assertNull(description.getContributor());
    assertNull(description.getCreator());
  }

  @Test
  public void testListByChecklistUsage() {
    List<Description> descriptions = ddt.getService().listByUsage(USAGE_ID, null).getResults();
    assertEquals(3, descriptions.size());
    assertEquals((Integer) 25, descriptions.get(0).getKey());
    assertEquals((Integer) 26, descriptions.get(1).getKey());
    assertEquals((Integer) 27, descriptions.get(2).getKey());

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    Description d1 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    Description d2 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, descriptions.get(0));
    assertEquals(d2, descriptions.get(1));
  }

  @Test
  public void testListByRange() {
    List<Description> records = ((DescriptionServiceMyBatis) ddt.getService()).listRange(1, 100000020);
    assertEquals(9, records.size());
    for (Description v : records) {
      assertNotNull(v.getUsageKey());
    }
  }
}
