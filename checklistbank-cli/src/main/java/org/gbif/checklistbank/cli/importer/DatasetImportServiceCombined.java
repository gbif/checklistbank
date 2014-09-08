package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.model.Usage;
import org.gbif.checklistbank.service.DatasetImportService;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A thin wrapper that delegates all import methods to both postgres and solr.
 * Solr index updates are handled asynchroneously in a separate thread but if the queue gets too large
 * it will block the current thread until the queue is small enough again.
 */
public class DatasetImportServiceCombined implements DatasetImportService {
  private final DatasetImportService sqlService;
  private final NameUsageIndexService solrService;

  public DatasetImportServiceCombined(DatasetImportService sqlService, NameUsageIndexService solrService) {
    this.sqlService = sqlService;
    this.solrService = solrService;
  }

  @Override
  public void insertUsages(UUID datasetKey, Iterator<Usage> usages) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int syncUsage(UUID datasetKey, NameUsageContainer usage, VerbatimNameUsage verbatim, NameUsageMetrics metrics) {
    int key = sqlService.syncUsage(datasetKey, usage, verbatim, metrics);
    solrService.insertOrUpdate(usage, usage.getVernacularNames(), usage.getDescriptions(), usage.getDistributions(), usage.getSpeciesProfiles());
    return key;
  }

  @Override
  public void updateBasionyms(UUID datasetKey, Map<Integer, Integer> basionymByUsage) {
    sqlService.updateBasionyms(datasetKey, basionymByUsage);
    solrService.insertOrUpdate(basionymByUsage.keySet());
  }

  @Override
  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    sqlService.insertNubRelations(datasetKey, relations);
    solrService.insertOrUpdate(relations.keySet());
 }

  @Override
  public int deleteDataset(UUID datasetKey) {
    solrService.delete(datasetKey);
    return sqlService.deleteDataset(datasetKey);
  }

  @Override
  public int deleteOldUsages(UUID datasetKey, Date before) {
    // iterate over all ids to be deleted and remove them from solr first
    for (Integer id : sqlService.listOldUsages(datasetKey, before)) {
      solrService.delete(id);
    }
    // finally remove them from postgres
    return sqlService.deleteOldUsages(datasetKey, before);
  }

  @Override
  public List<Integer> listOldUsages(UUID datasetKey, Date before) {
    return sqlService.listOldUsages(datasetKey, before);
  }
}
