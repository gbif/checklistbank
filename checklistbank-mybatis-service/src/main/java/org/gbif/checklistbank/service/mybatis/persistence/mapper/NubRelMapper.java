package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;
import org.gbif.checklistbank.model.NubMapping;

public interface NubRelMapper {

  void insert(@Param("uuid") UUID datasetKey, @Param("usageKey") int usageKey, @Param("nubKey") int nubKey);

  void delete(@Param("usageKey") int usageKey);

  void deleteByDataset(@Param("uuid") UUID datasetKey);

  Cursor<NubMapping> process(@Param("uuid") UUID datasetKey);
}
