package org.gbif.checklistbank.nub.model;

public class NubUsageMatch {
  public final NubUsage usage;
  public final boolean ignore;
  public final NubUsage doubtfulUsage;

  private NubUsageMatch(NubUsage usage, boolean ignore, NubUsage doubtfulUsage) {
    this.usage = usage;
    this.ignore = ignore;
    this.doubtfulUsage = doubtfulUsage;
  }

  public static NubUsageMatch match(NubUsage usage) {
    return new NubUsageMatch(usage, false, null);
  }

  /**
   * Snaps to a usage but flag it to be ignored in immediate processing.
   */
  public static NubUsageMatch snap(NubUsage usage) {
    return new NubUsageMatch(usage, true, null);
  }

  public static NubUsageMatch empty() {
    return new NubUsageMatch(null, false, null);
  }

  public static NubUsageMatch empty(NubUsage doubtfulUsage) {
    return new NubUsageMatch(null, false, doubtfulUsage);
  }

  public boolean isMatch() {
    return usage != null;
  }
}
