package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.util.UUID;

/**
 * A nub source which is backed by postgres checklistbank usages
 */
public class ClbSource extends NubSource {
    private final ClbConfiguration clb;
    private ClbUsageIteratorNeo iter;

    public ClbSource(ClbConfiguration clb, UUID key, String name) {
        this.key = key;
        this.name = name;
        this.clb = clb;
    }

    @Override
    public CloseableIterable<SrcUsage> usages() {
        return iter;
    }

    @Override
    public void init() {
        try {
            iter = new ClbUsageIteratorNeo(clb, this);
            iter.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
