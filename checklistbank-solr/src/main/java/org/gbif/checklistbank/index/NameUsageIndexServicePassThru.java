package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.model.UsageForeignKeys;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ImporterCallback;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;

/**
 * Implementation that does nothing. Only useful for testing other parts of the system.
 */
public class NameUsageIndexServicePassThru implements DatasetImportService {
  private final List<Integer> empty = ImmutableList.<Integer>builder().build();

  @Override
  public Future<List<Integer>> sync(UUID datasetKey, ImporterCallback dao, Iterable<Integer> usages) {
    return Futures.immediateFuture(empty);
  }

  @Override
  public Future<List<Integer>> sync(UUID datasetKey, List<NameUsage> usages, List<ParsedName> names) {
    return Futures.immediateFuture(empty);
  }

  @Override
  public Future<List<Integer>> updateForeignKeys(List<UsageForeignKeys> fks) {
    return Futures.immediateFuture(empty);
  }

  @Override
  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    // nothing to do
  }

  @Override
  public int deleteDataset(UUID datasetKey) {
    return 0;
  }

  @Override
  public Future<List<Integer>> deleteUsages(UUID datasetKey, List<Integer> usageKeys) {
    return Futures.immediateFuture(usageKeys);
  }

  @Override
  public boolean isRunning() {
    return false;
  }

  @Override
  public void close() throws Exception {
    // nothing to do

  }

}
