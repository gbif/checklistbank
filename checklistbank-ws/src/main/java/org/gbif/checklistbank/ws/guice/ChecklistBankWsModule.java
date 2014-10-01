package org.gbif.checklistbank.ws.guice;


import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.ws.resources.SpeciesResource;
import org.gbif.ws.server.guice.GbifServletListener;
import org.gbif.checklistbank.search.inject.SearchModule;

import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Module;

public class ChecklistBankWsModule extends GbifServletListener {

  public ChecklistBankWsModule() {
    super("checklistbank.properties", SpeciesResource.class.getPackage().getName(), false);
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    List<Module> modules = Lists.newArrayList();
    modules.add(new ChecklistBankServiceMyBatisModule(properties));
    modules.add(new SearchModule(properties, true));
    modules.add(new NameParserModule());
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
