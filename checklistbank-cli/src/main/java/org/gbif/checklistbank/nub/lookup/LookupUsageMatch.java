package org.gbif.checklistbank.nub.lookup;

import java.util.List;
import java.util.Objects;

/**
 * Simple usage representing the minimal nub usage info needed to match names.
 */
public class LookupUsageMatch {
    private LookupUsage match;
    private List<LookupUsage> alternatives;

    public List<LookupUsage> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<LookupUsage> alternatives) {
        this.alternatives = alternatives;
    }

    public LookupUsage getMatch() {
        return match;
    }

    public void setMatch(LookupUsage match) {
        this.match = match;
    }

    /**
     * @return true if a match was found
     */
    public boolean hasMatch() {
        return match != null;
    }

    /**
     * @return true if there was no match because of multiple, ambigous options
     */
    public boolean isAmbigous() {
        return match != null && alternatives.size() > 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LookupUsageMatch that = (LookupUsageMatch) o;
        return Objects.equals(match, that.match) &&
                Objects.equals(alternatives, that.alternatives);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match, alternatives);
    }
}
