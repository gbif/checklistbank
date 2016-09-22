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

public class ChecklistBankWsModule extends GbifServletListener {

  public static final String APP_CONF_FILE = "checklistbank.properties";

  public ChecklistBankWsModule() throws IOException {
    this(PropertiesUtil.readFromFile(ConfUtils.getAppConfFile(APP_CONF_FILE)));
  }

  public ChecklistBankWsModule(Properties properties) {
    super(properties, SpeciesResource.class.getPackage().getName(), false);
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    List<Module> modules = Lists.newArrayList();
    modules.add(new MetricModule(properties));
    modules.add(new ChecklistBankServiceMyBatisModule(properties));
    modules.add(new SearchModule(properties, true));
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
