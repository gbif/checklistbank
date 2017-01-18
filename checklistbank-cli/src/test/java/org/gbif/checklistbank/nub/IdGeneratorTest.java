package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.LookupUsage;

import java.util.Collection;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
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
        new LookupUsage(16, "Aeropyrum coil-shaped virus", null, null, Rank.UNRANKED, Kingdom.VIRUSES, true),

        new LookupUsage(17, map(100, 17, 110, 18, 111, 19),  "Admetidae", "Troschel", "1865", Rank.FAMILY, Kingdom.ANIMALIA, false),
        new LookupUsage(20, null, "Admetidae", null, null, Rank.FAMILY, Kingdom.ANIMALIA, true)
    );
    return IdLookupImpl.temp().load(usages);
  }

  /**
   * key, value, key, value, ...
   * pro parte maps have the parent usageKey as key, the pro parte usage key as value
   */
  private static Int2IntMap map(int ... kvs) {
    Int2IntMap m = new Int2IntArrayMap(kvs.length / 2);
    int idx = 0;
    while (idx < kvs.length) {
      m.put(kvs[idx], kvs[idx+1]);
      idx = idx + 2;
    }
    return m;
  }

  @Test
  public void testIssueId() throws Exception {
    IdGenerator gen = new IdGenerator(newTestLookup(), 1000);
    assertEquals(1000, gen.issue("Dracula", null, null, Rank.GENUS, Kingdom.PLANTAE));
    assertEquals(1, gen.issue("Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA));
    assertEquals(8, gen.issue("Rodentia", null, null, Rank.GENUS, Kingdom.ANIMALIA));
    // external issueing
    gen.reissue(14);
    // was issued already!
    assertEquals(1001, gen.issue("Carex cayouettei", null, null, Rank.SPECIES, Kingdom.PLANTAE));
    assertEquals(1002, gen.issue("Animalia", null, null, Rank.KINGDOM, Kingdom.ANIMALIA));
    assertEquals(1003, gen.issue("Carex cayouettei", null, null, Rank.SPECIES, Kingdom.PLANTAE));
  }

  @Test
  public void testProParte() throws Exception {
    IdGenerator gen = new IdGenerator(newTestLookup(), 1000);
    // wrong kingdom
    assertEquals(1000, gen.issue("Admetidae", null, null, Rank.FAMILY, Kingdom.PLANTAE));
    // regular canonical match
    assertEquals(20, gen.issue("Admetidae", null, null, Rank.FAMILY, Kingdom.ANIMALIA));
    // pro parte matching
    assertEquals(17, gen.issue("Admetidae", "Troschel", null, Rank.FAMILY, Kingdom.ANIMALIA, 100));
    assertEquals(18, gen.issue("Admetidae", "Troschel", null, Rank.FAMILY, Kingdom.ANIMALIA, 110));
    assertEquals(19, gen.issue("Admetidae", "Troschel", null, Rank.FAMILY, Kingdom.ANIMALIA, 111));
    assertEquals(1001, gen.issue("Admetidae", "Troschel", null, Rank.FAMILY, Kingdom.ANIMALIA, 200));
  }

}