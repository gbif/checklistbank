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
      assertNotNull("HighlightField mapping for "+hlField+" missing", SolrMapping.HIGHLIGHT_FIELDS.get(hlField));
    }

    for (NameUsageSearchParameter p : NameUsageSearchParameter.values()) {
      assertNotNull("NameUsageSearchParameter mapping for "+p+" missing", SolrMapping.FACET_MAPPING.get(p));
    }
  }

}