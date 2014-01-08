package org.gbif.nub.lookup;

import org.gbif.api.model.common.LinneanClassification;

/**
 * Interface to get pure classification information for name usages.
 */
public interface ClassificationResolver {

  LinneanClassification getClassification(int usageKey);

}
