/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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