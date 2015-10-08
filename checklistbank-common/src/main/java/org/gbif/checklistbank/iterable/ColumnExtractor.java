package org.gbif.checklistbank.iterable;

import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Wrapper class that extracts a single column from an underlying string iterator
 */
public class ColumnExtractor implements Iterator<String> {
    private final Iterator<String> iter;
    private final Pattern delimiter;
    private final int column;

    public ColumnExtractor(Iterator<String> iterator, char delimiter, int column) {
        this.iter = iterator;
        this.delimiter = Pattern.compile(String.valueOf(delimiter));
        this.column = column;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public String next() {
        String[] cols = delimiter.split(iter.next());
        return cols.length > column ? cols[column] : null;
    }

    @Override
    public void remove() {
        iter.remove();
    }
}
