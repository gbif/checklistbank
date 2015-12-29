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

  List<UsageCount> children(@Param("key") Integer parentKey);
}
