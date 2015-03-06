package org.gbif.nub.lookup;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple manual utility to merge new synonym entries into a single, clean dictionary file
 * to be picked up by the nub lookup and hosted at
 * http://rs.gbif.org/dictionaries/synonyms/
 */
public class SynonymMerger {

  public static void main (String[] args) throws Exception {
    File f = new File("/Users/markus/code/rs.gbif.org/dictionaries/synonyms/family.txt");

    Map<String, Set<String>> vals = new TreeMap<String, Set<String>>();
    LineIterator iter = new LineIterator(new FileReader(f));
    while (iter.hasNext()) {
      String line = iter.next();
      if (StringUtils.isBlank(line)) {
        continue;
      }
      String[] cols = line.split("\t");
      if (cols.length != 2) {
        System.out.println("IGNORE LINE: "+line);
        continue;
      }
      String syn = cols[0].toUpperCase().trim();
      String acc = cols[1].trim();
      if (vals.containsKey(syn)) {
        if (vals.get(syn).contains(acc)) {
          // same entry, just ignore
        } else {
          vals.get(syn).add(acc);
          System.out.println("CONFLICT for " + syn);
          System.out.println("  " + vals.get(syn));
        }
      } else {
        vals.put(syn, new HashSet<String>());
        vals.get(syn).add(acc);
      }
    }

    // write
    File fo = new File(f.getParent(), f.getName()+"-2");
    FileWriter out = new FileWriter(fo);
    for (Map.Entry<String, Set<String>> entry : vals.entrySet()) {
      for (String x: entry.getValue()) {
        out.write(entry.getKey());
        out.write("\t");
        out.write(x);
        out.write("\n");
      }
    }
    out.close();
    System.out.println("DONE");
  }
}
