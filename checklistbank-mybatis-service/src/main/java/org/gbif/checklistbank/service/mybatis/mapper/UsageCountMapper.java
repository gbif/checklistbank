package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.checklistbank.model.UsageCount;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/**
 *
 */
public interface UsageCountMapper {

  List<UsageCount> root(@Param("key") UUID datasetKey);

  /**
   * @return all children sorted by rank, then name
   */
  List<UsageCount> children(@Param("key") Integer parentKey);
}
