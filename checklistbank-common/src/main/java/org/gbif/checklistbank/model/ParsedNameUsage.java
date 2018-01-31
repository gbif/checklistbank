package org.gbif.checklistbank.model;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;

/**
 * Adds a parsed name instance to the name usage class for cases when both are needed.
 * Uses inheritance so mappers can return this easily.
 */
public class ParsedNameUsage extends NameUsage {
  private ParsedName parsedName;

  public ParsedName getParsedName() {
    return parsedName;
  }

  public void setParsedName(ParsedName parsedName) {
    this.parsedName = parsedName;
  }
}
