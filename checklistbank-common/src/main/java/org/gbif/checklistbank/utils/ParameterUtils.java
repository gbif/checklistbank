package org.gbif.checklistbank.utils;

import org.apache.commons.lang3.StringUtils;

public class ParameterUtils {

  /**
   * @return the first non blank value
   */
  public static String first(String... values){
    if (values != null) {
      for (String val : values) {
        if (!StringUtils.isBlank(val)) {
          return val;
        }
      }
    }
    return null;
  }
}
