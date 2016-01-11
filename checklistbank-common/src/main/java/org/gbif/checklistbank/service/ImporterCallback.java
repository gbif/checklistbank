package org.gbif.checklistbank.service;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.model.UsageExtensions;

import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
public interface ImporterCallback {

  /**
   * Return the usage for the given (neo4j node) id.
   * There can be multiple usages in case of the (rare) pro parte synonyms.
   */
  NameUsage readUsage(long id);

  boolean isInsert(NameUsage usage);

  UsageExtensions readExtensions(long id);

  NameUsageMetrics readMetrics(long id);

  VerbatimNameUsage readVerbatim(long id);

  List<Integer> readParentKeys(long id);

  void reportNewUsageKey(long id, int usageKey);

  void reportNewFuture(Future<Boolean> future);
}
