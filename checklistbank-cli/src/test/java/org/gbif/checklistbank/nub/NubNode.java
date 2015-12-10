package org.gbif.checklistbank.nub;

import org.gbif.checklistbank.neo.printer.TreePrinter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple bean for testing nub taxonomies.
 */
public class NubNode {
  public String name;
  public boolean basionym;
  public List<NubNode> synonyms = Lists.newArrayList();
  public LinkedList<NubNode> children = Lists.newLinkedList();

  public NubNode(String name, boolean isBasionym) {
    this.name = name;
    this.basionym = isBasionym;
  }

  public boolean isBasionym() {
    return basionym;
  }

  @Override
  public String toString() {
    return name;
  }

  public void print(Appendable out, int level, boolean synonym) throws IOException {
    out.append(StringUtils.repeat(" ", level * 2));
    if (synonym) {
      out.append(TreePrinter.SYNONYM_SYMBOL);
    }
    if (basionym) {
      out.append(TreePrinter.BASIONYM_SYMBOL);
    }
    out.append(name + "\n");
    // recursive
    for (NubNode n : synonyms) {
      n.print(out, level + 1, true);
    }
    for (NubNode n : children) {
      n.print(out, level + 1, false);
    }
  }
}
