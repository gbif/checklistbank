package org.gbif.checklistbank.neo.printer;

import org.gbif.checklistbank.neo.traverse.StartEndHandler;

public interface TreePrinter extends StartEndHandler, AutoCloseable {

  /**
   * We prefer no exceptions in close()
   */
  @Override
  void close();

}
