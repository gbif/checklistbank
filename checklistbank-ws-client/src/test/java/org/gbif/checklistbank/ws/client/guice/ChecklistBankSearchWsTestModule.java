package org.gbif.checklistbank.ws.client.guice;

import com.google.common.collect.Lists;
import com.google.inject.Module;
import org.gbif.checklistbank.search.SearchTestModule;
import org.gbif.checklistbank.search.ws.guice.ChecklistBankSearchWsModule;
import org.gbif.checklistbank.ws.client.NameUsageSearchWsClientIT;

import java.util.List;
import java.util.Properties;

public class ChecklistBankSearchWsTestModule extends ChecklistBankSearchWsModule {

  @Override
  protected List<Module> getModules(Properties properties) {
    List<Module> modules = Lists.newArrayList();
    // very cowboy way of getting the solr index build first and "inject" the server here
    modules.add(new SearchTestModule(properties, NameUsageSearchWsClientIT.setup().getSolr()));
    return modules;
  }
}
