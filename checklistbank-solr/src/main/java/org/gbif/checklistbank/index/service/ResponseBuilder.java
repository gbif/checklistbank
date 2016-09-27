package org.gbif.checklistbank.index.service;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.checklistbank.index.NameUsageDocConverter;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import static org.gbif.common.search.util.SolrConstants.HL_POST;
import static org.gbif.common.search.util.SolrConstants.HL_PRE;
import static org.gbif.common.search.util.SolrConstants.HL_PRE_REGEX;

/**
 *
 */
public class ResponseBuilder {

  private final NameUsageDocConverter converter = new NameUsageDocConverter();

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> buildSearch(NameUsageSearchRequest searchRequest, QueryResponse response) {
    // Create response
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> resp = new SearchResponse<NameUsageSearchResult, NameUsageSearchParameter>(searchRequest);
    resp.setCount(response.getResults().getNumFound());
    resp.setLimit(response.getResults().size());

    // Main result documents
    SolrDocumentList docs = response.getResults();
    for (SolrDocument doc : docs) {
      resp.getResults().add(converter.toSearchUsage(doc, searchRequest.isExtended()));
    }

    // add facets
    setFacets(resp, response);

    // add highlighting
    setHighlighting(resp, response, searchRequest);
    return resp;
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public List<NameUsageSuggestResult> buildSuggest(QueryResponse response) {
    List<NameUsageSuggestResult> result = Lists.newArrayList();
    // Main result documents
    SolrDocumentList docs = response.getResults();
    for (SolrDocument doc : docs) {
      result.add(converter.toSuggestUsage(doc));
    }
    return result;
  }

  /**
   * Helper method that takes Solr response and extracts the facets results.
   * The facets are converted to a list of Facets understood by the search API.
   * The result of this method can be a empty list.
   *
   * @param queryResponse that contains the facets information returned by Solr
   * @return the List of facets retrieved from the Solr response
   */
  private void setFacets(SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response, final QueryResponse queryResponse) {
    List<Facet<NameUsageSearchParameter>> facets = Lists.newArrayList();
    if (queryResponse.getFacetFields() != null) {
      List<FacetField> facetFields =  queryResponse.getFacetFields();
      for (final FacetField facetField : facetFields) {
        NameUsageSearchParameter facetParam = SolrMapping.FACET_MAPPING.inverse().get(facetField.getName());
        Facet<NameUsageSearchParameter> facet = new Facet<NameUsageSearchParameter>(facetParam);

        List<Facet.Count> counts = Lists.newArrayList();
        if (facetField.getValues() != null) {
          for (final FacetField.Count count : facetField.getValues()) {
            String value = SolrMapping.interpretSolrValue(facetParam, count.getName());
            counts.add(new Facet.Count(value, count.getCount()));
          }
        }
        facet.setCounts(counts);
        facets.add(facet);
      }
    }
    response.setFacets(facets);
  }

  /**
   * Takes the highlighted fields form solrResponse and copies them to the response object.
   * @param response to set the highlighted fields.
   * @param solrResponse to extract the highlighting information
   * @param request the search request
   */
  private void setHighlighting(SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response, final QueryResponse solrResponse, NameUsageSearchRequest request) {
    if ((solrResponse.getHighlighting() != null) && !solrResponse.getHighlighting().isEmpty()) {
      for (String docId : solrResponse.getHighlighting().keySet()) {
        NameUsageSearchResult bean = getByKey(response, docId);
        if (bean != null) {
          Map<String, List<String>> docHighlights = solrResponse.getHighlighting().get(docId);
          for (NameUsageSearchRequest.QueryField hlField : request.getHighlightFields()) {
            if (docHighlights.containsKey(SolrMapping.HIGHLIGHT_FIELDS.get(hlField))) {
              for (String hlSnippet : docHighlights.get(SolrMapping.HIGHLIGHT_FIELDS.get(hlField))) {
                if (request.isExtended()) {
                  // merge highlighting into existing results
                  switch (hlField) {
                    case DESCRIPTION:
                      for (Description d : bean.getDescriptions()) {
                        d.setDescription(mergeHl(d.getDescription(), hlSnippet));
                      }
                      break;

                    case VERNACULAR:
                      for (VernacularName v : bean.getVernacularNames()) {
                        v.setVernacularName(mergeHl(v.getVernacularName(), hlSnippet));
                      }
                      break;
                  }

                } else {
                  // just add highlighted results
                  switch (hlField) {
                    case DESCRIPTION:
                      Description d = new Description();
                      d.setDescription(hlSnippet);
                      bean.getDescriptions().add(d);
                      break;

                    case VERNACULAR:
                      VernacularName v = new VernacularName();
                      v.setVernacularName(hlSnippet);
                      bean.getVernacularNames().add(v);
                      break;
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  @VisibleForTesting
  protected static String mergeHl(String original, String hlSnippet) {
    // Cleans the hl markers
    String hlCleaned = cleanHighlightingMarks(hlSnippet);
    // replace snippet in original
    return original.replace(hlCleaned, hlSnippet);
  }


  /**
   * Cleans all occurrences of highlighted tags/marks in the parameter and returns an new instance clean of those
   * marks.
   */
  private static String cleanHighlightingMarks(final String hlText) {
    String hlLiteral = hlText;
    int indexPre = hlLiteral.indexOf(HL_PRE);
    while (indexPre > -1) {
      int indexPost = hlLiteral.indexOf(HL_POST, indexPre + HL_PRE.length());
      if (indexPost > -1) {
        String post = hlLiteral.substring(indexPost + HL_POST.length());
        String pre = hlLiteral.substring(0, indexPost);
        Matcher preMatcher = HL_PRE_REGEX.matcher(pre);
        pre = preMatcher.replaceFirst("");
        hlLiteral = pre + post;
      }
      indexPre = hlLiteral.indexOf(HL_PRE);
    }
    return hlLiteral;
  }

  private NameUsageSearchResult getByKey(SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response, String key) {
    for (NameUsageSearchResult bean : response.getResults()) {
      if (bean.getKey().toString().equals(key)) {
        return bean;
      }
    }
    return null;
  }
}
