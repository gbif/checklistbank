package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.concurrent.NamedThreadFactory;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.service.mybatis.DatasetImportServiceMyBatis;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thin wrapper that delegates all import methods to both postgres and solr using a single threaded executor to update solr.
 * We only use an executor for solr cause the solr syncs depend on the sql one be completed first as postgres issues new keys solr needs to know about.
 * As solr should be much faster than postgres we only use a single thread here.
 */
public class DatasetImportServiceCombined implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DatasetImportServiceCombined.class);

    private final DatasetImportServiceMyBatis sqlService;
    private final NameUsageIndexService solrService;
    private final ExecutorService solrExecutor;

    public DatasetImportServiceCombined(DatasetImportServiceMyBatis sqlService, NameUsageIndexService solrService, int importerPoolSize) {
        this.sqlService = sqlService;
        sqlService.useParallelSyncing(importerPoolSize);
        this.solrService = solrService;
        Preconditions.checkArgument(importerPoolSize > 0, "Number of importer threads needs to be at least one");
        int solrThreads = 1+Math.round(importerPoolSize/3);
        solrExecutor = Executors.newFixedThreadPool(solrThreads, new NamedThreadFactory("solr-sync"));
    }

    public int syncUsage(boolean insert, NameUsage usage, List<Integer> parents, @Nullable VerbatimNameUsage verbatim, NameUsageMetrics metrics, @Nullable UsageExtensions extensions) {
        Preconditions.checkNotNull(usage.getDatasetKey(), "datasetKey must exist");
        int key = sqlService.syncUsage(insert, usage, verbatim, metrics, extensions);
        solrExecutor.submit(new SolrUpdate(usage, parents, extensions));
        return key;
    }

    public void updateForeignKeys(int usageKey, Integer parentKey, Integer basionymKey) {
        sqlService.updateForeignKeys(usageKey, parentKey, basionymKey);
        solrExecutor.submit(new SolrUpdateById(usageKey));
    }

    public int deleteOldUsages(UUID datasetKey, Date before) {
        LOG.debug("Deleting all usages in dataset {} before {}", datasetKey, before);
        // iterate over all ids to be deleted and remove them from solr first
        int counter = 0;
        for (Integer id : sqlService.listOldUsages(datasetKey, before)) {
            sqlService.delete(id);
            solrExecutor.submit(new SolrDelete(id));
            counter++;
        }
        LOG.info("Deleted {} usages from dataset {} before {}", counter, datasetKey, before);
        return counter;
    }

    @Override
    public void close() throws Exception {
        LOG.info("Closing DatasetImportServiceCombined");
        sqlService.close();
        LOG.info("Shutting down solr executor");
        solrExecutor.shutdown();
        if (!solrExecutor.awaitTermination(2*60, TimeUnit.SECONDS)) {
            List<?> jobs = solrExecutor.shutdownNow(); // Cancel currently executing tasks
            LOG.warn("Shutting down solr executor instantly, loosing {} updates", jobs.size());
        }
    }

    class SolrUpdate implements Runnable {
        private final NameUsage usage;
        private final List<Integer> parents;
        private final UsageExtensions extensions;

        public SolrUpdate(NameUsage usage, List<Integer> parents, UsageExtensions extensions) {
            this.usage = usage;
            this.parents = parents;
            this.extensions = extensions;
        }

        @Override
        public void run() {
            solrService.insertOrUpdate(usage, parents, extensions);
        }
    }

    class SolrUpdateById implements Runnable {
        private final int id;

        public SolrUpdateById(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            solrService.insertOrUpdate(id);
        }
    }

    class SolrDelete implements Runnable {
        private final int id;

        public SolrDelete(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            solrService.delete(id);
        }
    }
}
