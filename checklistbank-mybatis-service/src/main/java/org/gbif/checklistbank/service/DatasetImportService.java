package org.gbif.checklistbank.service;

import org.gbif.checklistbank.service.mybatis.model.Usage;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Persistence service dealing with methods needed to import new checklists into checklistbank.
 * The methods are mostly doing batch operations and larger operations, hardly any single record modifications.
 * This interface is restricted to the mybatis module only!
 */
public interface DatasetImportService {

  /**
   * Batch insert usages for the given dataset.
   * @param datasetKey
   * @param usages
   */
  void insertUsages(UUID datasetKey, Iterator<Usage> usages);

  /**
   * Delete all existing nub relations and then batch insert new ones from the passed iterator.
   * @param datasetKey the datasource to map to the nub
   * @param relations map from source usage id to a nub usage id for all usages in a dataset
   */
  void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations);

  /**
   * Remove entire dataset from checklistbank
   */
  void deleteDataset(UUID datasetKey);
}
