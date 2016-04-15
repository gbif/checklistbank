package org.gbif.checklistbank.nub.source;

import java.util.concurrent.Callable;

/**
 * Job that initializes a NubSource
 */
class LoadSource implements Callable<NubSource> {
    private final NubSource src;
    private final boolean parseNames;

  /**
   * @param parseNames if true parse names and populate SrcUsage.parsedName which will be null otherwise!
   */
  public LoadSource(NubSource src, boolean parseNames) {
        this.src = src;
      this.parseNames = parseNames;
    }

    @Override
    public NubSource call() throws Exception {
        src.init(false, false, parseNames);
        return src;
    }
}