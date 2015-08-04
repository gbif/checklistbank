package org.gbif.checklistbank.service;

import org.gbif.api.model.checklistbank.NameUsage;

import java.util.List;
import java.util.UUID;

/**
 * Persistence service dealing with name usages.
 * This interface is restricted to the mybatis module only!
 */
public interface UsageService {

  /**
   * @return int array of all current (not deleted) name usage ids in checklist bank
   */
  List<Integer> listAll();

  /**
   * @return the highest usageKey used in the dataset
   */
  Integer maxUsageKey(UUID datasetKey);

  /**
   * Lists al1l name usages with a key between start / end.
   */
  List<NameUsage> listRange(int usageKeyStart, int usageKeyEnd);

  /**
   * Lists classification as parent keys.
   */
  List<Integer> listParents(int usageKey);

}
