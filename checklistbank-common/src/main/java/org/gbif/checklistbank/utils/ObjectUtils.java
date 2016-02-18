package org.gbif.checklistbank.utils;

/**
 *
 */
public class ObjectUtils {

  private ObjectUtils() {

  }

  public static <T> T coalesce(T ... items) {
    if (items != null) {
      for (T i : items) if (i != null) return i;
    }
    return null;
  }

  public static <T> T coalesce(Iterable<T> items) {
    if (items != null) {
      for (T i : items) if (i != null) return i;
    }
    return null;
  }
}
