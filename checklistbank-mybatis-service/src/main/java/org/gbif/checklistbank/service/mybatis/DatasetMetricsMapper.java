package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.DatasetMetrics;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for DatasetMetrics.
 */
public interface DatasetMetricsMapper {

  DatasetMetrics get(@Param("uuid") UUID datasetKey);

  List<DatasetMetrics> list(@Param("uuid") UUID datasetKey);

  DatasetMetrics insert(@Param("uuid") UUID datasetKey);

}
