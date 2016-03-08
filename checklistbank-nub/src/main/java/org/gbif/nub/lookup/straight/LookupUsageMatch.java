package org.gbif.nub.lookup.straight;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Simple usage representing the minimal nub usage info needed to match names.
 */
public class LookupUsageMatch {
    private LookupUsage match;
    private List<LookupUsage> alternatives = Lists.newArrayList();

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
    @JsonIgnore
    public boolean hasMatch() {
        return match != null;
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
