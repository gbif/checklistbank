package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;

import javax.annotation.Nullable;

import com.google.common.base.Function;
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

  final int indentation;
  int level = 0;

  public static final Ordering<Node> SYNONYM_ORDER = Ordering.natural().onResultOf(new Function<Node, String>() {
    @Nullable
    @Override
    public String apply(@Nullable Node n) {
      return (String) n.getProperty(NeoProperties.SCIENTIFIC_NAME);
    }
  });

  public TreePrinter(int indentation) {
    this.indentation = indentation;
  }

  public TreePrinter() {
    this(2);
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
    LOG.debug("{}{}{}{} [{}]",
        StringUtils.repeat(' ', level * indentation),
        n.hasLabel(Labels.SYNONYM) ? SYNONYM_SYMBOL : "",
        n.hasLabel(Labels.BASIONYM) ? BASIONYM_SYMBOL : "",
        n.getProperty(NeoProperties.SCIENTIFIC_NAME),
        n.hasProperty(NeoProperties.RANK) ? Rank.values()[(Integer) n.getProperty(NeoProperties.RANK)] : "none");
  }
}
