package org.gbif.checklistbank.service.mybatis.mapper;

import org.apache.ibatis.session.ResultHandler;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.VernacularName;

import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * The MyBatis mapper interface for VernacularName.
 */
public interface VernacularNameMapper extends NameUsageComponentMapper<VernacularName> {

  /**
   * Retrieves a single vernacular name or none for a given checklist usage and language.
   */
  VernacularName getByChecklistUsage(@Param("key") int usageKey, @Param("lang") String language);

  /**
   * Retrieves a single vernacular name or none for a given nub usage and language.
   */
  VernacularName getByNubUsage(@Param("key") int usageKey, @Param("lang") String language);

  void insert(@Param("key") int usageKey, @Param("obj") VernacularName vernacularName, @Param("sourceKey") Integer sourceKey);


  /**
   * Iterates over all vernacular names of a given dataset and processes them with the supplied handler.
   * This allows a single query to efficiently stream all its values without keeping them in memory.
   *
   * @param handler to process each name usage with
   */
  void processDataset(@Param("uuid") UUID datasetKey, ResultHandler<VernacularName> handler);

}
