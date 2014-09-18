package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.mybatis.mapper.DatasetMetricsMapper;

import java.util.UUID;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetAnalysisServiceMyBatis implements DatasetAnalysisService {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetAnalysisServiceMyBatis.class);
  private final DatasetMetricsMapper mapper;

  @Inject
  DatasetAnalysisServiceMyBatis(DatasetMetricsMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public DatasetMetrics analyse(UUID datasetKey) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

}
