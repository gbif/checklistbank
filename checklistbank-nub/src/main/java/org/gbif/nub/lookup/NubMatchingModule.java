package org.gbif.nub.lookup;

import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.nameparser.NameParser;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module setting up all dependencies to expose the NubMatching service.
 */
public class NubMatchingModule extends PrivateModule {
  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingModule.class);

  @Override
  protected void configure() {

    bind(NameUsageMatchingService.class).to(NubMatchingServiceImpl.class).asEagerSingleton();
    expose(NameUsageMatchingService.class);
  }

  @Provides
  @Inject
  @Singleton
  public NubIndex provideIndex(UsageService usageService) throws IOException {
    LOG.info("Start loading nub usages into lucene index ...");
    NubIndex index = NubIndex.newNubIndex(usageService);
    LOG.info("Lucene index initialized");
    return index;
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


}
