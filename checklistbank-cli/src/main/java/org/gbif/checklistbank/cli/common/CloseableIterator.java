package org.gbif.checklistbank.cli.common;

import java.util.Iterator;

public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {

}
