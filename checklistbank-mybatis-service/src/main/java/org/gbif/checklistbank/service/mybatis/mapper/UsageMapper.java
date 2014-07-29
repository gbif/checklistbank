package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.checklistbank.service.mybatis.model.Usage;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface UsageMapper {

  List<Usage> list(@Param("uuid") UUID datasetKey, @Param("page") Pageable page);

  void insert(@Param("uuid") UUID datasetKey, @Param("usage") Usage usage);

  void deleteByDataset(@Param("uuid") UUID datasetKey);

  void deleteByDatasetAndDate(@Param("uuid") UUID datasetKey, @Param("before") Date before);
}
