package org.gbif.checklistbank.nub;

/**
 * Exception thrown to indicate to the nub builder that a source usage should be ignored and not be included in the backbone.
 * Please use this exception in exceptional cases only and try to use regular control flows instead.
 */
public class IgnoreSourceUsageException extends RuntimeException {
    public final String name;

    public IgnoreSourceUsageException(String message, String name) {
        super(message);
        this.name = name;
    }
}
