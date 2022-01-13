/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.cli.model;

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
  TAB("tab"),

  /**
   * Denormalised list of genus or infrageneric names. Not tree format really.
   */
  LIST("txt");

  public final String suffix;

  GraphFormat(String suffix) {
    this.suffix = suffix;
  }
}
