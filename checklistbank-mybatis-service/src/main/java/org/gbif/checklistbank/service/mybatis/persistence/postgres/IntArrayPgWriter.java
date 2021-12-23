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
