package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.checklistbank.service.mybatis.mapper.DistributionMapper;

import com.google.inject.Inject;

/**
 * Implements a DistributionService using MyBatis.
 */
public class DistributionServiceMyBatis extends NameUsageComponentServiceMyBatis<Distribution>
  implements DistributionService {

  @Inject
  DistributionServiceMyBatis(DistributionMapper distributionMapper) {
    super(distributionMapper);
  }
}
