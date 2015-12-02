package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;

import java.io.IOException;
import java.io.Writer;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Ordering;
import org.neo4j.graphdb.Node;
import org.parboiled.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handler that can be used with the TaxonWalker to print a neo4j taxonomy in a simple nested text structure.
 */
public class TreePrinter implements StartEndHandler {
  private static final Logger LOG = LoggerFactory.getLogger(TreePrinter.class);
  public static final String SYNONYM_SYMBOL = "*";
  public static final String BASIONYM_SYMBOL = "$";

  private static final int indentation = 2;
  private int level = 0;
  private final String nameProperty;
  private final Writer writer;

  public final Ordering<Node> SYNONYM_ORDER = Ordering.natural().onResultOf(new Function<Node, String>() {
    @Nullable
    @Override
    public String apply(@Nullable Node n) {
      return (String) n.getProperty(nameProperty);
    }
  });

  public TreePrinter(Writer writer, String nameProperty) {
    this.writer = writer;
    this.nameProperty = nameProperty;
  }

  public TreePrinter(Writer writer) {
    this(writer, NeoProperties.SCIENTIFIC_NAME);
  }

  @Override
  public void start(Node n) {
    print(n);
    level++;
    // check for synonyms and sort by name
    for (Node s : SYNONYM_ORDER.sortedCopy(Traversals.SYNONYMS.traverse(n).nodes())) {
      print(s);
    }
  }

  @Override
  public void end(Node n) {
    level--;
  }

  private void print(Node n) {
    try {
      writer.write(StringUtils.repeat(' ', level * indentation));
      if (n.hasLabel(Labels.SYNONYM)) {
        writer.write(SYNONYM_SYMBOL);
      }
      if (n.hasLabel(Labels.BASIONYM)) {
        writer.write(BASIONYM_SYMBOL);
      }
      writer.write((String)n.getProperty(nameProperty, "???"));
      if (n.hasProperty(NeoProperties.RANK)) {
        writer.write(" [");
        writer.write(Rank.values()[(Integer) n.getProperty(NeoProperties.RANK)].name().toLowerCase());
        writer.write("]");
      }
      writer.write("\n");

    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }
}
