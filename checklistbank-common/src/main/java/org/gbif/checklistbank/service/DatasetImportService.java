package org.gbif.checklistbank.service;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.model.UsageExtensions;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Persistence service dealing with methods needed to import new checklists into checklistbank.
 * The methods are mostly doing batch operations and larger operations, hardly any single record modifications.
 * This interface is restricted to the mybatis module only!
 */
public interface DatasetImportService {

    /**
     * Inserts or updates a complete name usage with all its extension data.
     * The usage will have the key set to either an existing key or a newly generated one.
     *
     * @return the new or existing usage key, same as usage.key
     */
    int syncUsage(NameUsage usage, @Nullable VerbatimNameUsage verbatim, @Nullable NameUsageMetrics metrics, @Nullable UsageExtensions extensions);

    void updateForeignKeys(int usageKey, Integer parentKey, Integer basionymKey);

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
     * Deletes a single usage and its related extension data
     * @param key usage key
     */
    void delete(int key);

    /**
     * Lists all old name usage ids last interpreted before the given date.
     */
    List<Integer> listOldUsages(UUID datasetKey, Date before);
}
