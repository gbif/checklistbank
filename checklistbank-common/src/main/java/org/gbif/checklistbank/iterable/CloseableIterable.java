package org.gbif.checklistbank.iterable;

public interface CloseableIterable<T> extends Iterable<T>, AutoCloseable {

}
