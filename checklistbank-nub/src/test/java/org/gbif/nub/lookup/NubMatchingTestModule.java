package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsage;
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
    NubIndex index = new NubIndex();
    LOG.info("Start loading test nub usages into lucene index ...");

    for (NameUsage u : loadIndexJson()) {
      index.addNameUsage(u);
    }
    LOG.info("Lucene index initialized");
    return index;
  }

  @Provides
  @Singleton
  public static HigherTaxaLookup provideSynonyms() throws IOException {
    LOG.info("Loading synonym dictionaries from classpath ...");
    HigherTaxaLookup syn = new HigherTaxaLookup();
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
  private static List<NameUsage> loadIndexJson() {
    Map<Integer, NameUsage> usages = Maps.newHashMap();

    InputStreamUtils isu = new InputStreamUtils();
    ObjectMapper mapper = new ObjectMapper();

    int id = 0;
    while (id < 100) {
      String file = "index/nub"+id+".json";
      InputStream json = isu.classpathStream(file);
      if (json != null) {
        try {
          int before = usages.size();
          NameUsageMatch m = mapper.readValue(json, NameUsageMatch.class);
          for (NameUsage u : extractUsages(m)) {
            if (u != null) {
              usages.put(u.getKey(), u);
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

  private static List<NameUsage> extractUsages(NameUsageMatch m) {
    List<NameUsage> usages = Lists.newArrayList();
    usages.add(toUsage(m));
    if (m.getAlternatives() != null) {
      for (NameUsageMatch m2 : m.getAlternatives()) {
        usages.add(toUsage(m2));
      }
    }
    return usages;
  }

  private static NameUsage toUsage(NameUsageMatch m) {
    if (m.getMatchType() != NameUsageMatch.MatchType.NONE) {
      NameUsage u = new NameUsage();
      u.setKey(m.getUsageKey());
      u.setCanonicalName(m.getCanonicalName());
      u.setScientificName(m.getScientificName());
      u.setRank(m.getRank());
      u.setSynonym(m.isSynonym());

      u.setKingdom(m.getKingdom());
      u.setPhylum(m.getPhylum());
      u.setClazz(m.getClazz());
      u.setFamily(m.getFamily());
      u.setGenus(m.getGenus());
      u.setSubgenus(m.getSubgenus());
      u.setSpecies(m.getSpecies());

      u.setKingdomKey(m.getKingdomKey());
      u.setPhylumKey(m.getPhylumKey());
      u.setClassKey(m.getClassKey());
      u.setFamilyKey(m.getFamilyKey());
      u.setGenusKey(m.getGenusKey());
      u.setSubgenusKey(m.getSubgenusKey());
      u.setSpeciesKey(m.getSpeciesKey());
      return u;
    }
    return null;
  }

}
