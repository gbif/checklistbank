package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.nameparser.NameParser;
import org.gbif.utils.file.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module setting up all dependencies to expose the NubMatching service.
 */
public class NubMatchingTestModule extends PrivateModule {
  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingTestModule.class);

  @Override
  protected void configure() {

    bind(NameUsageMatchingService.class).to(NubMatchingServiceImpl.class).asEagerSingleton();
    expose(NameUsageMatchingService.class);
  }

  @Provides
  @Inject
  @Singleton
  public static NubIndex provideIndex() throws IOException {
    return NubIndexImmutable.newMemoryIndex(loadIndexJson());
  }

  @Provides
  @Singleton
  public static HigherTaxaComparator provideSynonyms() throws IOException {
    LOG.info("Loading synonym dictionaries from classpath ...");
    HigherTaxaComparator syn = new HigherTaxaComparator();
    syn.loadClasspathDicts("dicts");
    return syn;
  }

  @Provides
  @Singleton
  public NameParser provideParser() {
    NameParser parser = new NameParser();
    return parser;
  }

  /**
   * Load all nubXX.json files from the index resources into a distinct list of NameUsage instances.
   * The individual nubXX.json files are regular results of a NameUsageMatch and can be added to the folder
   * to be picked up here.
   */
  private static List<NameUsageMatch> loadIndexJson() {
    Map<Integer, NameUsageMatch> usages = Maps.newHashMap();

    InputStreamUtils isu = new InputStreamUtils();
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);

    int id = 0;
    while (id < 250) {
      String file = "index/nub"+id+".json";
      InputStream json = isu.classpathStream(file);
      if (json != null) {
        try {
          int before = usages.size();
          NameUsageMatch m = mapper.readValue(json, NameUsageMatch.class);
          for (NameUsageMatch u : extractUsages(m)) {
            if (u != null) {
              usages.put(u.getUsageKey(), u);
            }
          }
          System.out.println("Loaded " + (usages.size() - before) + " new usage(s) from " + file);
        } catch (IOException e) {
          Assert.fail("Failed to read " + file + ": " + e.getMessage());
        }
      }
      id++;
    }
    return Lists.newArrayList(usages.values());
  }

  private static List<NameUsageMatch> extractUsages(NameUsageMatch m) {
    List<NameUsageMatch> usages = Lists.newArrayList();
    usages.add(m);
    if (m.getAlternatives() != null) {
      usages.addAll(m.getAlternatives());
    }
    return usages;
  }

}
