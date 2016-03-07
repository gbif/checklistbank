package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.LookupUsage;

import java.util.Collection;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IdGeneratorTest {

  public static IdLookup newTestLookup() {
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
        new LookupUsage(12, "Picea alba", null, "1778", Rank.SPECIES, Kingdom.PLANTAE, true),
        new LookupUsage(13, "Picea", null, null, Rank.GENUS, Kingdom.PLANTAE, true),
        new LookupUsage(14, "Carex cayouettei", null, null, Rank.SPECIES, Kingdom.PLANTAE, true),
        new LookupUsage(15, "Carex comosa × Carex lupulina", null, null, Rank.SPECIES, Kingdom.PLANTAE, true),
        new LookupUsage(16, "Aeropyrum coil-shaped virus", null, null, Rank.UNRANKED, Kingdom.VIRUSES, true)
    );
    return IdLookupImpl.temp().load(usages);
  }

  @Test
  public void testIssueId() throws Exception {
    IdGenerator gen = new IdGenerator(newTestLookup(), 1000);
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