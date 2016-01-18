package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.model.UsageForeignKeys;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ImporterCallback;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;

/**
 * Implementation that does nothing. Only useful for testing other parts of the system.
 */
public class NameUsageIndexServicePassThru implements DatasetImportService {
  private final List<Integer> empty = ImmutableList.<Integer>builder().build();

  @Override
  public Future<List<Integer>> sync(UUID datasetKey, ImporterCallback dao, Iterable<Integer> usageNeoIds) {
    for (int id : usageNeoIds) {
      checkUsage(dao.readUsage(id), dao.readName(id));
    }
    return Futures.immediateFuture(empty);
  }

  private void checkUsage(NameUsage u, ParsedName pn) {
    Preconditions.checkNotNull(u.getKey(), "Missing usage key for " + u.getScientificName());
    Preconditions.checkNotNull(u.getScientificName(), "Missing name for " + u.getKey());
    Preconditions.checkNotNull(pn, "Missing parsed name for " + u.getKey());
  }

  @Override
  public Future<List<Integer>> sync(UUID datasetKey, List<NameUsage> usages, List<ParsedName> names) {
    Iterator<ParsedName> iter = names.iterator();
    for (NameUsage u : usages) {
      checkUsage(u, iter.next());
    }
    return Futures.immediateFuture(empty);
  }

  @Override
  public Future<List<Integer>> updateForeignKeys(List<UsageForeignKeys> fks) {
    for (UsageForeignKeys fk : fks) {
      Preconditions.checkNotNull(fk.getUsageKey());
    }
    return Futures.immediateFuture(empty);
  }

  @Override
  public void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations) {
    for (Integer i : relations.keySet()) {
      Preconditions.checkNotNull(i);
    }
  }

  @Override
  public int deleteDataset(UUID datasetKey) {
    return 0;
  }

  @Override
  public Future<List<Integer>> deleteUsages(UUID datasetKey, List<Integer> usageKeys) {
    for (Integer i : usageKeys) {
      Preconditions.checkNotNull(i);
    }
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
