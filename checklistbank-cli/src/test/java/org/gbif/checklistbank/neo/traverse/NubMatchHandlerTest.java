package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.model.ParsedNameUsageMatch;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.service.MatchingService;
import org.gbif.nameparser.GBIFNameParser;
import org.gbif.nub.lookup.fuzzy.NubMatchingServiceImpl;

import java.util.Collection;
import java.util.Date;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NubMatchHandlerTest {
  static NameParser parser = new GBIFNameParser();
  static final Date delDate = new Date();

  UsageDao dao = UsageDao.temporaryDao(10);

  public static MatchingService newTestLookup() {
    Collection<ParsedNameUsageMatch<NameUsage>> usages = Lists.newArrayList(
        usage(1, "Animalia", Rank.KINGDOM, Kingdom.ANIMALIA, false),
        usage(2, "Oenanthe Vieillot, 1816", Rank.GENUS, Kingdom.ANIMALIA, false),
        usage(3, "Oenanthe Linnaeus, 1753", Rank.GENUS, Kingdom.PLANTAE, false),
        usage(4, "Oenanthe aquatica Poir.", Rank.SPECIES, Kingdom.PLANTAE, false),
        usage(5, "Oenanthe aquatica Senser, 1957", Rank.SPECIES, Kingdom.PLANTAE, false),
        usage(6, "Oenanthe aquatica", Rank.SPECIES, Kingdom.PLANTAE, true),
        usage(7, "Rodentia Bowdich, 1821", Rank.ORDER, Kingdom.ANIMALIA, false),
        usage(8, "Rodentia", Rank.GENUS, Kingdom.ANIMALIA, true),
        usage(9, "Abies alba", Rank.SPECIES, Kingdom.PLANTAE, false),
        usage(10, "Abies alba Mumpf.", Rank.SPECIES, Kingdom.PLANTAE, true),
        usage(11, "Abies alba 1778", Rank.SPECIES, Kingdom.PLANTAE, true),
        usage(12, "Picea alba 1778", Rank.SPECIES, Kingdom.PLANTAE, true),
        usage(13, "Picea", Rank.GENUS, Kingdom.PLANTAE, true),
        usage(14, "Carex cayouettei", Rank.SPECIES, Kingdom.PLANTAE, true),
        usage(15, "Carex comosa × Carex lupulina", Rank.SPECIES, Kingdom.PLANTAE, true),
        usage(16, "Aeropyrum coil-shaped virus", Rank.UNRANKED, Kingdom.VIRUSES, true)
    );
    return NubMatchingServiceImpl.strictMatchingIndex(usages);
  }

  static ParsedNameUsageMatch<NameUsage> usage(int key, String sciname, Rank rank, Kingdom kingdom, boolean deleted) {
    ParsedName pn = parser.parseQuietly(sciname, rank);

    NameUsage u = new NameUsage();
    u.setKey(key);
    u.setScientificName(sciname);
    u.setCanonicalName(pn.canonicalName());
    u.setRank(rank);
    u.setKingdom(kingdom.scientificName());
    if (deleted) {
      u.setDeleted(delDate);
    }

    return new ParsedNameUsageMatch<NameUsage>(pn, u);
  }

  @Test
    public void testMatching() throws Exception {
        MatchingService lookup = newTestLookup();
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