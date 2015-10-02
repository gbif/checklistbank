package org.gbif.checklistbank.iterable;

import java.util.Iterator;
import java.util.List;

public class BatchIterable<E> implements Iterable<List<E>>{
    private final Iterable<E> iterable;
    private int batchSize;

    /**
     * Returns a BatchIterator over the specified collection.
     *
     * @param collection the collection over which to iterate
     * @param batchSize  the maximum size of each batch returned
     */
    public BatchIterable(Iterable<E> collection, int batchSize) {
        iterable = collection;
        this.batchSize = batchSize;
    }

    /**
     * Returns a BatchIterator over the specified iterable.
     * This is a convenience method to simplify the code need to loop over an existing collection.
     *
     * @param collection the collection over which to iterate
     * @param batchSize  the maximum size of each batch returned
     *
     * @return a BatchIterator over the specified collection
     */
    public static <E> BatchIterable<E> batches(Iterable<E> collection, int batchSize) {
        return new BatchIterable<E>(collection, batchSize);
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<List<E>> iterator() {
        return new BatchIterator(iterable.iterator(), batchSize);
    }

}