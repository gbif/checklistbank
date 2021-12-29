/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nub.lookup.straight;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

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
