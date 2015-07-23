package org.gbif.checklistbank.neo;

/**
 * Exception to be thrown when a unique key is expected but wasn't given.
 */
public class NotUniqueException extends Exception {
    private final String key;

    public NotUniqueException(String key, String message) {
        super(message);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
