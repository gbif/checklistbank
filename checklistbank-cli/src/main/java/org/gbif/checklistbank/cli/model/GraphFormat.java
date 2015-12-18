package org.gbif.checklistbank.cli.model;

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
   * See http://www.fim.uni-passau.de/fileadmin/files/lehrstuhl/brandenburg/projekte/gml/gml-technical-report.pdf
   */
  GML("gml"),

  /**
   * http://www.graphviz.org/doc/info/lang.html
   */
  DOT("dot"),

  /**
   * Tab delimited format used for nub sources in integration tests
   */
  TAB("tab");

  public final String suffix;

  GraphFormat(String suffix) {
    this.suffix = suffix;
  }
}
