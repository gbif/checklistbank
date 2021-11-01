package org.gbif.checklistbank.neo.printer;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.traverse.RankEvaluator;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * Expects no pro parte relations in the walker!

 * https://en.wikipedia.org/wiki/Graph_Modelling_Language
 * See http://www.fim.uni-passau.de/fileadmin/files/lehrstuhl/brandenburg/projekte/gml/gml-technical-report.pdf
 */
public class GmlPrinter implements TreePrinter {
  private final Writer writer;
  private final List<Edge> edges = Lists.newArrayList();
  private final Function<Node, String> getTitle;
  private final boolean strictTree;
  private final RankEvaluator rankEvaluator;

  /**
   * @param strictTree if true omit any pro parte and basionym relations to force a strict tree
   */
  public GmlPrinter(Writer writer, @Nullable Rank rankThreshold, Function<Node, String> getTitle, boolean strictTree) {
    this.strictTree = strictTree;
    this.writer = writer;
    this.getTitle = getTitle;
    this.rankEvaluator = new RankEvaluator(rankThreshold);
    printHeader();
  }

  private static class Edge {
    public final long source;
    public final long target;

    public Edge(long source, long target) {
      this.source = source;
      this.target = target;
    }

    public static Edge create(Relationship rel) {
      return new Edge(rel.getStartNode().getId(), rel.getEndNode().getId());
    }

    public static Edge inverse(Relationship rel) {
      return new Edge(rel.getEndNode().getId(), rel.getStartNode().getId());
    }

    void print(Writer writer) throws IOException {
      writer.append("  edge [\n")
          .append("    source ")
          .append(String.valueOf(source))
          .append("\n    target ")
          .append(String.valueOf(target))
          .append("\n  ]\n");
    }
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
      for (Edge e : edges) {
        e.print(writer);
      }
      writer.write("]\n");
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void start(Node n) {
    try {
      String label = String.format("%s [%s]",
          getTitle.apply(n),
          NeoProperties.getRank(n, Rank.UNRANKED).name().toLowerCase()
      );
      writer.write("  node [\n");
      writer.write("    id " + n.getId() + "\n");
      writer.write("    label \"" + label + "\"\n");
      writer.write("  ]\n");

      // keep edges with minimal footprint for later writes, they need to go at the very end!

      // for a strict tree we only use parent and synonym of relations
      // synonym_of relations are inversed so the tree strictly points into one direction
      if (strictTree) {
        for (Relationship rel : n.getRelationships(Direction.OUTGOING, RelType.PARENT_OF)) {
          if (rankEvaluator.evaluateNode(rel.getOtherNode(n))) {
            edges.add(Edge.create(rel));
          }
        }
        for (Relationship rel : n.getRelationships(Direction.OUTGOING, RelType.SYNONYM_OF)) {
          if (rankEvaluator.evaluateNode(rel.getOtherNode(n))) {
            edges.add(Edge.inverse(rel));
          }
        }

      } else {
        for (Relationship rel : n.getRelationships(Direction.OUTGOING)) {
          if (rankEvaluator.evaluateNode(rel.getOtherNode(n))) {
            edges.add(Edge.create(rel));
          }
        }
      }

    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void end(Node n) {

  }
}
