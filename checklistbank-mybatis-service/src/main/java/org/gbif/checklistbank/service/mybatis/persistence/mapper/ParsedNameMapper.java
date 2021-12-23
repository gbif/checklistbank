package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.Rank;

import java.util.List;
import java.util.UUID;

/**
 * The MyBatis mapper interface for ParsedName instance to persist in the name table.
 * ParsedName.strain property is being ignored currently and not persisted!
 */
public interface ParsedNameMapper {

  /**
   * Get a parsed name by its name key.
   *
   * @param key the key of the parsed name
   * @return the parsed name or null
   */
  ParsedName get(@Param("key") int key);

  /**
   * Get a parsed name by a given name usage key.
   *
   * @param usageKey the key of the parsed name
   * @return the parsed name or null
   */
  ParsedName getByUsageKey(@Param("key") int usageKey);

  ParsedName getByName(@Param("name") String scientificName, @Param("rank") Rank rank);

  /**
   * Insert a new parsed name into the name table.
   */
  void create(@Param("pn") ParsedName name);

  void createWithKey(@Param("key") int key, @Param("pn") ParsedName name);

  void failed(@Param("key") int key, @Param("scientific_name") String scientificName, @Param("rank") Rank rank);

  void delete(@Param("key") int key);

  int deleteOrphaned(@Param("keyMin") int keyMin, @Param("keyMax") int keyMax);

  /**
   * @return the maximum name key used
   */
  Integer maxKey();

  List<ParsedName> list(@Param("uuid") UUID datasetKey, @Param("page") Pageable page);

  /**
   * Iterates over all names and processes them with the supplied handler.
   * This allows a single query to efficiently stream all its values without keepong them in memory.
   *
   * @param handler to process each name with
   */
  void processNames(ResultHandler<ParsedName> handler);

  void update(@Param("pn") ParsedName name);
}
