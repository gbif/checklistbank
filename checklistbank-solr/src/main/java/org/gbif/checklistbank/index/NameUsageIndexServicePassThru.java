package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.model.UsageExtensions;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Implementation that does nothing. Only useful for testing other parts of the system.
 */
public class NameUsageIndexServicePassThru implements NameUsageIndexService {
  @Override
  public void delete(int usageKey) {
  }

  @Override
  public void delete(UUID datasetKey) {
  }

  @Override
  public void insertOrUpdate(int usageKey) {
  }

  @Override
  public void insertOrUpdate(Collection<Integer> usageKeys) {
  }

  @Override
  public void insertOrUpdate(NameUsage usage, List<Integer> parentKeys, UsageExtensions extensions) {
  }
}
