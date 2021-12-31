/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.ChecklistbankMyBatisServiceITBase;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.MultimediaService;
import org.gbif.api.vocabulary.MediaType;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;

import java.net.URI;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestData(name = "squirrels")
public class MultimediaServiceChecklistbankMyBatisIT extends ChecklistbankMyBatisServiceITBase {

  private final MultimediaService service;

  private final Integer USAGE_ID = 100000025;

  @Autowired
  public MultimediaServiceChecklistbankMyBatisIT(DataSource dataSource, MultimediaService multimediaService) {
    super(dataSource);
    this.service = multimediaService;
  }

  @Test
  public void testListByChecklistUsage() {
    List<NameUsageMediaObject> images = service.listByUsage(USAGE_ID, null).getResults();
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
    assertEquals(
        "A Eurasian Red Squirrel seen from below. Photographed in Wagenigen, Nederlands.",
        image.getDescription());
    assertEquals(
        URI.create("http://upload.wikimedia.org/wikipedia/commons/e/e2/Eurasian_Red_Squirrel.jpg"),
        image.getIdentifier());
    assertEquals("Creative Commons Attribution 3.0 Unported", image.getLicense());
    assertNull(image.getPublisher());
    assertEquals(
        URI.create("http://en.wikipedia.org/wiki/File:Eurasian_Red_Squirrel.jpg"),
        image.getReferences());

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    NameUsageMediaObject d1 = service.listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    NameUsageMediaObject d2 = service.listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, images.get(0));
    assertEquals(d2, images.get(1));
  }

  @Test
  public void testBadUri() {
    List<NameUsageMediaObject> media = service.listByUsage(100000030, null).getResults();
    assertEquals(2, media.size());
    // http less URL, should work
    NameUsageMediaObject img = media.get(0);
    assertNotNull(img);
    assertEquals(89, img.getCreated().getYear());
    assertEquals(10, img.getCreated().getMonth());
    assertEquals(
        URI.create(
            "http://www.wikimedia.org/wikipedia/commons/thumb/d/d8/Sciurus_anomalus_-_Jeita_Grotto.jpg/724px-Sciurus_anomalus_-_Jeita_Grotto.jpg"),
        img.getIdentifier());
    assertEquals(URI.create("http://www.gbif.org"), img.getReferences());
    assertEquals("Persian Squirrel in a cage", img.getDescription());
    assertEquals(MediaType.StillImage, img.getType());
    // citation instead of URI, should be ignored
    img = media.get(1);
    assertNotNull(img);
    assertEquals(89, img.getCreated().getYear());
    assertEquals(10, img.getCreated().getMonth());
    assertEquals("Persian Squirrel in a cage", img.getDescription());
    assertNull(img.getIdentifier());
    assertNull(img.getReferences());
    assertNull(img.getType());
  }
}
