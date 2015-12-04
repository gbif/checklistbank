package org.gbif.checklistbank.cli.show;

/**
 *
 */
public enum GraphFormat {
  /**
   * Taxonomic tree as nested text rows
   */
  TEXT("txt"),

  /**
   * Taxonomic tree as xml
   */
  XML("xml"),

  /**
   * Graph modelling language
   */
  GML("gml"),

  /**
   * Tab delimited format used for nub sources in integration tests
   */
  TAB("tab");

  public final String suffix;

  GraphFormat(String suffix) {
    this.suffix = suffix;
  }
}
