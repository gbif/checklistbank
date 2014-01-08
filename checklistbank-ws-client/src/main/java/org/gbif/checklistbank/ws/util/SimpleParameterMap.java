package org.gbif.checklistbank.ws.util;

import com.google.common.base.Strings;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Builder class for a request parameter map using single values only.
 */
public class SimpleParameterMap extends MultivaluedMapImpl {

  /**
   * Sets a parameter of the map if value is not null or empty.
   *
   * @param parameter to set
   * @param value     value to set
   * @return the current parameter map to support builder pattern
   */
  public SimpleParameterMap param(String parameter, String value) {
    if (!Strings.isNullOrEmpty(value)) {
      putSingle(parameter, value);
    }
    return this;
  }
}
