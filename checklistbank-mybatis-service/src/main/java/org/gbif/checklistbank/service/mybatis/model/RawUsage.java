package org.gbif.checklistbank.service.mybatis.model;

import java.util.Date;
import java.util.UUID;

/**
 * Different model for a verbatim name usage record as it is stored in postgres and used in the mybatis DAO layer.
 */
public class RawUsage {
  private Integer usageKey;
  private UUID datasetKey;
  private byte[] data;
  private Date lastCrawled;

  public Integer getUsageKey() {
    return usageKey;
  }

  public void setUsageKey(Integer usageKey) {
    this.usageKey = usageKey;
  }

  public UUID getDatasetKey() {
    return datasetKey;
  }

  public void setDatasetKey(UUID datasetKey) {
    this.datasetKey = datasetKey;
  }

  public Date getLastCrawled() {
    return lastCrawled;
  }

  public void setLastCrawled(Date lastCrawled) {
    this.lastCrawled = lastCrawled;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }
}
