package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.TypeSpecimen;

import org.apache.ibatis.annotations.Param;

/**
 * The MyBatis mapper interface for TypeSpecimen.
 */
public interface TypeSpecimenMapper extends NameUsageComponentMapper<TypeSpecimen> {

  void insert(@Param("key") int usageKey, @Param("obj") TypeSpecimen typeSpecimen, @Param("sourceKey") Integer sourceKey);

}
