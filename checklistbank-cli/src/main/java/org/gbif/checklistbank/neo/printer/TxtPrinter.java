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
import org.gbif.checklistbank.neo.NeoProperties;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.Nullable;

import org.neo4j.graphdb.Node;
import org.parboiled.common.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Throwables;

/**
 * A handler that can be used with the TaxonWalker to print a neo4j taxonomy in a simple nested text structure.
 */
public class TxtPrinter implements TreePrinter {
  public static final String SYNONYM_SYMBOL = "*";
  public static final String BASIONYM_SYMBOL = "$";

  private static final int indentation = 2;
  private int level = 0;
  private final Function<Node, String> getTitle;
  private final Writer writer;
  private final boolean showIds;


  public TxtPrinter(Writer writer, Function<Node, String> getTitle, boolean showIds) {
    this.writer = writer;
    this.getTitle = getTitle;
    this.showIds = showIds;
  }

  public TxtPrinter(Writer writer, Function<Node, String> getTitle) {
    this(writer, getTitle, false);
  }

  public TxtPrinter(Writer writer) {
    this(writer, new Function<Node, String>() {
      @Nullable
      @Override
      public String apply(@Nullable Node input) {
        return NeoProperties.getScientificName(input);
      }
    }, false);
  }

  @Override
  public void start(Node n) {
    print(n);
    level++;
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
      writer.write(getTitle.apply(n));
      if (n.hasProperty(NeoProperties.RANK)) {
        writer.write(" [");
        writer.write(Rank.values()[(Integer) n.getProperty(NeoProperties.RANK)].name().toLowerCase());
        if (showIds) {
          writer.write(",");
          writer.write(String.valueOf(n.getId()));
        }
        writer.write("]");
      }
      writer.write("\n");

    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public void close() {

  }
}
