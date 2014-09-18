package org.gbif.checklistbank.service;

import org.gbif.api.model.checklistbank.DatasetMetrics;

import java.util.UUID;

public interface DatasetAnalysisService {

  /**
   * Analyses an entire checklist dataset and persists a newly generated DatasetMetrics.
   * @return the newly generated dataset metric
   */
  DatasetMetrics analyse(UUID datasetKey);

}
