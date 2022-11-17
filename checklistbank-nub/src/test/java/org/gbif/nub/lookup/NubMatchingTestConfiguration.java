/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nub.lookup;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.checklistbank.utils.NameParsers;
import org.gbif.nub.lookup.fuzzy.HigherTaxaComparator;
import org.gbif.nub.lookup.fuzzy.NubIndex;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.LookupUsage;
import org.gbif.nub.lookup.straight.LookupUsageMatch;
import org.gbif.utils.file.InputStreamUtils;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Guice module setting up all dependencies to expose the NubMatching service.
 */
@Configuration
@ComponentScan(
  basePackages = {
    "org.gbif.nub"
  })
public class NubMatchingTestConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingTestConfiguration.class);

  @Bean
  public static NubIndex provideIndex() throws IOException {
    return NubIndex.newMemoryIndex(loadIndexJson());
  }

  @Bean
  public static IdLookup provideLookup() throws IOException {
    return IdLookupImpl.temp().load(loadLookupJson());
  }

  @Bean
  public static HigherTaxaComparator provideSynonyms() throws IOException {
    LOG.info("Loading synonym dictionaries from classpath ...");
    HigherTaxaComparator syn = new HigherTaxaComparator();
    syn.loadClasspathDicts("dicts");
    return syn;
  }

  @Bean
  public NameParser provideParser() {
    return NameParsers.INSTANCE;
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
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    int id = 1;
    while (id < 295) {
      String file = "index/nub" + id + ".json";
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
          Assertions.fail("Failed to read " + file + ": " + e.getMessage());
        }
      }
      id++;
    }
    return Lists.newArrayList(usages.values());
  }

  /**
   * Load all uXX.json files from the lookup resources into a distinct list of LookupUsage instances.
   * The individual uXX.json files are regular results of a LookupUsageMatch and can be added to the folder
   * to be picked up here.
   */
  private static List<LookupUsage> loadLookupJson() {
    Map<Integer, LookupUsage> usages = Maps.newHashMap();

    InputStreamUtils isu = new InputStreamUtils();
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    int id = 1;
    while (id < 100) {
      String file = String.format("lookup/u%03d.json", id);
      InputStream json = isu.classpathStream(file);
      if (json != null) {
        try {
          int before = usages.size();
          LookupUsageMatch m = mapper.readValue(json, LookupUsageMatch.class);
          for (LookupUsage u : extractUsages(m)) {
            if (u != null) {
              usages.put(u.getKey(), u);
            }
          }
          System.out.println("Loaded " + (usages.size() - before) + " new usage(s) from " + file);
        } catch (IOException e) {
          Assertions.fail("Failed to read " + file + ": " + e.getMessage());
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

  private static List<LookupUsage> extractUsages(LookupUsageMatch m) {
    List<LookupUsage> usages = Lists.newArrayList();
    usages.add(m.getMatch());
    if (m.getAlternatives() != null) {
      usages.addAll(m.getAlternatives());
    }
    return usages;
  }

}
