package org.gbif.checklistbank.cli.admin;

import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.RegistryServiceConfiguration;
import org.gbif.checklistbank.cli.common.ZooKeeperConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.internal.Lists;
import org.apache.commons.io.LineIterator;

/**
 *
 */
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
    @Valid
    @NotNull
    public ZooKeeperConfiguration zookeeper = new ZooKeeperConfiguration();

    @ParametersDelegate
    @NotNull
    @Valid
    public NeoConfiguration neo;

    @Parameter(names = "--archive-repository")
    @NotNull
    public File archiveRepository;

    @Parameter(names = {"-k", "--key"}, required = false)
    @Nullable
    public UUID key;

    @Parameter(names = {"-f", "--keys"}, required = false)
    @Nullable
    public File keys;

    @Parameter(names = {"-t", "--type"}, required = false)
    @NotNull
    public DatasetType type = DatasetType.CHECKLIST;

    @Parameter(names = {"-op", "--operation"}, required = true)
    @NotNull
    public AdminOperation operation;

    /**
     * Returns the directory with the decompressed archive folder created by the dwca downloader.
     */
    public File archiveDir(UUID datasetKey) {
        return new File(archiveRepository, datasetKey.toString());
    }

    public List<UUID> listKeys() {
        List<UUID> result = Lists.newArrayList();
        if (keys != null || keys.exists()) {
            try {
                LineIterator lines = new LineIterator(new FileReader(keys));
                while (lines.hasNext()) {
                    try {
                        result.add(UUID.fromString(lines.nextLine()));
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
