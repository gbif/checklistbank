package org.gbif.checklistbank.nub.model;

public class NubUsageMatch {
    public final NubUsage usage;
    public final boolean ignore;

    private NubUsageMatch(NubUsage usage, boolean ignore, boolean homonym) {
        this.usage = usage;
        this.ignore = ignore;
    }

    public static NubUsageMatch match(NubUsage usage) {
        return new NubUsageMatch(usage, false, false);
    }

    /**
     * Snaps to a usage but flag it to be ignored in immediate processing.
     */
    public static NubUsageMatch snap(NubUsage usage, boolean homonym) {
        return new NubUsageMatch(usage, true, homonym);
    }

    public static NubUsageMatch empty() {
        return new NubUsageMatch(null, false, false);
    }

    public static NubUsageMatch homonym() {
        return new NubUsageMatch(null, false, true);
    }

    public boolean isMatch() {
        return usage != null;
    }
}
