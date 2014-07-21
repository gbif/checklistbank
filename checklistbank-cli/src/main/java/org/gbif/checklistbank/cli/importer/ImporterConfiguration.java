package org.gbif.checklistbank.cli.importer;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import org.gbif.checklistbank.cli.common.GangliaConfiguration;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.UUID;

/**
 *
 */
public class ImporterConfiguration {

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

    @Parameter(names = "--msg-pool-size")
    @Min(1)
    public int msgPoolSize = 10;

    @Parameter(names = "--primary-queue-name")
    @NotNull
    public String primaryQueueName;

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
