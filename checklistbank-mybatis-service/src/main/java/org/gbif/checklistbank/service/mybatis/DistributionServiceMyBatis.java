package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.checklistbank.service.mybatis.mapper.DistributionMapper;

import com.google.inject.Inject;

import javax.annotation.Nullable;
import java.util.List;

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
