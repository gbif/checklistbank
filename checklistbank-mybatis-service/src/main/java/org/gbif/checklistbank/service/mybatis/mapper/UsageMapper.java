package org.gbif.checklistbank.service.mybatis.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.UUID;

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

  /**
   * Update a backbone name usage with the given source taxon key
   */
  void updateSourceTaxonKey(@Param("key") int nubKey, @Param("sourceTaxonKey") int sourceTaxonKey);

  /**
   * Sets sourceTaxonKey to null for all backbone name usages from a given constituent
   */
  void deleteSourceTaxonKeyByConstituent(@Param("uuid") UUID constituentKey);

}
