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

  /**
   * Shuts down an executor and waits up to 1 week for the already submitted job to finish.
   * @param exec
   * @param name
   */
  public static void stop(ExecutorService exec, String name) {
    stop(exec, name, 7, TimeUnit.DAYS);
  }

  /**
   * Shuts down an executor and waits for the job to finish until the given timeout is reached
   * before forcing an immediate shutdown.
   * @param exec
   * @param name
   * @param timeout
   * @param unit
   */
  public static void stop(ExecutorService exec, String name, int timeout, TimeUnit unit) {
    LOG.info("Shutting down executor service {}...", name);
    exec.shutdown(); // Disable new tasks from being submitted
    try {
      exec.awaitTermination(timeout, unit);
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      LOG.warn("Forcing shut down of executor service {}, pending tasks will be lost!", name);
      exec.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }
}
