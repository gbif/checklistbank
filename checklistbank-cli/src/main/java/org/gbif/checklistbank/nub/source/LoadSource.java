package org.gbif.checklistbank.nub.source;

import java.util.concurrent.Callable;

/**
 * Job that initializes a NubSource
 */
class LoadSource implements Callable<NubSource> {
    private final NubSource src;

    public LoadSource(NubSource src) {
        this.src = src;
    }

    @Override
    public NubSource call() throws Exception {
        src.init(false, false);
        return src;
    }
}