package org.gbif.checklistbank.neo;

/**
 * Exception to be thrown when a unique key is expected but wasn't given.
 */
public class NotUniqueRuntimeException extends RuntimeException {
    private final String property;
    private final Object key;

    public NotUniqueRuntimeException(String property, Object key) {
        super(property + " not unique: " + key);
        this.property = property;
        this.key = key;
    }

    public String getProperty() {
        return property;
    }

    public Object getKey() {
        return key;
    }
}
