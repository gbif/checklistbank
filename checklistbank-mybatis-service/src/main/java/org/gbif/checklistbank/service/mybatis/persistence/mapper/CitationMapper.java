package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.checklistbank.model.Citation;

import org.apache.ibatis.annotations.Param;

public interface CitationMapper {

  Integer getByCitation(@Param("citation") String citation);

  void insert(@Param("c") Citation citation);

}
