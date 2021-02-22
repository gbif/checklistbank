package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.checklistbank.model.IucnRedListCategory;

import org.apache.ibatis.annotations.Param;

import java.util.UUID;


/**
 * The MyBatis mapper interface for Distribution.
 */
public interface DistributionMapper extends NameUsageComponentMapper<Distribution> {
  UUID iucnDatasetKey = UUID.fromString("19491596-35ae-4a91-9a98-85cf505f1bd3");

  void insert(@Param("key") int usageKey, @Param("obj") Distribution distribution, @Param("sourceKey") Integer sourceKey);

  IucnRedListCategory getIucnRedListCategory(@Param("key") int nubKey);

}
