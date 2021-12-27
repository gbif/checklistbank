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
package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * A writer for native postgres copy commands that converts a pg query result of integers into a integer list
 * very efficiently.
 */
public class IntArrayPgWriter extends Writer {
  private ArrayList<Integer> array = Lists.newArrayListWithCapacity(10000);
  private final CharBuffer value = CharBuffer.allocate(16);

  @Override
  public void close() throws IOException {
    // nothing to do
  }

  @Override
  public void flush() throws IOException {
    // nothing to do
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    CharBuffer buf = CharBuffer.wrap(cbuf, off, len);
    while (buf.hasRemaining()) {
      char c = buf.get();
      if (c == '\n') {
        add();
      } else {
        value.append(c);
      }
    }
    add();
  }


  private void add() throws IOException {
    value.flip();
    if (value.hasRemaining()) {
      try {
        array.add(Integer.valueOf(value.toString()));
      } catch (NumberFormatException e) {
      } catch (NullPointerException e) {
      }
    }
    value.clear();
  }


  public List<Integer> result() {
    return array;
  }
}
