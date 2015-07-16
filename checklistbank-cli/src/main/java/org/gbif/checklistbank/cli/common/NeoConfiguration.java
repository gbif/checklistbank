package org.gbif.checklistbank.cli.common;

import org.gbif.checklistbank.utils.CleanupUtils;

import java.io.File;
import java.util.UUID;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.google.common.io.Files;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
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

    /**
     * Creates a new embedded neo db in the temporary directory and registers a cleanup hook that removes all files whne the JVM shuts down.
     */
    public GraphDatabaseService newTmpDb() {
        File storeDir = Files.createTempDir();
        CleanupUtils.registerCleanupHook(storeDir);
        LOG.info("Creating new tmp neo4j db with {}M mapped memory in {}", mappedMemory, storeDir.getAbsolutePath());
        return new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(storeDir.getAbsolutePath())
                .setConfig(GraphDatabaseSettings.keep_logical_logs, "false")
                .setConfig(GraphDatabaseSettings.cache_type, cacheType)
                .setConfig(GraphDatabaseSettings.pagecache_memory, mappedMemory+"M")
                .newGraphDatabase();
    }
}
