package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.checklistbank.model.NameUsageWritable;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for NameUsages.
 *
 * @see org.gbif.api.service.checklistbank.NameUsageService
 */
public interface NameUsageMapper {

  NameUsage get(@Param("key") int key);

  /**
   * A simple paging query for all usages in checklistbank.
   * We only return name usage ids here to avoid extremely heavy operations for the database when the offset gets
   * bigger.
   *
   * @return the name usage ids belonging to the requested page.
   */
  List<Integer> list(@Param("uuid") UUID datasetKey, @Param("page") Pageable page);

  List<NameUsage> listByTaxonId(
    @Param("uuid") UUID datasetKey, @Param("taxonId") String taxonId, @Param("page") Pageable page);

  List<NameUsage> listByCanonicalName(@Param("canonical") String canonicalName, @Param("uuids") UUID[] datasetKey,
    @Param("page") Pageable page);

  List<NameUsage> listRange(@Param("start") int usageKeyStart, @Param("end") int usageKeyEnd);

  List<NameUsage> listRoot(@Param("uuid") UUID datasetKey, @Param("page") Pageable page);

  List<NameUsage> listChildren(@Param("key") int parentKey, @Param("page") Pageable page);

  List<NameUsage> listSynonyms(@Param("key") int usageKey, @Param("page") Pageable page);

  /**
   * List all related name usages that have a given nubKey.
   */
  List<NameUsage> listRelated(@Param("key") int nubKey, @Param("uuids") UUID[] datasetKeys);

  /**
   * @return the maximum usage key used in a dataset
   */
  Integer maxUsageKey(@Param("uuid") UUID datasetKey);

  /**
   * Insert a new name usage, setting lastInterpretedDate to current date and assigning a new usage key.
   * If higher rank keys like kingdomKey are -1 this is interpreted that they should point to the newly inserted record
   * itself, inserting the newly generated usage key for those negative properties.
   * @param usage
   */
  void insert(@Param("u") NameUsageWritable usage);

  /**
   * Updates an existing name usage.
   * See #insert(NameUsageWritable usage) method for details on behavior.
   */
  void update(@Param("u") NameUsageWritable usage);

  void updateForeignKeys(@Param("key") int usageKey,
    @Param("par") Integer parentKey, @Param("ppk") Integer proparteKey, @Param("bas") Integer basionymKey);
}
