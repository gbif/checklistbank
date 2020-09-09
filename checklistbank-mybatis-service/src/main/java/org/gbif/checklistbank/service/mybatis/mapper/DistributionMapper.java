package org.gbif.checklistbank.service.mybatis.mapper;

import org.apache.ibatis.session.ResultHandler;
import org.gbif.api.model.checklistbank.Distribution;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.Country;
import org.gbif.checklistbank.model.ParsedNameUsage;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The MyBatis mapper interface for Distribution.
 */
public interface DistributionMapper extends NameUsageComponentMapper<Distribution> {

  void insert(@Param("key") int usageKey, @Param("obj") Distribution distribution, @Param("sourceKey") Integer sourceKey);

  List<Distribution> listByNubUsageAndCountry(@Param("key") int usageKey, @Param("country") Country country, @Param("page") Pageable page);

  List<Distribution> listByChecklistUsageAndCountry(@Param("key") int usageKey, @Param("country") Country country, @Param("page") Pageable page);

}
