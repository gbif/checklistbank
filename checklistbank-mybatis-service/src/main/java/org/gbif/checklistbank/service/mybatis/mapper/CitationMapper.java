package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.checklistbank.model.Citation;

import org.apache.ibatis.annotations.Param;

public interface CitationMapper {

  String get(@Param("key") int key);

  Integer getByCitation(@Param("citation") String citation);

  void insert(@Param("c") Citation citation);

}
