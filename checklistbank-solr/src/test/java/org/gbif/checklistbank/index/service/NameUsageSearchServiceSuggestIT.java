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
package org.gbif.checklistbank.index.service;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.index.BaseIT;
import org.gbif.checklistbank.test.extensions.SolrDbLoadBeforeAll;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests using an embedded solr server with the mybatis squirrels test dataset.
 * The solr index will be rebuild before the test using the NameUsageIndexerBaseIT base class.
 */
@Disabled
@ExtendWith(SolrDbLoadBeforeAll.class)
public class NameUsageSearchServiceSuggestIT extends BaseIT  {

  private final NameUsageSearchService searchService;

  @Autowired
  public NameUsageSearchServiceSuggestIT(NameUsageSearchService searchService) {
    this.searchService = searchService;
  }

  @Test
  public void testSuggest() {
    List<NameUsageSuggestResult> results = searchSuggest("Puma");
    assertTrue(results.size() > 0,  "Results expected");
    // highest rank first
    assertEquals("Puma Jardine, 1834", results.get(0).getScientificName());

    // https://github.com/gbif/checklistbank/issues/11
    assertPumaConcolor("Puma concolor");
    assertPumaConcolor("Puma concolo");
    assertPumaConcolor("Puma concol");
    assertPumaConcolor("Puma conco");
    assertPumaConcolor("Puma conc");
    assertPumaConcolor("Puma con");
    assertPumaConcolor("Puma co");
    assertPumaConcolor("Puma c");

    assertPumaConcolor("concolor");
  }

  private void assertPumaConcolor(String q) {
    List<NameUsageSuggestResult> results = searchSuggest(q);
    assertTrue(results.size() > 0,  "Results expected");
    assertEquals(2435099, (int) results.get(0).getKey());
    assertEquals("Puma concolor (Linnaeus, 1771)", results.get(0).getScientificName());

  }

  @Test
  public void testSuggestStraight() {
    assertSuggestNone("Bakt");
    assertSuggestNone("Bacterii");
    assertSuggestNone("okenil");
    assertSuggestNone("Bacterion");
    assertSuggestNone("Bacterius");

    assertSuggest("okeni", 2500009, Rank.SPECIES, "Bacterium okeniilium");
    assertSuggest("Bacterium ok", 2500009, Rank.SPECIES, "Bacterium okeniilium");

    List<NameUsageSuggestResult> bacteria = assertSuggest("Bacteria", 3, Rank.KINGDOM, "Bacteria");
    assertEquals(2500002, (int) bacteria.get(1).getKey());
    assertEquals(2500001, (int) bacteria.get(2).getKey());
    System.out.println(bacteria.get(3).getScientificName());
    assertEquals(Rank.SPECIES, bacteria.get(3).getRank());

    assertSuggest("Bacterium", 2500003, Rank.GENUS, "Bacterium");

    List<NameUsageSuggestResult> aborigena = assertSuggest("aborig", 2500005, Rank.SPECIES, "Bacteria aborigena Giglio-Tos, 1910");
    // does not match the author as partial matches are not allowed
    assertEquals(1, aborigena.size());

    assertSuggest("aborigena", 2500005, Rank.SPECIES, "Bacteria aborigena Giglio-Tos, 1910");

    aborigena = assertSuggest("Aborigena", 2500005, Rank.SPECIES, "Bacteria aborigena Giglio-Tos, 1910");
    assertEquals(2500011, (int) aborigena.get(1).getKey());
    assertEquals("Bacteria marca Aborigena, 1910", aborigena.get(1).getScientificName());
  }

  private List<NameUsageSuggestResult> assertSuggest(String q, int key, Rank rank, String sciname) {
    List<NameUsageSuggestResult> results = searchSuggest(q);
    assertEquals(key, (int) results.get(0).getKey());
    assertEquals(sciname, results.get(0).getScientificName());
    assertEquals(rank, results.get(0).getRank());
    return results;
  }

  private void assertSuggestNone(String q) {
    List<NameUsageSuggestResult> results = searchSuggest(q);
    assertTrue(results.isEmpty());
  }

  /**
   * Utility method for testing suggest service.
   */
  private List<NameUsageSuggestResult> searchSuggest(String q) {
    NameUsageSuggestRequest req = new NameUsageSuggestRequest();
    req.setQ(q);
    req.setLimit(50);
    req.addParameter(NameUsageSearchParameter.DATASET_KEY, Constants.NUB_DATASET_KEY.toString());
    return searchService.suggest(req);
  }
}
