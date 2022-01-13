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
package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.NeoProperties;

import java.util.Comparator;

import org.neo4j.graphdb.Node;

/**
 * Orders taxon nodes by their rank first, then canonical name and scientificName ultimately.
 */
public class TaxonomicOrder implements Comparator<Node> {

  @Override
  public int compare(Node n1, Node n2) {
    int r1 = (int) n1.getProperty(NeoProperties.RANK, Integer.MAX_VALUE);
    int r2 = (int) n2.getProperty(NeoProperties.RANK, Integer.MAX_VALUE);

    if (r1!=r2) {
      return r1 - r2;
    }
    String c1 = NeoProperties.getCanonicalName(n1);
    String c2 = NeoProperties.getCanonicalName(n2);
    int canonicalComparison = c1.compareTo(c2);
    if (canonicalComparison == 0) {
      return NeoProperties.getScientificName(n1).compareTo(NeoProperties.getScientificName(n2));
    }
    return canonicalComparison;
  }

}
