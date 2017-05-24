package org.gbif.checklistbank.ws.nub;


import com.google.common.collect.Lists;
import com.google.inject.Module;
import org.gbif.api.model.Constants;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.utils.CloseableUtils;
import org.gbif.nub.lookup.NubMatchingModule;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.app.ConfUtils;
import org.gbif.ws.server.guice.GbifServletListener;

import javax.servlet.ServletContextEvent;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class NubWsListener extends GbifServletListener {
  private static final String APP_CONF_FILE = "checklistbank.properties";
  private static final String INDEX_DIR = "checklistbank.nub.indexDir";
  private static final String INCL_DELETED = "checklistbank.nub.inclDeleted";
  private static final String NUB_DATASET_KEY = "checklistbank.nub.datasetKey";
  private List<Closeable> toBeClosed = Lists.newArrayList();

  public NubWsListener() throws IOException {
    super(PropertiesUtil.readFromFile(ConfUtils.getAppConfFile(APP_CONF_FILE)), NubResource.class.getPackage().getName(), false);
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    List<Module> modules = Lists.newArrayList();

    ChecklistBankServiceMyBatisModule clbMod = new ChecklistBankServiceMyBatisModule(properties);
    modules.add(clbMod);
    toBeClosed.add(clbMod);

    boolean incDeleted = PropertiesUtil.propertyAsBool(properties, INCL_DELETED, false);
    UUID datasetKey = UUID.fromString(properties.getProperty(NUB_DATASET_KEY, Constants.NUB_DATASET_KEY.toString()));
    NubMatchingModule nubMod = new NubMatchingModule(new File(properties.getProperty(INDEX_DIR)), incDeleted, datasetKey);
    modules.add(nubMod);
    toBeClosed.add(nubMod);
    // use the line below to run the webservice locally with the json test index data from the nub module
    // modules.add(new NubMatchingTestModule());
    return modules;
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    super.contextDestroyed(servletContextEvent);
    // close modules
    CloseableUtils.close(toBeClosed);
  }
}
