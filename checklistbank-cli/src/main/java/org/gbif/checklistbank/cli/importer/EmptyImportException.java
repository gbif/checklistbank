package org.gbif.checklistbank.cli.importer;

import java.util.UUID;

/**
 * Exception thrown if no records could be imported.
 * Either because there were none in the source or because syncing errors happened.
 */
public class EmptyImportException extends IllegalStateException {
    public final UUID datasetKey;

    public EmptyImportException(UUID datsetKey, String msg) {
        super(msg);
        this.datasetKey = datsetKey;
    }
}
