package org.gbif.checklistbank.iterable;

import java.util.Iterator;

import com.google.common.base.Preconditions;

/**
 * Utils class to provider an iterable integer range usable in for each statements.
 */
public class Ints implements Iterable<Integer> {

  private int start;
  private int end;

  /**
   * Provides an int iterable starting with 1
   * @param end inclusive end
   */
  public static Iterable<Integer> until(int end) {
    return new Ints(1, end);
  }

  /**
   * @param start inclusive start
   * @param end inclusive end
   */
  public static Iterable<Integer> range(int start, int end) {
    return new Ints(start, end);
  }

  private Ints(int start, int end) {
    Preconditions.checkArgument(start <= end);
    this.start = start;
    this.end = end;
  }

  @Override
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {
      private int actual = start;

      @Override
      public boolean hasNext() {
        return actual <= end;
      }

      @Override
      public Integer next() {
        return actual++;
      }

      @Override
      public void remove() {
        // do nothing
      }
    };
  }
}