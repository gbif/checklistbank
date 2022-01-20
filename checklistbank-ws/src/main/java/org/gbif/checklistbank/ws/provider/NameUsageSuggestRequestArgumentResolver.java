package org.gbif.checklistbank.ws.provider;

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.ws.server.provider.SearchRequestProvider;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class NameUsageSuggestRequestArgumentResolver
    extends SearchRequestProvider<NameUsageSuggestRequest, NameUsageSearchParameter>
    implements HandlerMethodArgumentResolver {

  public NameUsageSuggestRequestArgumentResolver() {
    super(NameUsageSuggestRequest.class, NameUsageSearchParameter.class);
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return NameUsageSuggestRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory)
      throws Exception {
    return this.getValue(webRequest);
  }
}
