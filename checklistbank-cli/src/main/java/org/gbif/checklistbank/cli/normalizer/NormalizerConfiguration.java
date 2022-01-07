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
package org.gbif.checklistbank.cli.normalizer;

import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.ZooKeeperConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.config.GangliaConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.File;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

@SuppressWarnings("PublicField")
public class NormalizerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizerConfiguration.class);

    @ParametersDelegate
    @Valid
    @NotNull
    public GangliaConfiguration ganglia = new GangliaConfiguration();

    @ParametersDelegate
    @Valid
    @NotNull
    public NeoConfiguration neo = new NeoConfiguration();

    @ParametersDelegate
    @NotNull
    @Valid
    public MessagingConfiguration messaging = new MessagingConfiguration();

    @ParametersDelegate
    @Valid
    public ZooKeeperConfiguration zookeeper = new ZooKeeperConfiguration();

    @ParametersDelegate
    @Valid
    @NotNull
    public ClbConfiguration clb = new ClbConfiguration();

    @Parameter(names = "--pool-size")
    @Min(1)
    public int poolSize = 3;

    @Parameter(names = "--archive-repository")
    @NotNull
    public File archiveRepository;

    /**
     * Returns the directory with the decompressed archive folder created by the dwca downloader.
     */
    public File archiveDir(UUID datasetKey) {
        return new File(archiveRepository, datasetKey.toString());
    }

}
