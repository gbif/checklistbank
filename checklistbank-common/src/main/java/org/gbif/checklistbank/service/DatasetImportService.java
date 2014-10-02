package org.gbif.checklistbank.service;

import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.model.Usage;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
     *
     * @param datasetKey
     * @param usages
     */
    void insertUsages(UUID datasetKey, Iterator<Usage> usages);

    /**
     * Inserts or updates a complete name usage with all its extension data.
     *
     * @param usage
     * @param metrics
     * @return the new or existing usage key
     */
    int syncUsage(UUID datasetKey, NameUsageContainer usage, VerbatimNameUsage verbatim, NameUsageMetrics metrics);

    void updateForeignKeys(int usageKey, Integer parentKey, Integer proparteKey, Integer basionymKey);

    /**
     * Delete all existing nub relations and then batch insert new ones from the passed iterator.
     *
     * @param datasetKey the datasource to map to the nub
     * @param relations  map from source usage id to a nub usage id for all usages in a dataset
     */
    void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations);

    /**
     * Remove entire dataset from checklistbank
     * @return number of deleted usage records
     */
    int deleteDataset(UUID datasetKey);

    /**
     * Removes all usages and related data from a dataset that was last interpreted before the given date.
     *
     * @param datasetKey
     * @param before     threshold date
     * @return number of deleted old records
     */
    int deleteOldUsages(UUID datasetKey, Date before);

    /**
     * Lists all old name usage ids last interpreted before the given date.
     */
    List<Integer> listOldUsages(UUID datasetKey, Date before);
}
