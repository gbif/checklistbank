package org.gbif.checklistbank.service.mybatis.mapper;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface NubRelMapper {

  void insert(@Param("uuid") UUID datasetKey, @Param("usageKey") int usageKey, @Param("nubKey") int nubKey);

  void deleteByDataset(@Param("uuid") UUID datasetKey);
}
