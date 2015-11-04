package org.gbif.checklistbank.service.mybatis.mapper;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface DatasetMapper {

    String get(@Param("uuid") UUID datasetKey);

    void insert(@Param("uuid") UUID datasetKey, @Param("title") String title);

    void update(@Param("uuid") UUID datasetKey, @Param("title") String title);

    void delete(@Param("uuid") UUID datasetKey);

    void truncate();

}
