package org.gbif.checklistbank.model;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;

/**
 * Bundles a parsed name instance with a name usage instance when both are needed.
 */
public class ParsedNameUsageCompound {
  public final NameUsage usage;
  public final ParsedName parsedName;

  public ParsedNameUsageCompound(NameUsage usage, ParsedName parsedName) {
    this.usage = usage;
    this.parsedName = parsedName;
  }
}
