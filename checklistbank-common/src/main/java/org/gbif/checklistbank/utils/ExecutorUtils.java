package org.gbif.checklistbank.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ExecutorUtils {
  private static final Logger LOG = LoggerFactory.getLogger(ExecutorUtils.class);

  public static void stop(ExecutorService exec, String name, int time, TimeUnit unit) {
    LOG.info("Shutting down executor service {}...", name);
    exec.shutdown(); // Disable new tasks from being submitted
    try {
      exec.awaitTermination(time, unit);
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      LOG.warn("Forcing shut down of executor service {}, pending tasks will be lost!", name);
      exec.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }
}
