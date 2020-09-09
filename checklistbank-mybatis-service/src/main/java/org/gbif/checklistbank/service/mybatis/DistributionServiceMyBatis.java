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

  private final DistributionMapper mapper;

  @Inject
  DistributionServiceMyBatis(DistributionMapper distributionMapper) {
    super(distributionMapper);
    mapper = distributionMapper;
  }

  /**
   * Returns all extension records for a checklist usage.
   *
   * @param taxonKey the usage the extensions are related to
   * @param countryCode  country code to filter by. If null filter for distributions without a country
   * @param page     paging parameters or null for first page with default size
   *
   * @return Wrapper that contains a potentially empty component list, but never null
   */
  public PagingResponse<Distribution> listByUsageAndCountry(int taxonKey, @Nullable String countryCode, @Nullable Pageable page){
    if (page == null) {
      page = new PagingRequest();
    }
    Country country = null;
    if (countryCode != null) {
      country = Country.fromIsoCode(countryCode);
      if (country == null) {
        throw new IllegalArgumentException("Unknown country ISO code " + countryCode);
      }
    }
    // is this a non nub usage?
    List<Distribution> result;
    if (isNub(taxonKey)) {
      result = mapper.listByNubUsageAndCountry(taxonKey, country, page);
    } else {
      result = mapper.listByChecklistUsageAndCountry(taxonKey, country, page);
    }

    return new PagingResponse<>(page, null, result);
  }

}
