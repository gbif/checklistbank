package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.util.Date;
import java.util.UUID;

/**
 * A backbone source dataset with some basic metadata that allows to iteratore over its source usages.
 */
public abstract class NubSource {
    public UUID key;
    public String name;
    public int priority = 0;
    public Rank ignoreRanksAbove = Rank.FAMILY;
    public Date created;
    public boolean nomenclator = false;

    /**
     * Returns a neo db backed iterable that can be used to iterate over all usages in the source multiple times.
     * The iteration is in taxonomic order, starting with the highest root taxa and walks
     * the taxonomic tree in depth order first, including synonyms.
     */
    public abstract CloseableIterable<SrcUsage> usages();

    /**
     * Loads data into the source and does any other initialization needed before usages() can be called.
     * Make sure to call this method once before the usage iterator is used!
     */
    public abstract void init();
}
