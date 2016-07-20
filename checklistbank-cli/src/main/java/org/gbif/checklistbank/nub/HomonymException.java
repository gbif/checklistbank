package org.gbif.checklistbank.nub;

/**
 *
 */
public class HomonymException extends Exception {
  private final String name;

  public HomonymException(String name) {
    this.name = name;
  }

  public HomonymException(String message, String name) {
    super(message);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
