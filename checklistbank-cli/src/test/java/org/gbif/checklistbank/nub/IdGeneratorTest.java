package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.nub.lookup.IdLookupImplTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IdGeneratorTest {

    @Test
    public void testIssueId() throws Exception {
        IdGenerator gen = new IdGenerator(IdLookupImplTest.newTestLookup(), 1000);
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

}