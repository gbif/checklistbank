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
package org.gbif.checklistbank.cli.crawler;

import org.gbif.checklistbank.config.GangliaConfiguration;
import org.gbif.checklistbank.config.RegistryServiceConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.File;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@SuppressWarnings("PublicField")
public class CrawlerConfiguration {

  @ParametersDelegate
  @Valid
  @NotNull
  public GangliaConfiguration ganglia = new GangliaConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public RegistryServiceConfiguration registry = new RegistryServiceConfiguration();

  @Parameter(names = "--archive-repository")
  @NotNull
  public File archiveRepository;

  @Parameter(names = "--pool-size")
  @Min(1)
  public int poolSize = 1;

  /**
   * Http timeout in milliseconds
   */
  @Parameter(names = "--http-timeout")
  @Min(1)
  public int httpTimeout = 60*1000;

  /**
   * Returns the directory with the decompressed archive folder created by the dwca downloader.
   */
  public File archiveDir(UUID datasetKey) {
    return new File(archiveRepository, datasetKey.toString());
  }

  /**
   * Returns the dwc archive file within the data repository.
   */
  public File archiveFile(UUID datasetKey) {
    return new File(archiveRepository, datasetKey.toString() + ".zip");
  }
}
