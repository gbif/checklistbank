package org.gbif.checklistbank.service.mybatis.mapper;

import org.apache.ibatis.session.ResultHandler;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.checklistbank.model.UsageRelated;

import java.util.List;
import java.util.UUID;

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

  void deleteByUsage(@Param("key") int usageKey);

  /**
   * Iterates over all components of a given dataset and processes them with the supplied handler.
   * This allows a single query to efficiently stream all its values without keeping them in memory.
   *
   * @param handler to process each name usage with
   */
  void processDataset(@Param("uuid") UUID datasetKey, ResultHandler<T> handler);

}
