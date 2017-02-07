package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.checklistbank.model.ParsedNameUsage;
import org.gbif.checklistbank.model.ScientificName;
import org.gbif.checklistbank.model.NameUsageWritable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

/**
 * The MyBatis mapper interface for NameUsages.
 *
 * @see org.gbif.api.service.checklistbank.NameUsageService
 */
public interface NameUsageMapper {

  NameUsage get(@Param("key") int key);

  /**
   * Returns the existing usage key for a given taxonID in a dataset or null if its not existing.
   * In case more than one usage exists with the given taxonID (which should never happen, these are invalid datasets
   * with non unique keys) the first key is selected.
   */
  Integer getKey(@Param("uuid") UUID datasetKey, @Param("taxonId") String taxonId);

  /**
   * A simple paging query for all non deleted usages in checklistbank.
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


  List<NameUsage> listRoot(@Param("uuid") UUID datasetKey, @Param("page") Pageable page);

  List<NameUsage> listChildren(@Param("key") int parentKey, @Param("page") Pageable page);

  List<NameUsage> listSynonyms(@Param("key") int usageKey, @Param("page") Pageable page);

  List<NameUsage> listCombinations(@Param("key") int basionymKey);

  List<NameUsage> listRange(@Param("start") int usageKeyStart, @Param("end") int usageKeyEnd);

  /**
   * Iterates over all name usages of a given dataset and processes them with the supplied handler.
   * This allows a single query to efficiently stream all its values without keeping them in memory.
   *
   * @param handler to process each name usage with
   */
  void processDataset(@Param("uuid") UUID datasetKey, ResultHandler<ParsedNameUsage> handler);

  /**
   * Iterates over all usage names and processes them with the supplied handler.
   * This allows a single query to efficiently stream all its values without keeping them in memory.
   *
   * @param handler to process each name with
   */
  void processAllNames(ResultHandler<ScientificName> handler);

  /**
   * List all related name usages that have a given nubKey.
   */
  List<NameUsage> listRelated(@Param("key") int nubKey, @Param("uuids") UUID[] datasetKeys, @Param("page") Pageable page);

  /**
   * @return the maximum usage key used in a dataset incl deleted records
   */
  Integer maxUsageKey(@Param("uuid") UUID datasetKey);

  /**
   * Insert a new name usage, setting lastInterpretedDate to current date and assigning a new usage key.
   * If higher rank keys like kingdomKey are -1 this is interpreted that they should point to the newly inserted record
   * itself, inserting the newly generated usage key for those negative properties.
   */
  void insert(@Param("u") NameUsageWritable usage);

  /**
   * Updates an existing name usage.
   * See #insert(NameUsageWritable usage) method for details on behavior.
   */
  void update(@Param("u") NameUsageWritable usage);

  void updateName(@Param("key") int usageKey, @Param("nkey") int nameKey);

  void updateForeignKeys(@Param("key") int usageKey, @Param("par") Integer parentKey, @Param("bas") Integer basionymKey);

  /**
   * @return the set of issues associated with the usage
   */
  NameUsage getIssues(@Param("key") int usageKey);

  /**
   * Update the name usage issues with the given set
   */
  void updateIssues(@Param("key") int usageKey, @Param("issues") Set<NameUsageIssue> issues);

}
