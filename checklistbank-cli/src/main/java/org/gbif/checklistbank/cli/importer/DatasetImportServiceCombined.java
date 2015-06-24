package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.service.DatasetImportService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * A thin wrapper that delegates all import methods to both postgres and solr.
 * Solr index updates are handled asynchroneously in a separate thread but if the queue gets too large
 * it will block the current thread until the queue is small enough again.
 */
public class DatasetImportServiceCombined {
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

  public void updateForeignKeys(int usageKey, Integer parentKey, Integer proparteKey, Integer basionymKey) {
    sqlService.updateForeignKeys(usageKey, parentKey, proparteKey, basionymKey);
    solrService.insertOrUpdate(usageKey);
  }

  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    sqlService.insertNubRelations(datasetKey, relations);
    solrService.insertOrUpdate(relations.keySet());
 }

  public int deleteDataset(UUID datasetKey) {
    solrService.delete(datasetKey);
    return sqlService.deleteDataset(datasetKey);
  }

  public int deleteOldUsages(UUID datasetKey, Date before) {
    // iterate over all ids to be deleted and remove them from solr first
    for (Integer id : sqlService.listOldUsages(datasetKey, before)) {
      solrService.delete(id);
    }
    // finally remove them from postgres
    return sqlService.deleteOldUsages(datasetKey, before);
  }

  public List<Integer> listOldUsages(UUID datasetKey, Date before) {
    return sqlService.listOldUsages(datasetKey, before);
  }
}
