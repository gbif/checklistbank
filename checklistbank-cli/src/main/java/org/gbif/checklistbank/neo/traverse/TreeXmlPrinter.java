package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Ordering;
import com.google.common.xml.XmlEscapers;
import org.neo4j.graphdb.Node;
import org.parboiled.common.StringUtils;

/**
 * A handler that can be used with the TaxonWalker to print a neo4j taxonomy in a simple nested text structure.
 */
public class TreeXmlPrinter implements StartEndHandler {
  private final Writer writer;
  private final NameParser parser;
  private LinkedList<String> parents = Lists.newLinkedList();

  public static final Ordering<Node> SYNONYM_ORDER = Ordering.natural().onResultOf(new Function<Node, String>() {
    @Nullable
    @Override
    public String apply(@Nullable Node n) {
      return (String) n.getProperty(NeoProperties.SCIENTIFIC_NAME);
    }
  });

  public TreeXmlPrinter(Writer writer) {
    this.writer = writer;
    parser = new NameParser(250);
  }

  @Override
  public void start(Node n) {
    open(n, false);
    // check for synonyms and sort by name
    for (Node s : SYNONYM_ORDER.sortedCopy(Traversals.SYNONYMS.traverse(n).nodes())) {
      open(s, true);
    }
  }

  @Override
  public void end(Node n) {
    close(n);
  }

  private void open(Node n, boolean close) {
    try {
      String sname = (String)n.getProperty(NeoProperties.SCIENTIFIC_NAME, "root");
      String cname = (String)n.getProperty(NeoProperties.CANONICAL_NAME, null);
      Rank rank = Rank.values()[(Integer) n.getProperty(NeoProperties.RANK, Rank.UNRANKED.ordinal())];
      if (Strings.isNullOrEmpty(cname)) {
        try {
          ParsedName pn = parser.parse(sname, rank);
          cname = pn.canonicalName();
        } catch (UnparsableException e) {
          cname = sname;
        }
      }
      cname = escapeTag(cname);

      writer.write(StringUtils.repeat(' ', parents.size() * 2));
      writer.write("<");
      writer.write(cname);
      printAttr("name", sname);
      printAttr("rank", rank.name().toLowerCase());
      if (n.hasLabel(Labels.BASIONYM)) {
        printAttr("basionym", "true");
      }
      if (n.hasLabel(Labels.SYNONYM)) {
        printAttr("synonym", "true");
      }
      if (close) {
        writer.write(" /");
      } else {
        parents.add(cname);
      }
      writer.write(">");
      writer.write("\n");
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  private void close(Node n) {
    try {
      writer.write("</");
      writer.write(parents.removeLast());
      writer.write(">");
      writer.write("\n");
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  private void printAttr(String attr, String value) throws IOException {
    if (!Strings.isNullOrEmpty(value)) {
      writer.write(" ");
      writer.write(attr);
      writer.write("=\"");
      writer.write(XmlEscapers.xmlAttributeEscaper().escape(value));
      writer.write("\"");
    }
  }

  private String escapeTag(String x) {
    return x.replaceAll("[^a-zA-Z0-9_.-]", "_");
  }
}
