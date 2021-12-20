package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.checklistbank.service.mybatis.mapper.DatasetMetricsMapper;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements the NameUsageService using MyBatis.
 * All PagingResponses will not have the count set as it can be too costly sometimes.
 */
public class DatasetMetricsServiceMyBatis implements DatasetMetricsService {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetMetricsServiceMyBatis.class);
  private final DatasetMetricsMapper mapper;

  @Autowired
  DatasetMetricsServiceMyBatis(DatasetMetricsMapper mapper) {
    this.mapper = mapper;
  }

  public static class Count<T> implements Comparable<Count<T>>{
    private T key;
    private Integer count;

    public Count() {
    }

    public Count(T key, Integer count) {
      this.key = key;
      this.count = count;
    }

    public T getKey() {
      return key;
    }

    public void setKey(T key) {
      this.key = key;
    }

    public Integer getCount() {
      return count;
    }

    public void setCount(Integer count) {
      this.count = count;
    }

    @Override
    public int compareTo(Count<T> that) {
      return ComparisonChain.start()
               .compare(this.count, that.count, Ordering.natural().reverse().nullsLast())
               .compare(this.key.toString(), that.key.toString())
               .result();
    }
  }

  /**
   * Returns the latest metric for the given dataset.
   */
  @Override
  public DatasetMetrics get(UUID datasetKey) {
    return mapper.get(datasetKey);
  }

  /**
   * Method that calculates all dataset metrics and then persists them as a new dataset_metrics record.
   * @param datasetKey
   * @param downloaded the date the dataset was last downloaded from the publisher
   */
  public DatasetMetrics create(UUID datasetKey, Date downloaded) {
    LOG.info("Create new dataset metrics");
    mapper.insert(datasetKey, downloaded);
    return get(datasetKey);
  }

  @Override
  public List<DatasetMetrics> list(UUID datasetKey) {
    return mapper.list(datasetKey);
  }

  /**
   * @return The percentage of total that count covers, or 0 should the total be 0
   */
  protected static int getPercentage(int count, int total) {
    return total > 0 ? count * 100 / total : 0;
  }

}
