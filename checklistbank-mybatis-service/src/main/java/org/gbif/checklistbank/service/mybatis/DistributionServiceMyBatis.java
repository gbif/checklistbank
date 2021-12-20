package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.checklistbank.service.mybatis.mapper.DistributionMapper;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a DistributionService using MyBatis.
 */
public class DistributionServiceMyBatis extends NameUsageComponentServiceMyBatis<Distribution>
  implements DistributionService {

  @Autowired
  DistributionServiceMyBatis(DistributionMapper distributionMapper) {
    super(distributionMapper);
  }

}
