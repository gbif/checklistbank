package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.NameUsageMetrics;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface NameUsageMetricsMapper {

  NameUsageMetrics get(@Param("key") int usageKey);

  void insert(@Param("uuid") UUID datasetKey, @Param("m") NameUsageMetrics metrics);

  void update(@Param("m") NameUsageMetrics metrics);
}
