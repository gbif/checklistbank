package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.MultimediaService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MultimediaServiceMyBatisIT {

  private final Integer USAGE_ID = 100000025;

  @Rule
  public DatabaseDrivenChecklistBankTestRule<MultimediaService> ddt =
    new DatabaseDrivenChecklistBankTestRule<MultimediaService>(MultimediaService.class);

  @Test
  public void testListByChecklistUsage() {
    List<NameUsageMediaObject> images = ddt.getService().listByUsage(USAGE_ID, null).getResults();
    assertEquals(8, images.size());
    for (NameUsageMediaObject m : images) {
      assertNotNull(m.getIdentifier());
      assertNotNull(m.getType());
    }
    NameUsageMediaObject image = images.get(0); // 100010
    assertNull(image.getSourceTaxonKey());
    assertEquals("2008-01-01", DateFormatUtils.ISO_DATE_FORMAT.format(image.getCreated()));
    assertEquals("Prashanthns", image.getCreator());
    assertEquals("Eurasian Red Squirrel", image.getTitle());
    assertEquals("A Eurasian Red Squirrel seen from below. Photographed in Wagenigen, Nederlands.", image.getDescription());
    assertEquals(URI.create("http://upload.wikimedia.org/wikipedia/commons/e/e2/Eurasian_Red_Squirrel.jpg"), image.getIdentifier());
    assertEquals("Creative Commons Attribution 3.0 Unported", image.getLicense());
    assertNull(image.getPublisher());
    assertEquals(URI.create("http://en.wikipedia.org/wiki/File:Eurasian_Red_Squirrel.jpg"), image.getReferences());

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    NameUsageMediaObject d1 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    NameUsageMediaObject d2 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, images.get(0));
    assertEquals(d2, images.get(1));
  }

  @Test
  public void testBadDate() {
    NameUsageMediaObject img = ddt.getService().listByUsage(100000040, null).getResults().get(0);
    assertNotNull(img);
    assertNull(img.getCreated());
    assertNotNull(img.getDescription());
    assertNotNull(img.getIdentifier());
    assertNotNull(img.getType());
  }
}
