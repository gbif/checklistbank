package org.gbif.checklistbank.cli.normalizer;

import org.gbif.checklistbank.cli.common.GangliaConfiguration;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.ZooKeeperConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.File;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
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
    @NotNull
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
