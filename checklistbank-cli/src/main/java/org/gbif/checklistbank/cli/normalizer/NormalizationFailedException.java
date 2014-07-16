package org.gbif.checklistbank.cli.normalizer;

/**
 *
 */
public class NormalizationFailedException extends Exception {

    public NormalizationFailedException(String message) {
        super(message);
    }

    public NormalizationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
