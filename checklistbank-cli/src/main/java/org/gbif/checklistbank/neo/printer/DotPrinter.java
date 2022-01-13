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
package org.gbif.checklistbank.neo.printer;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.traverse.RankEvaluator;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.Nullable;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.google.common.base.Function;
import com.google.common.base.Throwables;

/**
 * Expects no pro parte relations in the walker!
 * http://www.graphviz.org/doc/info/lang.html
 *
 * Example:
 *
 * digraph G {
 *   t1 [label="Animalia"]
 *   t1 -> t16 [type=parent_of];
 *   t16 [label="Abies"]
 *   t17 [label="Pinus"]
 *   t17 -> t16 [type=synonym_of];
 * }
 */
public class DotPrinter implements TreePrinter {
  private final Writer writer;
  private final Function<Node, String> getTitle;
  private final RankEvaluator rankEvaluator;

  public DotPrinter(Writer writer, @Nullable Rank rankThreshold, Function<Node, String> getTitle) {
    this.writer = writer;
    this.getTitle = getTitle;
    this.rankEvaluator = new RankEvaluator(rankThreshold);
    printHeader();
  }

  private void printHeader() {
    try {
      writer.write("digraph G {\n");
      writer.write("  node [shape=plaintext]\n\n");

    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void close() {
    try {
      writer.write("}\n");
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void start(Node n) {
    try {
      // n1 [label="Animalia"]
      writer.append("  n");
      writer.append(String.valueOf(n.getId()));
      writer.append("  [label=\"");
      writer.append(getTitle.apply(n));
      if (n.hasLabel(Labels.SYNONYM)) {
        writer.append("\", fontcolor=darkgreen]\n");
      } else {
        writer.append("\"]\n");
      }

      // edges
      for (Relationship rel : n.getRelationships(Direction.OUTGOING)) {
        if (rankEvaluator.evaluateNode(rel.getOtherNode(n))) {
          //n1 -> n16 [type=parent_of]
          long start;
          long end;
          String type = null;
          if (rel.isType(RelType.PARENT_OF)) {
            start = rel.getStartNode().getId();
            end = rel.getEndNode().getId();
          } else {
            start = rel.getEndNode().getId();
            end = rel.getStartNode().getId();
            if (rel.isType(RelType.SYNONYM_OF)) {
              type = "syn";
            } else if (rel.isType(RelType.PROPARTE_SYNONYM_OF)) {
              type = "pp";
            } else if (rel.isType(RelType.BASIONYM_OF)) {
              type = "bas";
            } else {
              type = rel.getType().name().toLowerCase().replace("_of", "");
            }
          }
          writer.append("  n");
          writer.append(String.valueOf(start));
          writer.append(" -> n");
          writer.append(String.valueOf(end));
          if (type != null) {
            writer.append("  [color=darkgreen, fontcolor=darkgreen, label=");
            writer.append(type);
            writer.append("]");
          }
          writer.append("\n");
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
