package org.gbif.checklistbank.nub.model;

import org.gbif.checklistbank.nub.model.NubUsage;

/**
 * Created by markus on 06/10/15.
 */
class NubUsageMatch {
    public final NubUsage usage;
    public final boolean avoidUpdate;

    public NubUsageMatch(NubUsage usage) {
        this.usage = usage;
        this.avoidUpdate = false;
    }

    public NubUsageMatch(NubUsage usage, boolean avoidUpdate) {
        this.usage = usage;
        this.avoidUpdate = avoidUpdate;
    }

    public boolean isMatch() {
        return usage != null;
    }
}
