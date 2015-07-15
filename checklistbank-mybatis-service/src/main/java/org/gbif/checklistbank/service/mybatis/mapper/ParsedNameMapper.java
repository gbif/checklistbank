package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for ParsedName instance to persist in the name table.
 * ParsedName.strain property is being ignored currently and not persisted!
 */
public interface ParsedNameMapper {

  /**
   * Get a parsed name by its name key.
   * @param key the key of the parsed name
   * @return the parsed name or null
   */
  ParsedName get(@Param("key") int key);

  /**
   * Get a parsed name by a given name usage key.
   * @param usageKey the key of the parsed name
   * @return the parsed name or null
   */
  ParsedName getByUsageKey(@Param("key") int usageKey);

  ParsedName getByName(@Param("name") String scientificName);

  /**
   * Insert a new parsed name into the name_string table.
   */
  void create(@Param("pn") ParsedName name, @Param("canonicalName") String canonicalName);

  void delete(@Param("key") int key);

  List<ParsedName> list(@Param("uuid") UUID datasetKey, @Param("page") Pageable page);

}
