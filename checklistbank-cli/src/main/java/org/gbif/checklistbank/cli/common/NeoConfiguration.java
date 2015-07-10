package org.gbif.checklistbank.cli.common;

import java.io.File;
import java.util.UUID;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@SuppressWarnings("PublicField")
public class NeoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(NeoConfiguration.class);

    @NotNull
    @Parameter(names = "--neo-repo")
    public File neoRepository;

    @NotNull
    @Parameter(names = "--neo-batchsize")
    public int batchSize = 10000;

    /**
     * none, soft, weak, strong
     * see http://docs.neo4j.org/chunked/stable/configuration-caches.html
     */
    @NotNull
    @Parameter(names = "--neo-cache-type")
    public String cacheType = "soft";

    @Min(0)
    @Parameter(names = "--neo-mapped-memory")
    public int mappedMemory = 256;

    public File neoDir(UUID datasetKey) {
        return new File(neoRepository, datasetKey.toString());
    }

    /**
     * @return the KVP dbmap file used for the given dataset
     */
    public File kvp(UUID datasetKey) {
        return new File(neoRepository, "kvp" + File.separator + datasetKey.toString());
    }
}
