package org.gbif.checklistbank.nub;

import java.io.IOException;
import java.util.List;

import com.beust.jcommander.internal.Lists;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple bean for testing nub taxonomies.
 */
public class NubNode {
    public String name;
    public List<NubNode> synonyms = Lists.newArrayList();
    public List<NubNode> children = Lists.newArrayList();

    public NubNode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public void print(Appendable out, int level, boolean synonym) throws IOException {
        out.append(StringUtils.repeat(" ", level * 2));
        if (synonym) {
            out.append("*");
        }
        out.append(name + "\n");
        // recursive
        for (NubNode n : synonyms) {
            n.print(out, level + 1, true);
        }
        for (NubNode n : children) {
            n.print(out, level+1, false);
        }
    }
}
