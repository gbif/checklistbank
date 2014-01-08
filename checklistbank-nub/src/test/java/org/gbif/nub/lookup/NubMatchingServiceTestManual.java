package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubMatchingServiceTestManual {

  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingServiceTestManual.class);

  private final NameUsageMatchingService matcher;

  public NubMatchingServiceTestManual() throws IOException {

    LOG.info("Load clb properties");
    Properties properties = PropertiesUtil.loadProperties("checklistbank.properties");

    LOG.info("Create guice injector");
    Injector inj = Guice.createInjector(new ChecklistBankServiceMyBatisModule(properties), new NubMatchingModule());

    LOG.info("Create matching service");
    matcher = inj.getInstance(NameUsageMatchingService.class);

    LOG.info("Nub Matching setup complete");
  }

  public void testMatching() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    // test identical
    matcher.match("Animalia", null, cl, true, true);
    matcher.match("Animals", null, cl, true, true);
    matcher.match("Insects", null, cl, true, true);
    cl.setKingdom("Animalia");
    matcher.match("Puma concolor", null, cl, true, true);
    cl.setKingdom("Plantae");
    matcher.match("Puma concolor", null, cl, true, true);
  }


  public static void main (String[] args) throws IOException {
    NubMatchingServiceTestManual test = new NubMatchingServiceTestManual();
    test.testMatching();
  }

}
