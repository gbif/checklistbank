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

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.ws.server.provider.FacetedSearchRequestProvider;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class NameUsageSearchRequestHandlerMethodArgumentResolver
    extends FacetedSearchRequestProvider<NameUsageSearchRequest, NameUsageSearchParameter>
    implements HandlerMethodArgumentResolver {

  public NameUsageSearchRequestHandlerMethodArgumentResolver() {
    super(NameUsageSearchRequest.class, NameUsageSearchParameter.class);
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return NameUsageSearchRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    return getValue(webRequest);
  }

  @Override
  protected NameUsageSearchRequest getSearchRequest(
      WebRequest webRequest, NameUsageSearchRequest searchRequest) {
    return super.getSearchRequest(webRequest, searchRequest);
  }
}
