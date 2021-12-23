package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.checklistbank.model.TocEntry;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for Description.
 */
public interface DescriptionMapper extends NameUsageComponentMapper<Description> {

  Description get(@Param("key") int key);

  List<TocEntry> listTocEntriesByNub(@Param("usageKey") int usageKey);

  List<TocEntry> listTocEntriesByUsage(@Param("usageKey") int usageKey);

  void insert(@Param("key") int usageKey, @Param("obj") Description description, @Param("sourceKey") Integer sourceKey);
}
