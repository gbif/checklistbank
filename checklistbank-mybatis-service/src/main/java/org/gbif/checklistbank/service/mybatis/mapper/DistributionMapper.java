package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.Distribution;

import org.apache.ibatis.annotations.Param;


/**
 * The MyBatis mapper interface for Distribution.
 */
public interface DistributionMapper extends NameUsageComponentMapper<Distribution> {

  void insert(@Param("key") int usageKey, @Param("obj") Distribution distribution, @Param("sourceKey") Integer sourceKey);

  String getIucnRedListCategory(@Param("key") int usageKey);

}
