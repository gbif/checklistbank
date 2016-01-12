package org.gbif.checklistbank.service;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.model.UsageExtensions;

import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 *
 */
public interface UsageSyncService {

  int syncUsage(boolean insert, NameUsage usage, ParsedName pn, NameUsageMetrics metrics);

  void syncUsageExtras(boolean insert, UUID datasetKey, int usageKey, @Nullable VerbatimNameUsage verbatim, @Nullable UsageExtensions extensions);

  void updateForeignKeys(int usageKey, Integer parentKey, Integer basionymKey);

  void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations);

  int deleteDataset(UUID datasetKey);

  void delete(int key);
}
