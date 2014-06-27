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

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NameUsageWsClientIT extends ClientMyBatisITBase<NameUsageService> {

  private final int NOT_FOUND_KEY = -10;

  public NameUsageWsClientIT() {
    super(NameUsageService.class);
  }

  @Test
  public void testGet() {
    NameUsage nameUsage = wsClient.get(1, null);
    assertNotNull(nameUsage);
    assertEquals("Animalia", nameUsage.getScientificName());
  }

  @Test
  public void testGetNotFound() {
    // null expected, 404 intercepted and converted to null thanks to HttpErrorResponseInterceptor
    assertNull(wsClient.get(NOT_FOUND_KEY, null));
  }

  /**
   * Testing the locale dependent vernacular name string property.
   */
  @Test
  public void testGetWithLocale() {
    int key = 100000025;
    NameUsage nameUsage = wsClient.get(key, Locale.ENGLISH);
    assertEquals("Eurasian Red Squirrel", nameUsage.getVernacularName());

    nameUsage = wsClient.get(key, Locale.GERMAN);
    assertEquals("Europäisches Eichhörnchen", nameUsage.getVernacularName());
  }

  @Test
  public void testGetWithLocaleNotFound() {
    assertNull(wsClient.get(NOT_FOUND_KEY, Locale.ENGLISH));
  }

  @Test
  public void testListChildren() {
    PagingResponse<NameUsage> children = wsClient.listChildren(100000005, Locale.ENGLISH, new PagingRequest(0, 20));
    assertNotNull(children);
    assertEquals(2, children.getResults().size());
  }

  @Test
  public void testListChildrenNotFound() {
    PagingResponse<NameUsage> res = wsClient.listChildren(NOT_FOUND_KEY, Locale.ENGLISH, new PagingRequest());
    NameUsageComponentWsClientITBase.assertEmptyResponse(res);
  }

  @Test
  public void testListParents() {
    List<NameUsage> parents = wsClient.listParents(100000009, Locale.ENGLISH);
    assertNotNull(parents);
    assertEquals(9, parents.size());
  }

  @Test
  public void testListParentsNotFound() {
    List<NameUsage> res = wsClient.listParents(NOT_FOUND_KEY, Locale.ENGLISH);
    assertNotNull(res);
    assertTrue(res.isEmpty());
  }

  @Test
  public void testListRelated() {
    List<NameUsage> related = wsClient.listRelated(10, Locale.ENGLISH);
    assertNotNull(related);
    assertEquals(1, related.size());

    related = ddt.getService().listRelated(1, Locale.ENGLISH, UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"));
    assertEquals(1, related.size());

    related = ddt.getService().listRelated(1, Locale.ENGLISH, UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088ff"));
    assertEquals(0, related.size());

  }

  @Test
  public void testListRelatedNotFound() {
    List<NameUsage> res = wsClient.listRelated(NOT_FOUND_KEY, Locale.ENGLISH);
    assertNotNull(res);
    assertTrue(res.isEmpty());
  }

  @Test
  public void testListSynonyms() {
    PagingResponse<NameUsage> synonyms = wsClient.listSynonyms(100000011, Locale.ENGLISH, new PagingRequest(0, 20));
    assertNotNull(synonyms);
    assertEquals(12, synonyms.getResults().size());
  }

  @Test
  public void testListSynonymsNotFound() {
    PagingResponse<NameUsage> synonyms = wsClient.listSynonyms(NOT_FOUND_KEY, Locale.ENGLISH, new PagingRequest(0, 20));
    NameUsageComponentWsClientITBase.assertEmptyResponse(synonyms);
  }

  @Test
  public void testGetMybatisStyle() {
    final NameUsage rodentia = wsClient.get(100000004, Locale.UK);
    final NameUsageMetrics rodentiaM = wsClient.getMetrics(100000004);
    assertNotNull(rodentia);
    assertEquals((Integer) 10, rodentia.getNubKey());
    assertFalse(rodentia.isSynonym());
    assertEquals("1000", rodentia.getTaxonID());
    assertEquals("Rodentia", rodentia.getCanonicalName());
    assertEquals("Rodentia Bowdich, 1821", rodentia.getScientificName());
    assertEquals("Bowdich, 1821", rodentia.getAuthorship());
    assertEquals(Rank.ORDER, rodentia.getRank());
    assertEquals((Integer) 100000003, rodentia.getParentKey());

    assertEquals("Animalia", rodentia.getKingdom());
    assertEquals((Integer) 100000001, rodentia.getKingdomKey());

    assertEquals("Chordata", rodentia.getPhylum());
    assertEquals((Integer) 100000002, rodentia.getPhylumKey());
    assertEquals(0, rodentiaM.getNumPhylum());

    assertEquals("Mammalia", rodentia.getClazz());
    assertEquals((Integer) 100000003, rodentia.getClassKey());
    assertEquals(0, rodentiaM.getNumClass());

    assertEquals(1, rodentiaM.getNumOrder());
    assertEquals(1, rodentiaM.getNumFamily());
    assertEquals(2, rodentiaM.getNumGenus());
    assertEquals(3, rodentiaM.getNumSpecies());
    assertEquals(8, rodentiaM.getNumSynonyms());
    assertEquals(1, rodentiaM.getNumChildren());
    assertEquals(24, rodentia.getNumDescendants());

    assertEquals(Origin.SOURCE, rodentia.getOrigin());

    assertEquals(UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"), rodentia.getDatasetKey());
    assertNull(rodentia.getPublishedIn());

    assertEquals("1000", rodentia.getTaxonID());


    NameUsage squirrel = wsClient.get(100000025, Locale.UK);
    final NameUsageMetrics squirrelM = wsClient.getMetrics(100000025);
    assertNotNull(squirrel);
    assertNull(squirrel.getNubKey());
    assertFalse(squirrel.isSynonym());
    assertEquals("Sciurus vulgaris", squirrel.getCanonicalName());
    assertEquals("Sciurus vulgaris Linnaeus, 1758", squirrel.getScientificName());
    assertEquals("Linnaeus, 1758", squirrel.getAuthorship());
    assertEquals("Eurasian Red Squirrel", squirrel.getVernacularName());
    assertEquals(Rank.SPECIES, squirrel.getRank());
    assertEquals((Integer) 100000024, squirrel.getParentKey());

    assertEquals("Animalia", squirrel.getKingdom());
    assertEquals((Integer) 100000001, squirrel.getKingdomKey());

    assertEquals("Chordata", squirrel.getPhylum());
    assertEquals((Integer) 100000002, squirrel.getPhylumKey());
    assertEquals(0, squirrelM.getNumPhylum());

    assertEquals("Mammalia", squirrel.getClazz());
    assertEquals((Integer) 100000003, squirrel.getClassKey());
    assertEquals(0, squirrelM.getNumClass());

    assertEquals("Rodentia", squirrel.getOrder());
    assertEquals((Integer) 100000004, squirrel.getOrderKey());
    assertEquals(0, squirrelM.getNumOrder());

    assertEquals("Sciuridae", squirrel.getFamily());
    assertEquals((Integer) 100000005, squirrel.getFamilyKey());
    assertEquals(0, squirrelM.getNumFamily());

    assertEquals("Sciurus", squirrel.getGenus());
    assertEquals((Integer) 100000011, squirrel.getGenusKey());
    assertEquals(0, squirrelM.getNumGenus());

    assertEquals(1, squirrelM.getNumSpecies());
    assertEquals(9, squirrelM.getNumChildren());
    assertEquals(4, squirrelM.getNumSynonyms());

    assertEquals(UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"), squirrel.getDatasetKey());
    assertEquals("Syst. Nat. , 10th ed. vol. 1 p. 63", squirrel.getPublishedIn());

    assertEquals("2010030", squirrel.getTaxonID());


    // TEST VERNACULAR
    squirrel = wsClient.get(100000040, null);
    assertNull(squirrel.getVernacularName());

    squirrel = wsClient.get(100000040, Locale.GERMANY);
    assertEquals("Kaukasischen Eichhörnchen", squirrel.getVernacularName());

    // TEST non existing language VERNACULAR
    squirrel = wsClient.get(100000040, Locale.CHINESE);
    assertEquals("Caucasian Squirrel", squirrel.getVernacularName());

    // TEST MULTIPLE IDENTIFIERS
    squirrel = wsClient.get(100000007, Locale.GERMANY);
    assertEquals("6905528", squirrel.getTaxonID());
    assertEquals(URI.create("http://www.catalogueoflife.org/details/species/id/6905528"), squirrel.getReferences());

    // TEST SYNONYM
    NameUsage syn = wsClient.get(100000027, Locale.FRENCH);
    assertNotNull(syn);
    assertTrue(syn.isSynonym());
    assertEquals("Sciurus nadymensis", syn.getCanonicalName());
    assertEquals("Sciurus nadymensis Serebrennikov, 1928", syn.getScientificName());
    assertNull(syn.getVernacularName());
    assertEquals(Rank.SPECIES, syn.getRank());
    assertEquals((Integer) 100000024, syn.getParentKey());
    assertEquals((Integer) 100000025, syn.getAcceptedKey());
    assertEquals("Sciurus vulgaris Linnaeus, 1758", syn.getAccepted());
    assertFalse(syn.isProParte());
  }
}
