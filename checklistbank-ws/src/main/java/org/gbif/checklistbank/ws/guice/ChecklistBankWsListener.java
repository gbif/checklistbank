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
package org.gbif.checklistbank.ws.guice;


import org.gbif.checklistbank.config.MetricModule;
import org.gbif.checklistbank.index.guice.SearchModule;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.ws.resources.SpeciesResource;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.app.ConfUtils;
import org.gbif.ws.server.guice.GbifServletListener;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Module;

public class ChecklistBankWsListener extends GbifServletListener {

  public static final String APP_CONF_FILE = "checklistbank.properties";

  public ChecklistBankWsListener() throws IOException {
    this(PropertiesUtil.readFromFile(ConfUtils.getAppConfFile(APP_CONF_FILE)));
  }

  public ChecklistBankWsListener(Properties properties) {
    super(properties, SpeciesResource.class.getPackage().getName(), false);
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    List<Module> modules = Lists.newArrayList();
    modules.add(new MetricModule(properties));
    modules.add(new ChecklistBankServiceMyBatisModule(properties));
    modules.add(new SearchModule(properties, true));
    modules.add(new ChecklistBankWsModule(properties));
    return modules;
  }

  @Override
  /**
   * Only overriden to be able to test it in ChecklistBankWsModuleTest, doesnt do anything new.
   */
  protected final Injector getInjector() {
    return super.getInjector();
  }

}
