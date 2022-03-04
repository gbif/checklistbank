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
package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.printer.TxtPrinter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

/**
 * Simple bean for testing nub taxonomies.
 */
public class NubNode {
  public String name;
  public Rank rank;
  public boolean basionym;
  public List<NubNode> synonyms = Lists.newArrayList();
  public LinkedList<NubNode> children = Lists.newLinkedList();

  public NubNode(String name, Rank rank, boolean isBasionym) {
    this.name = name;
    this.rank = rank;
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
      out.append(TxtPrinter.SYNONYM_SYMBOL);
    }
    if (basionym) {
      out.append(TxtPrinter.BASIONYM_SYMBOL);
    }
    out.append(name);
    if (rank != null) {
      out.append(" [" + rank.name().toLowerCase() + "]");
    }
    out.append("\n");
    // recursive
    for (NubNode n : synonyms) {
      n.print(out, level + 1, true);
    }
    for (NubNode n : children) {
      n.print(out, level + 1, false);
    }
  }
}
