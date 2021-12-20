package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.Constants;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.NameUsageExtensionService;
import org.gbif.checklistbank.model.UsageRelated;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageComponentMapper;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements the NameUsageComponentService using MyBatis.
 * All PagingResponses will not have the count set as it can be too costly sometimes.
 *
 * @param <T> the interpreted model class.
 */
public class NameUsageComponentServiceMyBatis<T> implements NameUsageExtensionService<T> {

  private final NameUsageComponentMapper<T> mapper;

  @Autowired
  NameUsageComponentServiceMyBatis(NameUsageComponentMapper<T> mapper) {
    this.mapper = mapper;
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

  protected boolean isNub(int usageKey) {
    return usageKey <= Constants.NUB_MAXIMUM_KEY;
  }

  /**
   * Lists all name usages with a key between start / end.
   *
   * @throws IllegalArgumentException if start <= end
   */
  public Map<Integer, List<T>> listRange(int usageKeyStart, int usageKeyEnd) {
    if (usageKeyStart > usageKeyEnd) {
      throw new IllegalArgumentException("start " + usageKeyStart + " > end " + usageKeyEnd + " range");
    }

    List<UsageRelated<T>> related;
    if (isNub(usageKeyEnd)) {
      // only nub usages
      related = mapper.listByNubUsageRange(usageKeyStart, usageKeyEnd);

    } else if (!isNub(usageKeyStart)) {
      // only checklist usages
      related = mapper.listByChecklistUsageRange(usageKeyStart, usageKeyEnd);

    } else {
      // mixed !!!
      // get nubs first
      related = mapper.listByNubUsageRange(usageKeyStart, Constants.NUB_MAXIMUM_KEY);
      related.addAll(mapper.listByChecklistUsageRange(Constants.NUB_MAXIMUM_KEY + 1, usageKeyEnd));
    }

    Map<Integer, List<T>> result = Maps.newHashMap();
    for (UsageRelated<T> r : related) {
      if (r.getValue() == null || r.getValue() instanceof Integer ) {
        // value can be null when all properties of the bean (e.g. SpeciesProfile) are null
        // in that case mybatis DOES NOT CREATE AN OBJECT !!!
        // for some association queries it actually returns an INTEGER instead of the UsageRelated class :(
        continue;
      }
      if (!result.containsKey(r.getUsageKey())) {
        result.put(r.getUsageKey(), Lists.<T>newArrayList());
      }
      result.get(r.getUsageKey()).add(r.getValue());
    }
    return result;
  }

}
