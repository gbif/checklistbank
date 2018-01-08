/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.checklistbank.ws.nub;

import com.google.inject.Singleton;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import org.gbif.api.model.common.LinneanClassification;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Type;

/**
 * Jersey provider class that extracts LinneanClassification instances from the query parameters.
 */
@Provider
@Singleton
public class ClassificationProvider extends AbstractHttpContextInjectable<LinneanClassification>
    implements InjectableProvider<Context, Type> {

  @Override
  public Injectable<LinneanClassification> getInjectable(ComponentContext ic, Context a, Type c) {
    if (c.equals(LinneanClassification.class)) {
      return this;
    }
    return null;
  }

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public LinneanClassification getValue(HttpContext c) {
    return buildClassification(c);
  }

  public static LinneanClassification buildClassification(HttpContext c) {
    MultivaluedMap<String, String> params = c.getRequest().getQueryParameters();

    Classification cl = new Classification();
    cl.setKingdom(params.getFirst("kingdom"));
    cl.setPhylum(params.getFirst("phylum"));
    cl.setClazz(params.getFirst("class"));
    cl.setOrder(params.getFirst("order"));
    cl.setFamily(params.getFirst("family"));
    cl.setGenus(params.getFirst("genus"));
    cl.setSubgenus(params.getFirst("subgenus"));

    return cl;
  }
}
