package org.gbif.checklistbank.cli.common;

import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.UUID;

/**
 *
 */
public class NeoConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(NeoConfiguration.class);

    @NotNull
    @Parameter(names = "--neo-repo")
    public File neoRepository;

    @NotNull
    @Parameter(names = "--neo-batchsize")
    public int batchSize = 10000;

    public File neoDir(UUID datasetKey) {
        return new File(neoRepository, datasetKey.toString());
    }

    /**
     * Creates a new emedded db in the neoRepository folder.
     * @param datasetKey subfolder for the db
     */
    public GraphDatabaseService newEmbeddedDb(UUID datasetKey) {
        Preconditions.checkNotNull(datasetKey);
        return embeddedDb(datasetKey);
    }

    /**
     * Creates a new emedded db directly in the neoRepository folder.
     */
    public GraphDatabaseService newEmbeddedDb() {
        return embeddedDb(null);
    }

    /**
     * Creates a new embedded db in the neoRepository folder.
     * @param datasetKey subfolder for the db, if null uses neoRepo directly
     */
    private GraphDatabaseService embeddedDb(@Nullable UUID datasetKey) {
        GraphDatabaseFactory factory = new GraphDatabaseFactory();
        File storeDir = datasetKey == null ? neoRepository: neoDir(datasetKey);
        GraphDatabaseService db = factory.newEmbeddedDatabaseBuilder(storeDir.getAbsolutePath())
            .setConfig(GraphDatabaseSettings.cache_type, "none")
            .setConfig(GraphDatabaseSettings.nodestore_mapped_memory_size, "256M")
            .setConfig(GraphDatabaseSettings.nodestore_propertystore_index_keys_mapped_memory_size, "64M")
            .setConfig(GraphDatabaseSettings.nodestore_propertystore_mapped_memory_size, "32M")
            .setConfig(GraphDatabaseSettings.relationshipstore_mapped_memory_size, "512M")
            .newGraphDatabase();
        LOG.info("Starting neo4j database from {}", storeDir.getAbsolutePath());
        return db;
    }

}
