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

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class SolrMappingTest {

  @Test
  public void testMappingCompleteness() {
    for (NameUsageSearchRequest.QueryField hlField : NameUsageSearchRequest.QueryField.values()) {
      if (hlField == NameUsageSearchRequest.QueryField.SCIENTIFIC) continue;
      assertNotNull("HighlightField mapping for "+hlField+" missing", SolrMapping.HIGHLIGHT_FIELDS.get(hlField));
    }

    for (NameUsageSearchParameter p : NameUsageSearchParameter.values()) {
      assertNotNull("NameUsageSearchParameter mapping for "+p+" missing", SolrMapping.FACET_MAPPING.get(p));
    }
  }

}