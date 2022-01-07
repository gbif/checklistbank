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
package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.config.RegistryServiceConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.ParametersDelegate;

public class NubConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public NeoConfiguration neo = new NeoConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public NeoConfiguration neoSources = new NeoConfiguration();

  public int sourceLoaderThreads = 2;

  @ParametersDelegate
  @Valid
  @NotNull
  public ClbConfiguration clb = new ClbConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public RegistryServiceConfiguration registry = new RegistryServiceConfiguration();

  @NotNull
  @Valid
  public List<NubSourceConfig> sources = new ArrayList<>();

  @NotNull
  @Valid
  public Set<String> blacklist = new HashSet<>();

  public boolean isBlacklisted(String name) {
    return blacklist.contains(name.trim().toUpperCase());
  }

  /**
   * List of doubtful names that should be assigned a doubtful status unless they are treated as synonyms.
   */
  @NotNull
  @Valid
  public Set<String> doubtful = new HashSet<>();

  public boolean isDoubtful(String name) {
    return doubtful.contains(name.trim());
  }

  /**
   * List of higher wrong homonyms that should be removed, regardless of which source they came from.
   * Map of a canonical name to its direct parent.
   * All other names with the same canonical name, but different parent, are kept.
   *
   * See https://github.com/gbif/checklistbank/issues/93 for more background.
   */
  @NotNull
  @Valid
  public Map<String, List<String>> homonymExclusions = new HashMap<>();

  /**
   * Checks the homonymExclusion list to see if this combination should be excluded.
   * @return true if the name with the given parent should be excluded
   */
  public boolean isExcludedHomonym(String name, String parent) {
    return parent != null && homonymExclusions.getOrDefault(name, Collections.EMPTY_LIST).contains(parent.trim().toUpperCase());
  }

  /**
   * If true algorithmic detecting and grouping of basionyms is executed.
   */
  @Valid
  public boolean groupBasionyms = false;

  /**
   * List of epithets, organised by families, which should be ignored during the automated basionym grouping/detection.
   */
  @NotNull
  @Valid
  public Map<String, Set<String>> basionymExclusions = new HashMap<>();

  /**
   * If false autonyms with no other sibling are removed.
   */
  @Valid
  public boolean keepLonelyAutonyms = false;

  /**
   * If false implicit names (genus, species) are removed in case they do not have any accepted children.
   */
  @Valid
  public boolean keepEmptyImplicitNames = false;

  /**
   * If true an import message is being send once the build is done and basic backbone assertions have passed successfully.
   */
  @Valid
  public boolean autoImport = false;

  /**
   * If true run validation routines at the end of the nub build, see NubTreeValidation.
   */
  @Valid
  public boolean validate = true;

  /**
   * If true run assertions for a complete production backbone, see NubAssertions.
   */
  @Valid
  public boolean runAssertions = false;

  /**
   * If true some explicit debugging and logging is enabled, e.g. the neo4j mapped memory stats and number of open files and threads.
   */
  @Valid
  @Nullable
  public File reportDir;

  /**
   * In milliseconds.
   */
  @Min(100)
  public int parserTimeout = 1000;

  public NubConfiguration() {
  }

  public NubConfiguration(NeoConfiguration neo) {
    this.neo = neo;
  }

  /**
   * Trims and changes cases for various sensitive configs, e.g. the blacklist.
   */
  public void normalizeConfigs(){
    blacklist = blacklist.stream()
      .map(String::trim)
      .map(String::toUpperCase)
      .collect(Collectors.toSet());

    for (String cn : homonymExclusions.keySet()) {
      if (homonymExclusions.get(cn).isEmpty()) {
        homonymExclusions.remove(cn);
      } else {
        homonymExclusions.put(cn, homonymExclusions.get(cn).stream()
            .map(String::trim)
            .map(String::toUpperCase)
            .collect(Collectors.toList())
        );
      }
    }
  }
}
