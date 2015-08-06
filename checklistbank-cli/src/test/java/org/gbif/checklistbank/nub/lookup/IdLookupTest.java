package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

import com.beust.jcommander.internal.Lists;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IdLookupTest {
    IdLookup l;

    @Before
    public void init() {
        l = newTestLookup();
    }

    public static IdLookup newTestLookup(){
        Collection<LookupUsage> usages = Lists.newArrayList(
                new LookupUsage(1, "Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA, false),
                new LookupUsage(2, "Oenanthe", "Vieillot", "1816", Rank.GENUS, Kingdom.ANIMALIA, false),
                new LookupUsage(3, "Oenanthe", "Linnaeus", "1753", Rank.GENUS, Kingdom.PLANTAE, false),
                new LookupUsage(4, "Oenanthe aquatica", "Poir.", null, Rank.SPECIES, Kingdom.PLANTAE, false),
                new LookupUsage(5, "Oenanthe aquatica", "Senser", "1957", Rank.SPECIES, Kingdom.PLANTAE, false),
                new LookupUsage(6, "Oenanthe aquatica", null, null, Rank.SPECIES, Kingdom.PLANTAE, true),
                new LookupUsage(7, "Rodentia", "Bowdich", "1821", Rank.ORDER, Kingdom.ANIMALIA, false),
                new LookupUsage(8, "Rodentia", null, null, Rank.GENUS, Kingdom.ANIMALIA, true),
                new LookupUsage(9, "Abies alba", null, null, Rank.SPECIES, Kingdom.PLANTAE, false),
                new LookupUsage(10, "Abies alba", "Mumpf.", null, Rank.SPECIES, Kingdom.PLANTAE, true),
                new LookupUsage(11, "Abies alba", null, "1778", Rank.SPECIES, Kingdom.PLANTAE, true),
                new LookupUsage(12, "Picea alba", null, "1778", Rank.SPECIES, Kingdom.PLANTAE, true)
        );
        return new IdLookupImpl(usages);
    }

    @Test
    public void testLookup() throws IOException, SQLException {
        assertEquals(12, l.size());
        assertEquals(1, l.match("Animalia", Rank.KINGDOM, Kingdom.ANIMALIA).getKey());

        assertEquals(7, l.match("Rodentia", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertNull(l.match("Rodentia", Rank.FAMILY, Kingdom.ANIMALIA));
        assertNull(l.match("Rodentia", Rank.ORDER, Kingdom.PLANTAE));
        assertNull(l.match("Rodenti", Rank.ORDER, Kingdom.ANIMALIA));

        assertEquals(7, l.match("Rodentia", "Bowdich", "1821", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(7, l.match("Rodentia", "Bowdich", "1221", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(7, l.match("Rodentia", "Bowdich", null, Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(7, l.match("Rodentia", null, "1821", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(7, l.match("Rodentia", "Bow.", null, Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(7, l.match("Rodentia", "Bow", "1821", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(7, l.match("Rodentia", "B", "1821", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertNull(l.match("Rodentia", "Mill.", "1823", Rank.ORDER, Kingdom.ANIMALIA));

        assertEquals(2, l.match("Oenanthe", null, null, Rank.GENUS, Kingdom.ANIMALIA).getKey());
        assertEquals(2, l.match("Oenanthe", "Vieillot", null, Rank.GENUS, Kingdom.ANIMALIA).getKey());
        assertEquals(2, l.match("Oenanthe", "V", null, Rank.GENUS, Kingdom.ANIMALIA).getKey());
        assertEquals(2, l.match("Oenanthe", "Vieillot", null, Rank.GENUS, Kingdom.INCERTAE_SEDIS).getKey());
        assertEquals(3, l.match("Oenanthe", null, null, Rank.GENUS, Kingdom.PLANTAE).getKey());
        assertNull(l.match("Oenanthe", null, null, Rank.GENUS, Kingdom.INCERTAE_SEDIS));
        assertNull(l.match("Oenanthe", "Camelot", null, Rank.GENUS, Kingdom.ANIMALIA));

        assertEquals(4, l.match("Oenanthe aquatica", "Poir", null, Rank.SPECIES, Kingdom.PLANTAE).getKey());
        assertNull(l.match("Oenanthe aquatica", null, null, Rank.SPECIES, Kingdom.PLANTAE));
        assertNull(l.match("Oenanthe aquatica", null, null, Rank.SPECIES, Kingdom.FUNGI));

        // it is allowed to add an author to the single current canonical name if it doesnt have an author yet!
        assertEquals(9, l.match("Abies alba", null, null, Rank.SPECIES, Kingdom.PLANTAE).getKey());
        assertEquals(9, l.match("Abies alba", null, "1789", Rank.SPECIES, Kingdom.PLANTAE).getKey());
        assertEquals(9, l.match("Abies alba", "Mill.", null, Rank.SPECIES, Kingdom.PLANTAE).getKey());
        assertEquals(9, l.match("Abies alba", "Miller", null, Rank.SPECIES, Kingdom.PLANTAE).getKey());
        assertEquals(9, l.match("Abies alba", "Mumpf.", null, Rank.SPECIES, Kingdom.PLANTAE).getKey());
        assertEquals(9, l.match("Abies alba", "Döring", "1778", Rank.SPECIES, Kingdom.PLANTAE).getKey());
    }
}