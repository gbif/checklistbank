package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.checklistbank.model.ColAnnotation;

import org.apache.ibatis.annotations.Param;

public interface ColAnnotationMapper {

  void delete(@Param("key") int taxonKey);

  void create(@Param("anno") ColAnnotation annotation);

}
