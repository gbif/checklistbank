package org.gbif.checklistbank.cli.normalizer;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.ZooKeeperConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.config.GangliaConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.UUID;

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
