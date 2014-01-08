package org.gbif.checklistbank.search;

import org.gbif.checklistbank.index.model.NameUsageSolrSearchResult;
import org.gbif.common.search.model.FacetField;
import org.gbif.common.search.model.FullTextSearchField;
import org.gbif.common.search.model.SearchMapping;

import java.lang.annotation.Annotation;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;


public class NameUsageMappingTest {

  @Test
  public void mappingTest() {
    for (Annotation annotation : NameUsageSolrSearchResult.class.getAnnotations()) {
      System.out.println(annotation.annotationType().getCanonicalName());
    }
    SearchMapping searchMapping = NameUsageSolrSearchResult.class.getAnnotation(SearchMapping.class);
    for (FacetField ff : searchMapping.facets()) {
      System.out.println(ff.name());
    }
    for (FullTextSearchField ftsf : searchMapping.fulltextFields()) {
      System.out.println(ftsf.field());
    }
    assertNotNull(searchMapping.facets());
    assertNotNull(searchMapping.fulltextFields());
  }

}
