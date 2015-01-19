package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.checklistbank.model.RawUsage;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for RawUsage.
 */
public interface RawUsageMapper {

  /**
   * @param key usage key
   */
  RawUsage get(@Param("key") int key);

  void insert(@Param("r") RawUsage usage);

  void update(@Param("r") RawUsage usage);

  /**
   * @param key usage key
   */
  void delete(@Param("key") int key);
}
