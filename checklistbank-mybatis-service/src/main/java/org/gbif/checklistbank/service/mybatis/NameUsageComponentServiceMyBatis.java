package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.Constants;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.NameUsageComponentService;

import java.util.List;
import javax.annotation.Nullable;

import com.google.inject.Inject;

/**
 * Implements the NameUsageComponentService using MyBatis.
 * All PagingResponses will not have the count set as it can be too costly sometimes.
 *
 * @param <T> the interpreted model class.
 */
public class NameUsageComponentServiceMyBatis<T> implements NameUsageComponentService<T> {

  private final NameUsageComponentMapper<T> mapper;

  @Inject
  NameUsageComponentServiceMyBatis(NameUsageComponentMapper<T> mapper) {
    this.mapper = mapper;
  }

  @Override
  public T get(int key) {
    return mapper.get(key);
  }

  @Override
  public PagingResponse<T> listByUsage(int usageKey, @Nullable Pageable page) {
    if (page == null) {
      page = new PagingRequest();
    }
    // is this a non nub usage?
    List<T> result;
    if (isNub(usageKey)) {
      result = mapper.listByNubUsage(usageKey, page);
    } else {
      result = mapper.listByChecklistUsage(usageKey, page);
    }

    return new PagingResponse<T>(page, null, result);
  }

  private boolean isNub(int usageKey) {
    return usageKey <= Constants.NUB_MAXIMUM_KEY;
  }

  /**
   * Lists all name usages with a key between start / end.
   *
   * @throws IllegalArgumentException if start <= end
   */
  public List<T> listRange(int usageKeyStart, int usageKeyEnd) {
    if (usageKeyStart > usageKeyEnd) {
      throw new IllegalArgumentException("start " + usageKeyStart + " > end " + usageKeyEnd + " range");
    }
    if (isNub(usageKeyEnd)) {
      // only nub usages
      return mapper.listByNubUsageRange(usageKeyStart, usageKeyEnd);
    } else if (!isNub(usageKeyStart)) {
      // only checklist usages
      return mapper.listByChecklistUsageRange(usageKeyStart, usageKeyEnd);
    } else {
      // mixed !!!
      // get nubs first
      List<T> usages = mapper.listByNubUsageRange(usageKeyStart, Constants.NUB_MAXIMUM_KEY);
      usages.addAll(mapper.listByChecklistUsageRange(Constants.NUB_MAXIMUM_KEY + 1, usageKeyEnd));
      return usages;
    }
  }

}
