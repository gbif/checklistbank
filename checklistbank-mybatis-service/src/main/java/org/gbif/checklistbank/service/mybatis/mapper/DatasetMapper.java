package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.checklistbank.model.DatasetCore;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface DatasetMapper {

  DatasetCore get(@Param("uuid") UUID datasetKey);

  void insert(@Param("d") DatasetCore dataset);

  void update(@Param("d") DatasetCore dataset);

  void delete(@Param("uuid") UUID datasetKey);

  void truncate();

}
