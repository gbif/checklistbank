package org.gbif.checklistbank.model;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.checklistbank.ParsedName;

/**
 * Simple wrapper for a parsed name & usage match.
 */
public class ParsedNameUsageMatch {
  public ParsedName pn;
  public NameUsageMatch u;

  public ParsedNameUsageMatch() {
  }

  public ParsedNameUsageMatch(ParsedName pn, NameUsageMatch u) {
    this.pn = pn;
    this.u = u;
  }
}
