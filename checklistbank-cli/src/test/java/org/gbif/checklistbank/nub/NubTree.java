package org.gbif.checklistbank.nub;

import org.gbif.utils.file.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.internal.Lists;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple class to keep a taxonomy of names.
 * We use this to compare nub build outputs with a very simple text based tree format that is very easy to read.
 * Especially useful for larger tree snippets.
 */
public class NubTree {
    private List<NubNode> root = Lists.newArrayList();

    public static NubTree read(String classpathFilename) throws IOException {
        return read(FileUtils.classpathStream(classpathFilename));
    }

    public static NubTree read(InputStream stream) throws IOException {
        Pattern INDENT = Pattern.compile("^( +\\*?)");
        NubTree tree = new NubTree();
        LinkedList<NubNode> parents = Lists.newLinkedList();

        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line = br.readLine();
        while ( line != null ) {
            int level = 0;
            boolean synonym = false;
            if (!StringUtils.isBlank(line)) {
                Matcher m = INDENT.matcher(line);
                if (m.find()) {
                    String prefix = m.group(1);
                    if (prefix.endsWith("*")) {
                        synonym = true;
                    }
                    level = prefix.length() - (synonym ? 1 : 0);
                    if (level % 2 != 0) {
                        throw new IllegalArgumentException("Tree is not indented properly. Use 2 spaces only for: " + line);
                    }
                    level = level / 2;
                    NubNode n = new NubNode(m.replaceAll("").trim());
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
                    NubNode n = new NubNode(line.trim());
                    tree.getRoot().add(n);
                    parents.clear();
                    parents.add(n);
                }
            }
            line = br.readLine();
        }
        return tree;
    }

    public List<NubNode> getRoot() {
        return root;
    }

    public void setRoot(List<NubNode> root) {
        this.root = root;
    }

    public void print(Appendable out) throws IOException {
        for (NubNode n : root) {
            n.print(out, 0, false);
        }
    }
}
