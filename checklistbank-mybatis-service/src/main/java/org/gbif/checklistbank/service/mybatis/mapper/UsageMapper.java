package org.gbif.checklistbank.service.mybatis.mapper;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface UsageMapper {

  void delete(@Param("key") int key);

  void deleteLogically(@Param("key") int key);

  int deleteByDataset(@Param("uuid") UUID datasetKey);

  List<Integer> listByDatasetAndDate(@Param("uuid") UUID datasetKey, @Param("before") Date before);

  /**
   * Return ids of all parents, limited to max 100 to avoid endless loops that bring down the JVM
   * as seen during CoL solr index build
   */
  List<Integer> listParents(@Param("key") int usageKey);

}
