package org.gbif.checklistbank.utils;

import java.io.Closeable;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CloseableUtils {
  private static final Logger LOG = LoggerFactory.getLogger(CloseableUtils.class);


  public static void close(Collection<Closeable> toBeClosed) {
    for (Closeable cl : toBeClosed) {
      try {
        cl.close();
      } catch (Exception e) {
        LOG.error("Failed to close {}", cl.getClass().getSimpleName(), e);
      }
    }
  }

  public static void close(Iterable<AutoCloseable> toBeClosed) {
    for (AutoCloseable cl : toBeClosed) {
      try {
        cl.close();
      } catch (Exception e) {
        LOG.error("Failed to close {}", cl.getClass().getSimpleName(), e);
      }
    }
  }
}
