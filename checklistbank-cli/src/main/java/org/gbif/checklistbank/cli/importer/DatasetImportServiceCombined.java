package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.service.DatasetImportService;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thin wrapper that delegates all import methods to both postgres and solr.
 * Solr index updates are handled asynchroneously in a separate thread but if the queue gets too large
 * it will block the current thread until the queue is small enough again.
 */
public class DatasetImportServiceCombined {
    private static final Logger LOG = LoggerFactory.getLogger(DatasetImportServiceCombined.class);

    private final DatasetImportService sqlService;
    private final NameUsageIndexService solrService;

    public DatasetImportServiceCombined(DatasetImportService sqlService, NameUsageIndexService solrService) {
        this.sqlService = sqlService;
        this.solrService = solrService;
    }

    public int syncUsage(NameUsage usage, List<Integer> parents, @Nullable VerbatimNameUsage verbatim, NameUsageMetrics metrics, @Nullable UsageExtensions extensions) {
        Preconditions.checkNotNull(usage.getDatasetKey(), "datasetKey must exist");
        int key = sqlService.syncUsage(usage, verbatim, metrics, extensions);
        solrService.insertOrUpdate(usage, parents, extensions);
        return key;
    }

    public void updateForeignKeys(int usageKey, Integer parentKey, Integer basionymKey) {
        sqlService.updateForeignKeys(usageKey, parentKey, basionymKey);
        solrService.insertOrUpdate(usageKey);
    }

    public int deleteOldUsages(UUID datasetKey, Date before) {
        LOG.debug("Deleting all usages in dataset {} before {}", datasetKey, before);
        // iterate over all ids to be deleted and remove them from solr first
        int counter = 0;
        for (Integer id : sqlService.listOldUsages(datasetKey, before)) {
            solrService.delete(id);
            sqlService.delete(id);
            counter++;
        }
        LOG.info("Deleted all {} usages from dataset {} before {}", counter, datasetKey, before);
        return counter;
    }

}
