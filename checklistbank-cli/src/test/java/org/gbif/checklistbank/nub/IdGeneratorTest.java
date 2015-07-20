package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.nub.lookup.IdLookupTest;

import com.google.common.io.Files;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IdGeneratorTest {

    @Test
    public void testIssueId() throws Exception {
        IdGenerator gen = new IdGenerator(IdLookupTest.newTestLookup(), 1000, Files.createTempDir());
        assertEquals(1000, gen.issue("Dracula", null, null, Rank.GENUS, Kingdom.PLANTAE));
        assertEquals(1, gen.issue("Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA));
        // was issued already!
        assertEquals(1001, gen.issue("Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA));
    }

    @Test
    public void testMetrics() throws Exception {
        IdGenerator gen = new IdGenerator(IdLookupTest.newTestLookup(), 1000, Files.createTempDir());
        assertEquals(1000, gen.issue("Dracula", null, null, Rank.GENUS, Kingdom.PLANTAE));
        assertEquals(1, gen.issue("Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA));
        // was issued already!
        assertEquals(1001, gen.issue("Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA));
        IdGenerator.Metrics m = gen.metrics();
        assertEquals(2, m.created.size());
        assertEquals(6, m.deleted.size());
        assertEquals(0, m.resurrected.size());
        assertTrue(m.created.contains(1001));

        assertEquals(2, gen.issue("Oenanthe", "Vieillot", "1816", Rank.GENUS, Kingdom.ANIMALIA));
        assertEquals(9, gen.issue("Abies alba", null, null, Rank.SPECIES, Kingdom.PLANTAE));
        assertEquals(8, gen.issue("Rodentia", "Bowdich", null, Rank.GENUS, Kingdom.ANIMALIA));
        assertEquals(12, gen.issue("Picea alba", null, null, Rank.SPECIES, Kingdom.PLANTAE));
        m = gen.metrics();
        assertEquals(2, m.created.size());
        assertEquals(4, m.deleted.size());
        assertEquals(2, m.resurrected.size());
        assertTrue(m.resurrected.contains(12));
    }
}