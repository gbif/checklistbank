package org.gbif.checklistbank.logging;

import java.util.UUID;

import org.slf4j.MDC;

/**
 *
 */
public class LogContext {

  public static final String DATASET_MDC = "dataset";

  public static void startDataset(UUID key) {
    MDC.put(DATASET_MDC, key.toString());
  }

  public static void endDataset() {
    MDC.remove(DATASET_MDC);
  }

}
