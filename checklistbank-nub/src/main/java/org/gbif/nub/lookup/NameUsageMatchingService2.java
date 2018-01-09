package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;

/**
 *
 */
public interface NameUsageMatchingService2 extends NameUsageMatchingService {

  NameUsageMatch2 v2(NameUsageMatch m);

}
