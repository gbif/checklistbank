package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.Reference;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for Reference.
 */
public interface ReferenceMapper extends NameUsageComponentMapper<Reference> {

  void insert(@Param("key") int usageKey, @Param("citationKey") int citationKey, @Param("obj") Reference reference);

}
