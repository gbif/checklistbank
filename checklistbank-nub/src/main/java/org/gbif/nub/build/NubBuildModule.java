package org.gbif.nub.build;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;
import org.gbif.nameparser.NameParser;
import org.gbif.nub.lookup.HigherTaxaLookup;
import org.gbif.registry.ws.client.guice.RegistryWsClientModule;
import org.gbif.ws.client.guice.AnonymousAuthModule;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module setting up all dependencies to expose the NubGenerate.
 */
public class NubBuildModule extends PrivateModule {
  private static final Logger LOG = LoggerFactory.getLogger(NubBuildModule.class);
  private Properties properties;

  public NubBuildModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    install(new ChecklistBankServiceMyBatisModule(properties));
    install(new ChecklistBankWsClientModule(properties, false, false, true));
    install(new RegistryWsClientModule(properties));
    // required by registry client
    install(new AnonymousAuthModule());
    bind(NubGenerator.class).in(Scopes.NO_SCOPE);
    expose(NubGenerator.class);
  }

  @Provides
  @Singleton
  public static HigherTaxaLookup provideSynonyms() {
    HigherTaxaLookup synonyms = new HigherTaxaLookup();
    LOG.info("Start loading synonym dictionaries from rs.gbif.org ...");
    synonyms.loadOnlineDicts();
    LOG.info("Online synonym dictionaries loaded");
    return synonyms;
  }

  @Provides
  @Singleton
  public static NameParser provideParser() {
    NameParser parser = new NameParser();
    return parser;
  }

  @Provides
  public static File dataDir() throws IOException {
    //TODO: make this configurable
    File dir = new File("/tmp/nub-data");
    if (dir.exists()) {
      LOG.warn("Nub writer dir exists. All data will be removed");
      FileUtils.deleteDirectory(dir);
    }
    // create new, empty dir
    FileUtils.forceMkdir(dir);
    LOG.info("New nub data directory created at {}", dir.getAbsoluteFile());
    return dir;
  }


}
