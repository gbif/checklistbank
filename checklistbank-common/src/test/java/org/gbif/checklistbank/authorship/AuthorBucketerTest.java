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
package org.gbif.checklistbank.authorship;

import org.gbif.checklistbank.iterable.ColumnExtractor;
import org.gbif.utils.file.FileUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.junit.jupiter.api.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utility that reads a stream of author names and splits them into sets of names that are classified as the same name by the author comparator.
 */
public class AuthorBucketerTest {

  public static Map<String, Set<String>> clusterNames(Iterator<String> authors) {
    Map<String, Set<String>> buckets = Maps.newHashMap();
    AuthorComparator comp = AuthorComparator.createWithAuthormap();

    while (authors.hasNext()) {
      String author = authors.next();
      String match = null;
      for (String x : buckets.keySet()) {
        if (comp.compareStrict(author, null, x, null)) {
          match = x;
          break;
        }
      }
      if (match == null) {
        // new bucket
        buckets.put(author, Sets.newHashSet(author));
      } else {
        buckets.get(match).add(author);
      }
    }
    return buckets;
  }

  @Test
  public void testAuthormap() throws Exception {
    LineIterator iter = FileUtils.getLineIterator(FileUtils.classpathStream("authorship/authormap.txt"));
    int lines = 0;
    while (iter.hasNext()) {
      lines++;
      iter.next();
    }
    iter = FileUtils.getLineIterator(FileUtils.classpathStream("authorship/authormap.txt"));
    Map<String, Set<String>> buckets = clusterNames(new ColumnExtractor(iter, '\t', 0));

    Joiner join = Joiner.on("; ").skipNulls();
    for (Map.Entry<String, Set<String>> entry : buckets.entrySet()) {
      if (entry.getValue().size() > 1) {
        System.out.println(entry.getKey());
        System.out.println("  " + join.join(entry.getValue()));
      }
    }
    System.out.println("Lines: " + lines + ", buckets: " + buckets.size());
    assertTrue(buckets.size() > 3212);
  }

}
