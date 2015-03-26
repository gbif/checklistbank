package org.gbif.checklistbank.ws.nub;


import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.nub.lookup.NubMatchingModule;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.app.ConfUtils;
import org.gbif.ws.server.guice.GbifServletListener;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.inject.Module;

public class NubWsListener extends GbifServletListener {

  private static final String APP_CONF_FILE = "checklistbank.properties";
  private static final String POOLSIZE = "checklistbank.db.maximumPoolSize";
  private static final String INDEX_DIR = "checklistbank.nub.indexDir";

  public NubWsListener() throws IOException {
    super(PropertiesUtil.readFromFile(ConfUtils.getAppConfFile(APP_CONF_FILE)), NubResource.class.getPackage().getName(), false);
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    List<Module> modules = Lists.newArrayList();
    modules.add(new ChecklistBankServiceMyBatisModule(properties));
    // we use half the db poolsize as threads to build a new index
    int poolsize = Integer.valueOf(properties.getProperty(POOLSIZE));
    modules.add(new NubMatchingModule(new File(properties.getProperty(INDEX_DIR)), poolsize/2));
    // use the line below to run the webservice locally with the json test index data from the nub module
    // modules.add(new NubMatchingTestModule());
    return modules;
  }
}
