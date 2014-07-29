package org.gbif.checklistbank.service.mybatis;

import org.gbif.checklistbank.service.mybatis.model.RawUsage;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for RawUsage.
 */
public interface RawUsageMapper {

  RawUsage get(@Param("key") int key);

  void insert(@Param("r") RawUsage usage);

  void update(@Param("r") RawUsage usage);
}
