package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.checklistbank.model.UsageRelated;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * A generic MyBatis mapper for NameUsageComponent subclasses.
 *
 * @param <T> the interpreted model class.
 */
public interface NameUsageComponentMapper<T> {

  List<T> listByChecklistUsage(@Param("key") int usageKey, @Param("page") Pageable page);

  List<T> listByNubUsage(@Param("key") int nubKey, @Param("page") Pageable page);

  List<UsageRelated<T>> listByChecklistUsageRange(@Param("start") int usageKeyStart, @Param("end") int usageKeyEnd);

  List<UsageRelated<T>> listByNubUsageRange(@Param("start") int usageKeyStart, @Param("end") int usageKeyEnd);

}
