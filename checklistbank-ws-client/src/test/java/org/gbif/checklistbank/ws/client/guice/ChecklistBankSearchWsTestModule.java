package org.gbif.checklistbank.ws.client.guice;

import org.gbif.checklistbank.search.SearchTestModule;
import org.gbif.checklistbank.ws.client.NameUsageSearchWsClientIT;
import org.gbif.checklistbank.ws.guice.ChecklistBankWsModule;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.google.inject.Module;

public class ChecklistBankSearchWsTestModule extends ChecklistBankWsModule {

  public ChecklistBankSearchWsTestModule() throws IOException {
    //do nothing
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    List<Module> modules = super.getModules(properties);
    // very cowboy way of getting the solr index build first and "inject" the server here
    // replace regular search module with the test one using the embedded solr server
    modules.remove(1);
    modules.add(new SearchTestModule(properties, NameUsageSearchWsClientIT.setup().getSolr()));
    return modules;
  }
}
