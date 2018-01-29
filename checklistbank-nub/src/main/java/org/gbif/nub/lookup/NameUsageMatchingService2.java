package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.v2.NameUsageMatch2;

/**
 *
 */
public interface NameUsageMatchingService2 extends NameUsageMatchingService {

  NameUsageMatch2 v2(NameUsageMatch m);

}
