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
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.IdGeneratorTest;
import org.gbif.nub.lookup.straight.IdLookup;

import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NubMatchHandlerTest {

    UsageDao dao = UsageDao.temporaryDao(10);

    @Test
    public void testMatching() throws Exception {
        IdLookup lookup = IdGeneratorTest.newTestLookup();
        try (Transaction tx = dao.beginTx()) {
            Node kp = dao.create(usage("Plantae", null, Rank.KINGDOM));
            kp.addLabel(Labels.ROOT);

            Node kv = dao.create(usage("Viruses", null, Rank.KINGDOM));
            kv.addLabel(Labels.ROOT);

            Node kf = dao.create(usage("Fungi", null, Rank.KINGDOM));
            kf.addLabel(Labels.ROOT);

            Node a = parentOf(dao.create(usage("Abies", null, Rank.GENUS)), kp);
            Node a1 = parentOf(dao.create(usage("Abies alba", null, Rank.SPECIES)), a);
            Node a2 = parentOf(dao.create(usage("Abies alpina", null, Rank.SPECIES)), a);
            Node a3 = parentOf(dao.create(usage("Abies alba subsp.", null, Rank.SUBSPECIES)), a1);

            Node o = parentOf(dao.create(usage("Oenanthe", null, Rank.GENUS)), kp);
            Node o1 = parentOf(dao.create(usage("Oenanthe sp.", null, Rank.SPECIES)), o);

            Node c = parentOf(dao.create(usage("Carex", null, Rank.GENUS)), kp);
            Node c1 = parentOf(dao.create(usage("Carex ×cayouettei", null, Rank.SPECIES)), c);
            Node c2 = parentOf(dao.create(usage("Carex comosa × Carex lupulina", null, Rank.SPECIES)), c);
            Node c3 = parentOf(dao.create(usage("Carex ×paletti", null, Rank.SPECIES)), c);

            Node v1 = parentOf(dao.create(usage("Aeropyrum coil-shaped virus", null, Rank.UNRANKED)), kv);
            Node v2 = parentOf(dao.create(usage("Aeropyrum coil-shaped virus 2", null, Rank.SPECIES)), kv);

            // assign keys
            NubMatchHandler handler = new NubMatchHandler(lookup, dao);
            TreeWalker.walkAcceptedTree(dao.getNeo(), handler);

            // verify keys
            verifyKey(a, null);
            verifyKey(a1, 9);
            verifyKey(a2, null);
            verifyKey(a3, null);

            verifyKey(o, 3);
            verifyKey(o1, null);

            verifyKey(c, null);
            verifyKey(c1, 14);
            verifyKey(c2, 15);
            verifyKey(c3, null);

            verifyKey(v1, 16);
            verifyKey(v2, null);
        }
    }

    public void verifyKey(Node n, Integer key) {
        NameUsage u = dao.readUsage(n, true);
        if (key == null) {
            assertNull(u.getNubKey());
            assertTrue(u.getIssues().contains(NameUsageIssue.BACKBONE_MATCH_NONE));
        } else {
            assertEquals(key, u.getNubKey());
        }
    }

    public static Node parentOf(Node child, Node parent) {
        parent.createRelationshipTo(child, RelType.PARENT_OF);
        return child;
    }

    public static NameUsage usage(String name, String author, Rank rank) {
        NameUsage u = new NameUsage();
        u.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
        u.setScientificName(name + (author == null ? "" : " " + author));
        u.setCanonicalName(name);
        u.setRank(rank);
        return u;
    }
}