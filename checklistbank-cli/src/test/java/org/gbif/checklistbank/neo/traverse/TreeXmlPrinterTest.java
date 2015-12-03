package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.source.ClasspathSource;

import java.io.PrintWriter;
import java.io.Writer;

import org.junit.Test;

/**
 *
 */
public class TreeXmlPrinterTest {

  @Test
  public void testTree() throws Exception {
    try (ClasspathSource src = new ClasspathSource(8);) {
      src.init(true, false);
      UsageDao dao = src.open();

      try (Writer writer = new PrintWriter(System.out)) {
        dao.printTree(writer, true);
      }
      dao.closeAndDelete();
    }
  }
}