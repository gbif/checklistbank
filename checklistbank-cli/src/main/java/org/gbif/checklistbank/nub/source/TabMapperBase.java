package org.gbif.checklistbank.nub.source;

import java.io.IOException;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TabMapperBase extends Writer {
  private static final Logger LOG = LoggerFactory.getLogger(TabMapperBase.class);

  private Integer rowNumber = 0;
  private int ROW_SIZE;
  private int idx;
  private String[] row;
  private String lastPartialString;

  public TabMapperBase(int ROW_SIZE) {
    this.ROW_SIZE = ROW_SIZE;
    this.row = new String[ROW_SIZE];
  }

  protected abstract void addRow(String[] row);

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
    int b = off, t = off + len;
    int bufStart = b;
    while (b < t) {
      char c = cbuf[b];
      if (c == '\n' || c == '\t') {
        if (lastPartialString != null) {
          row[idx] = lastPartialString + new String(cbuf, bufStart, b - bufStart);
          lastPartialString = null;
        } else {
          row[idx] = new String(cbuf, bufStart, b - bufStart);
          if (row[idx].equals("\\N") || row[idx].equals("")) {
            row[idx] = null;
          }
        }
        bufStart = b + 1;
        idx++;
        if (idx == ROW_SIZE) {
          idx = 0;
        }
      }
      if (c == '\n') {
        rowNumber++;
        addRow(this.row);
        row = new String[ROW_SIZE];
      }
      b++;
    }
    if (bufStart <= t) {
      lastPartialString = String.copyValueOf(cbuf, bufStart, b - bufStart);
    }
  }
}