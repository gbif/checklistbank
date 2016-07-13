package org.gbif.checklistbank.model;

public enum Equality {
  EQUAL, DIFFERENT, UNKNOWN;

  public Equality and (Equality other) {
    switch (this) {
      case UNKNOWN:
        return other;
      case DIFFERENT:
        return DIFFERENT;
      case EQUAL:
        return other == DIFFERENT ? DIFFERENT : EQUAL;
      default:
        throw new IllegalStateException();
    }
  }
}
