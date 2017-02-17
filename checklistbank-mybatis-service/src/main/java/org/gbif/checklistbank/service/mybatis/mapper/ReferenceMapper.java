package org.gbif.checklistbank.service.mybatis.mapper;

import org.apache.ibatis.session.ResultHandler;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.Reference;

import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * The MyBatis mapper interface for Reference.
 */
public interface ReferenceMapper extends NameUsageComponentMapper<Reference> {

  void insert(@Param("key") int usageKey, @Param("citationKey") int citationKey, @Param("obj") Reference reference);


  /**
   * Iterates over all references of a given dataset and processes them with the supplied handler.
   * This allows a single query to efficiently stream all its values without keeping them in memory.
   *
   * @param handler to process each name usage with
   */
  void processDataset(@Param("uuid") UUID datasetKey, ResultHandler<Reference> handler);

}
