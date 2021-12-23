package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.DistributionMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Implements a DistributionService using MyBatis. */
@Service
public class DistributionServiceMyBatis extends NameUsageComponentServiceMyBatis<Distribution>
    implements DistributionService {

  @Autowired
  DistributionServiceMyBatis(DistributionMapper distributionMapper) {
    super(distributionMapper);
  }
}
