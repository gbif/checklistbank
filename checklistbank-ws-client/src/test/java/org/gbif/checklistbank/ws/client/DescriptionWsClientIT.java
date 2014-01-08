/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.vocabulary.Language;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DescriptionWsClientIT extends NameUsageComponentWsClientITBase<DescriptionService> {

  private static final int USAGE_ID = 100000040;

  public DescriptionWsClientIT() {
    super(DescriptionService.class, USAGE_ID, 3);
  }

  @Test
  public void testListByUsageDetailed() {
    PagingResponse<Description> descriptionsResp = wsClient.listByUsage(USAGE_ID, new PagingRequest(0, 20));
    List<Description> descriptions = descriptionsResp.getResults();

    assertEquals(3, descriptions.size());
    assertEquals((Integer) 25, descriptions.get(0).getKey());
    assertEquals((Integer) 26, descriptions.get(1).getKey());
    assertEquals((Integer) 27, descriptions.get(2).getKey());

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    Description d1 = wsClient.listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    Description d2 = wsClient.listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, descriptions.get(0));
    assertEquals(d2, descriptions.get(1));
  }

  @Test
  public void testGet() {
    final Integer KEY = 14;
    Description d = wsClient.get(KEY);
    assertEquals(KEY, d.getKey());
    assertEquals("introduction", d.getType());
    assertEquals(Language.ENGLISH, d.getLanguage());

    assertNull(wsClient.get(-2));
  }

}
