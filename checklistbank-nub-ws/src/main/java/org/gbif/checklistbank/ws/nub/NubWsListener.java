package org.gbif.checklistbank.ws.nub;


import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.nub.lookup.NubMatchingModule;
import org.gbif.ws.server.guice.GbifServletListener;

import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.inject.Module;

public class NubWsListener extends GbifServletListener {

  public NubWsListener() {
    super("checklistbank.properties", NubResource.class.getPackage().getName(), false);
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    List<Module> modules = Lists.newArrayList();
    modules.add(new ChecklistBankServiceMyBatisModule(properties));
    modules.add(new NubMatchingModule());
    // use the line below to run the webservice locally with the json test index data from the nub module
    // modules.add(new NubMatchingTestModule());
    return modules;
  }

}
