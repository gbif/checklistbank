package org.gbif.checklistbank.nub.model;

public class NubUsageMatch {
    public final NubUsage usage;
    public final boolean ignore;

    private NubUsageMatch(NubUsage usage, boolean ignore) {
        this.usage = usage;
        this.ignore = ignore;
    }

    public static NubUsageMatch match(NubUsage usage) {
        return new NubUsageMatch(usage, false);
    }

    /**
     * Snaps to a usage but flag it to be ignored in immediate processing.
     */
    public static NubUsageMatch snap(NubUsage usage) {
        return new NubUsageMatch(usage, true);
    }

    public static NubUsageMatch empty() {
        return new NubUsageMatch(null, false);
    }

    public boolean isMatch() {
        return usage != null;
    }
}
