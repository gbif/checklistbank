package org.gbif.checklistbank.iterable;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.collect.Lists;

/**
 * Created by markus on 02/10/15.
 */
class BatchIterator<E> implements Iterator<List<E>> {
    private final Iterator<E> iter;
    private int batchSize;

    public BatchIterator(Iterator<E> iter, int batchSize) {
        this.iter = iter;
        this.batchSize = batchSize;
    }

    /* (non-Javadoc)
 * @see java.util.Iterator#hasNext()
 */
    public boolean hasNext() {
        return iter.hasNext();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public List<E> next() {
        if (!iter.hasNext()) {
            throw new NoSuchElementException();
        }

        List<E> batch = Lists.newArrayList();
        while (batch.size() < batchSize && iter.hasNext()) {
            batch.add(iter.next());
        }
        return batch;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
