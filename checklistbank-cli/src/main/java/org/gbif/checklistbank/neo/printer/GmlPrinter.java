package org.gbif.checklistbank.neo.printer;

import java.io.IOException;
import java.io.Writer;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * https://en.wikipedia.org/wiki/Graph_Modelling_Language
 */
public class GmlPrinter implements TreePrinter {
  private final Writer writer;
  private final Function<Node, String> getTitle;

  public GmlPrinter(Writer writer, Function<Node, String> getTitle) {
    this.writer = writer;
    this.getTitle = getTitle;
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
      writer.write("    label \"" + getTitle.apply(n) + "\"\n");
      writer.write("  ]\n");

      // edges
      for (Relationship rel : n.getRelationships(Direction.OUTGOING)) {
        writer.write("  edge [\n");
        writer.write("    source " + rel.getStartNode().getId() + "\n");
        writer.write("    target " + rel.getEndNode().getId() + "\n");
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
