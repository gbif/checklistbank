package org.gbif.checklistbank.neo.printer;

import org.gbif.checklistbank.neo.traverse.StartEndHandler;

import java.io.IOException;
import java.io.Writer;

import com.google.common.base.Throwables;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * https://en.wikipedia.org/wiki/Graph_Modelling_Language
 */
public class GmlPrinter implements StartEndHandler, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(GmlPrinter.class);
  private final Writer writer;
  private final String nameProperty;

  public GmlPrinter(Writer writer, String nameProperty) {
    this.writer = writer;
    this.nameProperty = nameProperty;
    printHeader();
  }

  private void printHeader() {
    try {
      writer.write("graph [\n");
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void close() {
    try {
      writer.write("]\n");
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void start(Node n) {
    try {
      writer.write("  node [\n");
      writer.write("    id " + n.getId() + "\n");
      if (n.hasProperty(nameProperty)) {
        writer.write("    label \"" + n.getProperty(nameProperty) + "\"\n");
      }
      writer.write("  ]\n");

      // edges
      for (Relationship rel : n.getRelationships(Direction.OUTGOING)) {
        writer.write("  edge [\n");
        writer.write("    source" + rel.getStartNode().getId() + "\n");
        writer.write("    target" + rel.getEndNode().getId() + "\n");
        writer.write("    label \"" + rel.getType().name() + "\"\n");
        writer.write("  ]\n");
      }

    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void end(Node n) {

  }
}
