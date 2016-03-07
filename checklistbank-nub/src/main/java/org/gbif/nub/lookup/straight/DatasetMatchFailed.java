package org.gbif.nub.lookup.straight;

import java.util.UUID;

/**
 *
 */
public class DatasetMatchFailed extends Exception {
  private final UUID datasetKey;

  public DatasetMatchFailed(UUID datasetKey, Throwable cause) {
    super(cause);
    this.datasetKey = datasetKey;
  }

  public DatasetMatchFailed(UUID datasetKey, String message, Throwable cause) {
    super(message, cause);
    this.datasetKey = datasetKey;
  }
}
