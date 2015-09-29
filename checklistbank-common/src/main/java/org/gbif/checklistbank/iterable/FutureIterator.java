package org.gbif.checklistbank.iterable;

import java.util.Iterator;
import java.util.concurrent.Future;

/**
 * A simple wrapper for a list of futures providing an iterator for their results.
 */
public class FutureIterator<T> implements Iterator<T> {
    private final Iterator<Future<T>> iter;

    public FutureIterator(Iterable<Future<T>> futures) {
        this.iter = futures.iterator();
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public T next() {
        Future<T> f = iter.next();
        try {
            return f.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove() {
        iter.remove();
    }
}
