package org.gbif.checklistbank.iterable;

import java.util.Iterator;

public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {

}
