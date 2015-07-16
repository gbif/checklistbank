package org.gbif.checklistbank.postgres;

import java.io.IOException;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A writer implementation that consumes a result stream from postgres
 * querying using the postgres jdbc copy command.
 * This is a very faste and direct way to execute select sql statements from postgres.
 *
 * Implement addRow to consume a single result row.
 *
 */
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
      }
      if (c == '\n') {
        rowNumber++;
        if (idx > 1) {
          // ignore empty rows
          addRow(this.row);
        }
        idx = 0;
        row = new String[ROW_SIZE];
      }
      b++;
    }
    if (bufStart <= t) {
      lastPartialString = String.copyValueOf(cbuf, bufStart, b - bufStart);
    }
  }
}