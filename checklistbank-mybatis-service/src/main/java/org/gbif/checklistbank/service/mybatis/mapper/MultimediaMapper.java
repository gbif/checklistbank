package org.gbif.checklistbank.service.mybatis.mapper;

import org.apache.ibatis.session.ResultHandler;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;

import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * The MyBatis mapper interface for NameUsageMediaObject.
 */
public interface MultimediaMapper extends NameUsageComponentMapper<NameUsageMediaObject> {

  void insert(@Param("key") int usageKey, @Param("obj") NameUsageMediaObject mediaObject, @Param("sourceKey") Integer sourceKey);

}
