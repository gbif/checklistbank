package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.NameUsageMediaObject;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for NameUsageMediaObject.
 */
public interface MultimediaMapper extends NameUsageComponentMapper<NameUsageMediaObject> {

  void insert(@Param("key") int usageKey, @Param("obj") NameUsageMediaObject mediaObject, @Param("sourceKey") Integer sourceKey);

}
