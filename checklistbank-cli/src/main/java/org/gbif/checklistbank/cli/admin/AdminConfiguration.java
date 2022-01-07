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
package org.gbif.checklistbank.cli.admin;

import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.ZooKeeperConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.config.RegistryServiceConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.LineIterator;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class AdminConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public RegistryServiceConfiguration registry = new RegistryServiceConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public ClbConfiguration clb = new ClbConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public ZooKeeperConfiguration zookeeper = new ZooKeeperConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public NeoConfiguration neo;

  @Parameter(names = "--archive-repository")
  @NotNull
  public File archiveRepository;

  @Parameter(names = "--export-repository")
  @NotNull
  public File exportRepository = new File("./exports");

  @Parameter(names = {"-k", "--key"})
  @Nullable
  public UUID key;

  @Parameter(names = {"-f", "--keys"})
  @Nullable
  public File keys;

  @Parameter(names = {"-t", "--type"})
  @NotNull
  public DatasetType type = DatasetType.CHECKLIST;

  @Parameter(names = {"-op", "--operation"}, required = true)
  @NotNull
  public AdminOperation operation;

  @Parameter(names = {"--nub-ranks-only"})
  @Valid
  public boolean nubRanksOnly = false;

  /**
   * Generic custom file parameter for various routines
   */
  @Parameter(names = {"--file"})
  @Valid
  public File file;

  /**
   * Generic custom file parameter for various routines
   */
  @Parameter(names = {"--file2"})
  @Valid
  public File file2;

  /**
   * Nub lookup service used for testing content, see NUB_CHECK operation
   */
  @Parameter(names = {"--nubws"})
  @Valid
  public String nubws = "http://api.gbif.org/v1/species/match";

  /**
   * Do not change any information in checklistbank, just report potential changes, e.g. for re-parsed names
   */
  @Parameter(names = {"--dry-run"})
  public boolean dryRun = false;

  /**
   * If true sets the Backbone dataset key in the key config
   */
  @Parameter(names = {"--nub"})
  public boolean nub = false;

  /**
   * If true sets the Catalog of Life dataset key in the key config
   */
  @Parameter(names = {"--col"})
  public boolean col = false;

  /**
   * If true sets the Plazi publisher key in the key config
   */
  @Parameter(names = {"--plazi"})
  public boolean plazi = false;

  /**
   * If true sets the IUCN redlist dataset key in the key config
   */
  @Parameter(names = {"--iucn"})
  public boolean iucn = false;

  /**
   * Returns the directory with the decompressed archive folder created by the dwca downloader.
   */
  public File archiveDir(UUID datasetKey) {
    return new File(archiveRepository, datasetKey.toString());
  }

  public List<UUID> listKeys() {
    List<UUID> result = new ArrayList<>();
    if (keys != null || keys.exists()) {
      try {
        LineIterator lines = new LineIterator(new FileReader(keys));
        while (lines.hasNext()) {
          try {
            result.add(UUID.fromString(lines.nextLine().trim()));
          } catch (IllegalArgumentException e) {
            // ignore
          }
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    return result;
  }
}
