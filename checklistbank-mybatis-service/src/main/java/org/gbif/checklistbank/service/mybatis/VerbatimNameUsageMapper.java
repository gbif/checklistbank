package org.gbif.checklistbank.service.mybatis;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for VerbatimNameUsage.
 */
public interface VerbatimNameUsageMapper {

  String get(@Param("key") int key);

}
