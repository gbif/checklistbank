package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.NameUsageMetrics;

import org.apache.ibatis.annotations.Param;

public interface NameUsageMetricsMapper {

  NameUsageMetrics get(@Param("key") int usageKey);

}
