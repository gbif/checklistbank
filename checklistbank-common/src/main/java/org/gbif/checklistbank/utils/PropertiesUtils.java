package org.gbif.checklistbank.utils;

import java.util.Properties;

/**
 *
 */
public class PropertiesUtils {
  public static int getIntProp(Properties properties, String propName, int defaultVal) {
    try {
      return Integer.valueOf(properties.getProperty(propName, "x"));
    } catch (NumberFormatException e) {
      return defaultVal;
    }
  }
}
