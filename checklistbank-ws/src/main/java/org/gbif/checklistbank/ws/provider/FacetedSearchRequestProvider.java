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
package org.gbif.checklistbank.ws.provider;

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.WebRequest;

import static org.gbif.ws.util.CommonWsUtils.getFirst;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_LIMIT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MINCOUNT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_MULTISELECT;
import static org.gbif.ws.util.WebserviceParameter.PARAM_FACET_OFFSET;

/**
 * This class is a copy org.gbif.ws.server.provider.FacetedSearchRequestProvider, copied to avoid gbif-api collisions.
 */

public class FacetedSearchRequestProvider<
        RT extends FacetedSearchRequest<P>, P extends SearchParameter>
    extends SearchRequestProvider<RT, P> {

  private static final int DEFAULT_FACET_LIMIT = 10;

  public FacetedSearchRequestProvider(Class<RT> requestType, Class<P> searchParameterClass) {
    super(requestType, searchParameterClass);
  }

  public FacetedSearchRequestProvider(
      Class<RT> requestType, Class<P> searchParameterClass, Integer maxPageSize) {
    super(requestType, searchParameterClass, maxPageSize);
  }

  @Override
  protected RT getSearchRequest(WebRequest webRequest, RT searchRequest) {
    RT request = super.getSearchRequest(webRequest, searchRequest);

    final Map<String, String[]> params = webRequest.getParameterMap();

    final String facetMultiSelectValue = getFirstIgnoringCase(PARAM_FACET_MULTISELECT, params);
    if (facetMultiSelectValue != null) {
      searchRequest.setMultiSelectFacets(Boolean.parseBoolean(facetMultiSelectValue));
    }

    final String facetMinCountValue = getFirstIgnoringCase(PARAM_FACET_MINCOUNT, params);
    if (facetMinCountValue != null) {
      searchRequest.setFacetMinCount(Integer.parseInt(facetMinCountValue));
    }

    final String facetLimit = getFirstIgnoringCase(PARAM_FACET_LIMIT, params);
    if (facetLimit != null) {
      searchRequest.setFacetLimit(Integer.parseInt(facetLimit));
    }

    final String facetOffset = getFirstIgnoringCase(PARAM_FACET_OFFSET, params);
    if (facetOffset != null) {
      searchRequest.setFacetOffset(Integer.parseInt(facetOffset));
    }

    final List<String> facets =
        params.get(PARAM_FACET) != null
            ? Arrays.asList(params.get(PARAM_FACET))
            : Collections.emptyList();
    if (!facets.isEmpty()) {
      for (String f : facets) {
        P p = findSearchParam(f);
        if (p != null) {
          searchRequest.addFacets(p);
          String pFacetOffset = getFirstIgnoringCase(f + '.' + PARAM_FACET_OFFSET, params);
          String pFacetLimit = getFirstIgnoringCase(f + '.' + PARAM_FACET_LIMIT, params);
          if (pFacetLimit != null) {
            if (pFacetOffset != null) {
              searchRequest.addFacetPage(
                  p, Integer.parseInt(pFacetOffset), Integer.parseInt(pFacetLimit));
            } else {
              searchRequest.addFacetPage(p, 0, Integer.parseInt(pFacetLimit));
            }
          } else if (pFacetOffset != null) {
            searchRequest.addFacetPage(p, Integer.parseInt(pFacetOffset), DEFAULT_FACET_LIMIT);
          }
        }
      }
    }

    return request;
  }

  /**
   * Get the first parameter value, the parameter is searched in a case-insensitive manner.
   * First tries with the exact match, then the lowercase and finally the uppercase value of the parameter.
   */
  protected static String getFirstIgnoringCase(String parameter, Map<String, String[]> params) {
    String value = getFirst(params, parameter);
    if (StringUtils.isNotEmpty(value)) {
      return value;
    }
    value = getFirst(params, parameter.toLowerCase());
    if (StringUtils.isNotEmpty(value)) {
      return value;
    }
    value = getFirst(params, parameter.toUpperCase());
    if (StringUtils.isNotEmpty(value)) {
      return value;
    }
    return null;
  }
}
