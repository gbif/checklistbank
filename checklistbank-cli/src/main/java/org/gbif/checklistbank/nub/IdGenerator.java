package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.nub.lookup.IdLookup;
import org.gbif.checklistbank.nub.lookup.LookupUsage;

public class IdGenerator {
    private IdLookup lookup;
    private int nextId;

    /**
     * load highest nub id from clb:
     * nextId = mapper.maxUsageKey(Constants.NUB_DATASET_KEY) + 1;
     */
    public IdGenerator(IdLookup lookup, int idStart) {
        this.lookup = lookup;
        nextId = idStart;
    }

    public int assignId(String canonicalName, String authorship, Rank rank, Kingdom kingdom, TaxonomicStatus status) {
        LookupUsage u = lookup.match(canonicalName, authorship, rank, kingdom, status);
        return u == null ? nextId++ : u.getKey();
    }
}
