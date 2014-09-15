package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for Identifier.
 * It does not extend the NameUsageComponentMapper cause nub and checklist usages are treated the same
 * for identifiers, i.e. all nub identifiers hang of the nub usage directly.
 */
public interface IdentifierMapper {

  Identifier get(@Param("key") int key);

  List<Identifier> listByUsage(@Param("key") int usageKey, @Param("page") Pageable page);

  void deleteByUsage(@Param("key") int usageKey);

  void insert(@Param("key") int usageKey, @Param("obj") Identifier identifier);
}
