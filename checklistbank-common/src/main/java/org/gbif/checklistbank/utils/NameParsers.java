package org.gbif.checklistbank.utils;

import org.gbif.nameparser.NameParserGbifV1;

public class NameParsers {
  public static final NameParserGbifV1 INSTANCE = new NameParserGbifV1(20000);

  private NameParsers() {};
}
