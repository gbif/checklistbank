package org.gbif.checklistbank.utils;

public class HumanSize {
  private static final int unit = 1024;

  public static String bytes(long bytes) {
    if (bytes < unit) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    char pre = "kMGTPE".charAt(exp-1);
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }
}
