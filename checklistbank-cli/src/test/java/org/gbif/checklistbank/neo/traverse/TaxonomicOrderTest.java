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

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.BaseTest;
import org.gbif.checklistbank.cli.model.NameUsageNode;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import static org.junit.Assert.assertArrayEquals;

public class TaxonomicOrderTest extends BaseTest {

  @Test
  public void testCompare() throws Exception {
    initDb();
    try (Transaction tx = beginTx()) {
      TaxonomicOrder order = new TaxonomicOrder();
      List<Node> nodes = Lists.newArrayList(
        build(Rank.GENUS, "Pinus"),
        build(Rank.GENUS, "Abies"),
        build(Rank.FAMILY, "Pinaceae"),
        build(Rank.SPECIES, "Abies alba", "Abies alba Mill."),
        build(Rank.KINGDOM, "Plantae"),
        build(Rank.SUBSPECIES, "Abies alba alpina", "Abies alba subsp. alpina L."),
        build(Rank.SPECIES, "Abies alba", "Abies alba L."),
        build(Rank.VARIETY, "Abies alba berlina", "Abies alba var. berlina DC."),
        build(Rank.SUBSPECIES, "Abies alba caucasia", "Abies alba subsp. caucasia Reich."),
        build(Rank.UNRANKED, "nonsense"),
        build(null, "Incertae sedis"),
        build(Rank.INFRAGENERIC_NAME, "Abieta")
      );
      Collections.sort(nodes, order);

      Long[] expected = new Long[]{4l,2l,1l,0l,11l,6l,3l,5l,8l,7l,9l,10l};
      Long[] sorted = Lists.transform(nodes, new Function<Node, Long>() {
        @Nullable
        @Override
        public Long apply(@Nullable Node n) {
          return n.getId();
        }
      }).toArray(new Long[]{});

      System.out.println(Joiner.on("l,").join(sorted));
      assertArrayEquals(expected, sorted);
    }
  }

  private Node build(Rank rank, String name){
    return build(rank, name, name);
  }

  private Node build(Rank rank, String canonical, String scientific){
    Node n = dao.createTaxon();
    NameUsage u = new NameUsage();
    u.setRank(rank);
    u.setCanonicalName(canonical);
    u.setScientificName(scientific);
    dao.store(new NameUsageNode(n, u, true), true);
    return n;
  }
}