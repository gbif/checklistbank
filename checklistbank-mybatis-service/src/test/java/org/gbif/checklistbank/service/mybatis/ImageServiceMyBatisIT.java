package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Image;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.ImageService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ImageServiceMyBatisIT {

  private final Integer USAGE_ID = 100000025;
  private final UUID SQUIRREL_UUID = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");

  @Rule
  public DatabaseDrivenChecklistBankTestRule<ImageService> ddt =
    new DatabaseDrivenChecklistBankTestRule<ImageService>(ImageService.class);

  @Test
  public void testGet() throws ParseException {
    Image image = ddt.getService().get(100010);
    assertEquals(USAGE_ID, image.getUsageKey());
    assertEquals(SQUIRREL_UUID, image.getDatasetKey());
    assertEquals("2008-01-01", image.getCreated());
    assertEquals("Prashanthns", image.getCreator());
    assertEquals("Eurasian Red Squirrel", image.getTitle());
    assertEquals("A Eurasian Red Squirrel seen from below. Photographed in Wagenigen, Nederlands.", image.getDescription());
    assertEquals("http://upload.wikimedia.org/wikipedia/commons/e/e2/Eurasian_Red_Squirrel.jpg", image.getImage());
    assertEquals("Creative Commons Attribution 3.0 Unported", image.getLicense());
    assertNull(image.getPublisher());
    assertEquals("http://en.wikipedia.org/wiki/File:Eurasian_Red_Squirrel.jpg", image.getLink());
  }

  @Test
  public void testListByChecklistUsage() {
    List<Image> images = ddt.getService().listByUsage(USAGE_ID, null).getResults();
    assertEquals(8, images.size());
    assertEquals((Integer) 100010, images.get(0).getKey());
    assertEquals((Integer) 100011, images.get(1).getKey());
    assertEquals((Integer) 100012, images.get(2).getKey());
    assertEquals((Integer) 100013, images.get(3).getKey());

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    Image d1 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    Image d2 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, images.get(0));
    assertEquals(d2, images.get(1));
  }
}
