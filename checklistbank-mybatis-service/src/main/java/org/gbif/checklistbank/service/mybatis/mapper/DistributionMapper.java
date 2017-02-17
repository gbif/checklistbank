package org.gbif.checklistbank.service.mybatis.mapper;

import org.apache.ibatis.session.ResultHandler;
import org.gbif.api.model.checklistbank.Distribution;

import org.apache.ibatis.annotations.Param;
import org.gbif.checklistbank.model.ParsedNameUsage;

import java.util.UUID;

/**
 * The MyBatis mapper interface for Distribution.
 */
public interface DistributionMapper extends NameUsageComponentMapper<Distribution> {

  void insert(@Param("key") int usageKey, @Param("obj") Distribution distribution, @Param("sourceKey") Integer sourceKey);

  /**
   * Iterates over all distributions of a given dataset and processes them with the supplied handler.
   * This allows a single query to efficiently stream all its values without keeping them in memory.
   *
   * @param handler to process each name usage with
   */
  void processDataset(@Param("uuid") UUID datasetKey, ResultHandler<Distribution> handler);

}
