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

import com.google.common.util.concurrent.Futures;

/**
 * Implementation that does nothing. Only useful for testing other parts of the system.
 */
public class NameUsageIndexServicePassThru implements DatasetImportService {

  @Override
  public Future<Boolean> sync(UUID datasetKey, ImporterCallback dao, Iterable<Integer> usages) {
    return Futures.immediateFuture(true);
  }

  @Override
  public Future<Boolean> sync(UUID datasetKey, List<NameUsage> usages, List<ParsedName> names) {
    return Futures.immediateFuture(true);
  }

  @Override
  public Future<Boolean> updateForeignKeys(List<UsageForeignKeys> fks) {
    return Futures.immediateFuture(true);
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
  public Future<Boolean> deleteUsages(UUID datasetKey, List<Integer> usageKeys) {
    return Futures.immediateFuture(true);
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
