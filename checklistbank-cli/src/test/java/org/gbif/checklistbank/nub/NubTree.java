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
import org.gbif.utils.file.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple class to keep a taxonomy of names.
 * We use this to compare nub build outputs with a very simple text based tree format that is very easy to read.
 * Especially useful for larger tree snippets.
 */
public class NubTree implements Iterable<NubNode> {
  private long count;
  private NubNode root = new NubNode(null, null, false);
  private static final Pattern INDENT = Pattern.compile("^( +\\" + TxtPrinter.SYNONYM_SYMBOL + "?\\" + TxtPrinter.BASIONYM_SYMBOL + "?)");
  private static final Pattern RANK = Pattern.compile(" \\[([a-z]+)\\] *$");

  public static NubTree read(String classpathFilename) throws IOException {
    return read(FileUtils.classpathStream(classpathFilename));
  }

  public long getCount() {
    return count;
  }

  public static NubTree read(InputStream stream) throws IOException {
    NubTree tree = new NubTree();
    LinkedList<NubNode> parents = Lists.newLinkedList();

    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
    String line = br.readLine();
    while (line != null) {
      int level = 0;
      boolean synonym = false;
      boolean basionym = false;
      if (!StringUtils.isBlank(line)) {
        tree.count++;
        Matcher m = INDENT.matcher(line);
        if (m.find()) {
          String prefix = m.group(1);
          if (prefix.endsWith(TxtPrinter.BASIONYM_SYMBOL)) {
            basionym = true;
          }
          if (prefix.contains(TxtPrinter.SYNONYM_SYMBOL)) {
            synonym = true;
          }
          level = prefix.length() - (synonym ? 1 : 0) - (basionym ? 1 : 0);
          if (level % 2 != 0) {
            throw new IllegalArgumentException("Tree is not indented properly. Use 2 spaces only for: " + line);
          }
          level = level / 2;
          NubNode n = node(m.replaceAll("").trim(), basionym);
          while (parents.size() > level) {
            // remove latest parents until we are at the right level
            parents.removeLast();
          }
          if (parents.size() < level) {
            throw new IllegalArgumentException("Tree is not properly indented. Use 2 spaces for children: " + line);
          }
          NubNode p = parents.peekLast();
          if (synonym) {
            p.synonyms.add(n);
          } else {
            p.children.add(n);
          }
          parents.add(n);

        } else {
          NubNode n = node(line.trim(), false);
          tree.getRoot().children.add(n);
          parents.clear();
          parents.add(n);
        }
      }
      line = br.readLine();
    }
    return tree;
  }

  private static NubNode node(String line, boolean basionym) {
    Matcher m = RANK.matcher(line);
    final Rank rank;
    final String name;
    if (m.find()) {
      rank = Rank.valueOf(m.group(1).toUpperCase());
      name = m.replaceAll("");
    } else {
      rank = null;
      name = line.trim();
    }
    return new NubNode(name, rank, basionym);
  }

  public NubNode getRoot() {
    return root;
  }

  public void print(Appendable out) throws IOException {
    for (NubNode n : root.children) {
      n.print(out, 0, false);
    }
  }

  @Override
  public Iterator<NubNode> iterator() {
    return new NNIterator(this);
  }

  private class NNIter {
    private int synIdx;
    private final NubNode node;

    public NNIter(NubNode node) {
      this.node = node;
    }

    public boolean moreSynonyms() {
      return node.synonyms.size() > synIdx;
    }

    public NNIter nextSynonym() {
      NubNode n = node.synonyms.get(synIdx);
      synIdx++;
      return new NNIter(n);
    }
  }

  private class NNIterator implements Iterator<NubNode> {
    private LinkedList<NNIter> stack = Lists.newLinkedList();
    private NNIter curr = null;

    NNIterator(NubTree tree) {
      for (NubNode r : tree.getRoot().children) {
        this.stack.addFirst(new NNIter(r));
      }
    }

    @Override
    public boolean hasNext() {
      return !stack.isEmpty() || (curr != null && curr.moreSynonyms());
    }

    @Override
    public NubNode next() {
      if (curr == null) {
        poll();
        return curr.node;

      } else if (curr.moreSynonyms()) {
        return curr.nextSynonym().node;

      } else {
        poll();
        return curr.node;
      }
    }

    private void poll() {
      curr = stack.removeLast();
      while (!curr.node.children.isEmpty()) {
        stack.add(new NNIter(curr.node.children.removeLast()));
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
