package org.gbif.checklistbank.model;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import com.google.common.base.Objects;

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

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RawUsage) {
      RawUsage that = (RawUsage) obj;
      return Objects.equal(this.usageKey, that.usageKey)
             && Objects.equal(this.datasetKey, that.datasetKey)
             && Objects.equal(this.lastCrawled, that.lastCrawled)
             && Arrays.equals(this.data, that.data);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(usageKey, datasetKey, data, lastCrawled);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("usageKey", usageKey)
      .add("datasetKey", datasetKey)
      .add("data", data)
      .add("lastCrawled", lastCrawled)
      .toString();
  }
}
