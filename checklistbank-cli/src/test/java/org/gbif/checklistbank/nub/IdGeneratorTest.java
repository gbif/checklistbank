package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.nub.lookup.IdLookupImplTest;

import com.google.common.io.Files;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IdGeneratorTest {

    @Test
    public void testIssueId() throws Exception {
        IdGenerator gen = new IdGenerator(IdLookupImplTest.newTestLookup(), 1000, Files.createTempDir());
        assertEquals(1000, gen.issue("Dracula", null, null, Rank.GENUS, Kingdom.PLANTAE, false));
        assertEquals(1, gen.issue("Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA, false));
        // was issued already!
        assertEquals(1001, gen.issue("Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA, false));
        // force new!
        assertEquals(1002, gen.issue("Carex cayouettei", null, null, Rank.SPECIES, Kingdom.PLANTAE, true));
        assertEquals(1003, gen.issue("Rodentia", null, null, Rank.GENUS, Kingdom.ANIMALIA, true));
        // allow old
        assertEquals(14, gen.issue("Carex cayouettei", null, null, Rank.SPECIES, Kingdom.PLANTAE, false));
    }

    @Test
    public void testMetrics() throws Exception {
        IdGenerator gen = new IdGenerator(IdLookupImplTest.newTestLookup(), 1000, Files.createTempDir());
        assertEquals(1000, gen.issue("Dracula", null, null, Rank.GENUS, Kingdom.PLANTAE, false));
        assertEquals(1, gen.issue("Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA, false));
        // was issued already!
        assertEquals(1001, gen.issue("Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA, false));
        IdGenerator.Metrics m = gen.metrics();
        assertEquals(2, m.created.size());
        assertEquals(6, m.deleted.size());
        assertEquals(0, m.resurrected.size());
        assertTrue(m.created.contains(1001));

        assertEquals(2, gen.issue("Oenanthe", "Vieillot", "1816", Rank.GENUS, Kingdom.ANIMALIA, false));
        assertEquals(9, gen.issue("Abies alba", null, null, Rank.SPECIES, Kingdom.PLANTAE, false));
        assertEquals(8, gen.issue("Rodentia", "Bowdich", null, Rank.GENUS, Kingdom.ANIMALIA, false));
        assertEquals(12, gen.issue("Picea alba", null, null, Rank.SPECIES, Kingdom.PLANTAE, false));
        m = gen.metrics();
        assertEquals(2, m.created.size());
        assertEquals(4, m.deleted.size());
        assertEquals(2, m.resurrected.size());
        assertTrue(m.resurrected.contains(12));
    }
}