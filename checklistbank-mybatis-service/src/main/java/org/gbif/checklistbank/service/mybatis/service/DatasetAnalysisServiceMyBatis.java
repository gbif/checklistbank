package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.DatasetMetricsMapper;

import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DatasetAnalysisServiceMyBatis implements DatasetAnalysisService {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetAnalysisServiceMyBatis.class);
  private final DatasetMetricsMapper mapper;

  @Autowired
  DatasetAnalysisServiceMyBatis(DatasetMetricsMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public DatasetMetrics analyse(UUID datasetKey, Date downloaded) {
    LOG.info("Create new metrics for dataset {}", datasetKey);
    mapper.insert(datasetKey, downloaded);
    return mapper.get(datasetKey);
  }
}
