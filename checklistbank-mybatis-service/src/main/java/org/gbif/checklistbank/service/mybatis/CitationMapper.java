package org.gbif.checklistbank.service.mybatis;

import org.apache.ibatis.annotations.Param;

public interface CitationMapper {

  String get(@Param("key") int key);

  Integer getByCitation(@Param("citation") String citation);

  int create(@Param("citation") String citation);

}
