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
package org.gbif.checklistbank.search.ws.guice;

import org.gbif.checklistbank.search.inject.SearchModule;
import org.gbif.ws.server.guice.GbifServletListener;

import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.inject.Module;

public class ChecklistBankSearchWsModule extends GbifServletListener {

  public ChecklistBankSearchWsModule() {
    super("checklistbank.properties", "org.gbif.checklistbank.search.ws", false);
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    List<Module> modules = Lists.newArrayList();
    modules.add(new SearchModule(properties, true));
    return modules;
  }

}
