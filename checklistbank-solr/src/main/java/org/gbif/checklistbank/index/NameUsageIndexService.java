package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.NameUsageContainer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Real time solr service for checklistbank.
 */
public interface NameUsageIndexService {

  void delete(int usageKey);

  void delete(UUID datasetKey);

  void insertOrUpdate(int usageKey);

  void insertOrUpdate(Collection<Integer> usageKeys);

  void insertOrUpdate(NameUsageContainer usage, List<Integer> parentKeys);
}
