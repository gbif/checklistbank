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

import org.gbif.api.model.checklistbank.Image;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.NameUsageComponentService;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NameUsageComponentWsClientITBase<T extends NameUsageComponentService> extends ClientMyBatisITBase<T> {

  private int usageKey;
  private int expectedResults;

  public NameUsageComponentWsClientITBase(Class<T> serviceClass, int usageKey, int expectedResults) {
    super(serviceClass);
    this.usageKey = usageKey;
    this.expectedResults = expectedResults;
  }

  @Test
  public void testListByUsage() {
    PagingResponse<Image> images = wsClient.listByUsage(usageKey, new PagingRequest(0, 25));
    assertNotNull(images);
    assertEquals(expectedResults, images.getResults().size());
  }

  @Test
  public void testNull() {
    assertNull(wsClient.get(-10));
  }

  @Test
  public void testNotFound() {
    PagingResponse<T> res = wsClient.listByUsage(-10, new PagingRequest());
    assertEmptyResponse(res);
  }

  static void assertEmptyResponse(PagingResponse<?> res){
    assertNotNull(res);
    assertTrue(res.getResults().isEmpty());
    assertTrue(res.isEndOfRecords());
  }

}
