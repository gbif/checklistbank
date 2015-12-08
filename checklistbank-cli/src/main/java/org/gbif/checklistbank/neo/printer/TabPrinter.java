package org.gbif.checklistbank.neo.printer;

import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.traverse.StartEndHandler;
import org.gbif.io.TabWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dumps a neo db in a simple tab delimited format used by the nub integration tests.
 */
public class TabPrinter implements StartEndHandler, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(TabPrinter.class);
  private final String nameProperty;
  private final TabWriter writer;
  private static final Joiner ID_CONCAT = Joiner.on(";").skipNulls();

  public TabPrinter(Writer writer, String nameProperty) {
    this.writer = new TabWriter(writer);
    this.nameProperty = nameProperty;
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
        for (Relationship synRel : n.getRelationships(RelType.SYNONYM_OF, Direction.OUTGOING)) {
          parentKeys.add(synRel.getOtherNode(n).getId());
        }
        row[1] = ID_CONCAT.join(parentKeys);
      } else {
        if (n.hasRelationship(RelType.PARENT_OF, Direction.INCOMING)) {
          row[1] = String.valueOf(n.getSingleRelationship(RelType.PARENT_OF, Direction.INCOMING).getOtherNode(n).getId());
        }
      }
      if (n.hasRelationship(RelType.BASIONYM_OF, Direction.INCOMING)) {
        row[2] = String.valueOf(n.getSingleRelationship(RelType.BASIONYM_OF, Direction.INCOMING).getOtherNode(n).getId());
      }
      if (n.hasProperty(NeoProperties.RANK)) {
        row[3] = Rank.values()[(Integer) n.getProperty(NeoProperties.RANK)].name();
      }
      row[4] = n.hasLabel(Labels.SYNONYM) ? TaxonomicStatus.SYNONYM.name() : TaxonomicStatus.ACCEPTED.name();
      row[6] = (String) n.getProperty(nameProperty, TreePrinter.NULL_NAME);
      writer.write(row);
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void end(Node n) {

  }
}
