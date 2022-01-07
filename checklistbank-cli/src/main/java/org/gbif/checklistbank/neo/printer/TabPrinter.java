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
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.dwc.TabWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;

/**
 * Dumps a neo db in a simple tab delimited format used by the nub integration tests.
 * Expects no pro parte relations in the walker!
 */
public class TabPrinter implements TreePrinter {
  private final Function<Node, String> getTitle;
  private final TabWriter writer;
  private static final Joiner ID_CONCAT = Joiner.on(";").skipNulls();

  public TabPrinter(Writer writer, Function<Node, String> getTitle) {
    this.writer = new TabWriter(writer);
    this.getTitle = getTitle;
  }

  @Override
  public void close() {
  }

  @Override
  public void start(Node n) {
    try {
      String[] row = new String[7];
      row[0] = String.valueOf(n.getId());
      if (n.hasLabel(Labels.SYNONYM)) {
        // we can have multiple accepted parents for pro parte synonyms
        Set<Long> parentKeys = Sets.newHashSet();
        for (Relationship synRel : n.getRelationships(Direction.OUTGOING, RelType.SYNONYM_OF)) {
          parentKeys.add(synRel.getOtherNode(n).getId());
        }
        for (Relationship synRel : n.getRelationships(Direction.OUTGOING, RelType.PROPARTE_SYNONYM_OF)) {
          parentKeys.add(synRel.getOtherNode(n).getId());
        }
        row[1] = ID_CONCAT.join(parentKeys);
      } else {
        if (n.hasRelationship(Direction.INCOMING, RelType.PARENT_OF)) {
          row[1] = String.valueOf(n.getSingleRelationship(RelType.PARENT_OF, Direction.INCOMING).getOtherNode(n).getId());
        }
      }
      if (n.hasRelationship(Direction.INCOMING, RelType.BASIONYM_OF)) {
        row[2] = String.valueOf(n.getSingleRelationship(RelType.BASIONYM_OF, Direction.INCOMING).getOtherNode(n).getId());
      }
      if (n.hasProperty(NeoProperties.RANK)) {
        row[3] = Rank.values()[(Integer) n.getProperty(NeoProperties.RANK)].name();
      }
      row[4] = n.hasLabel(Labels.SYNONYM) ? TaxonomicStatus.SYNONYM.name() : TaxonomicStatus.ACCEPTED.name();
      row[6] = getTitle.apply(n);
      writer.write(row);
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void end(Node n) {

  }
}
