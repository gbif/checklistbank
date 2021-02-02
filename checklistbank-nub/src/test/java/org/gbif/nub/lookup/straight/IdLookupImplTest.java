package org.gbif.nub.lookup.straight;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

import static org.gbif.api.vocabulary.Kingdom.*;
import static org.gbif.api.vocabulary.Rank.*;
import static org.gbif.api.vocabulary.TaxonomicStatus.ACCEPTED;
import static org.gbif.api.vocabulary.TaxonomicStatus.SYNONYM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IdLookupImplTest {
  IdLookup l;

  @Before
  public void init() {
    l = newTestLookup();
  }

  public static IdLookup newTestLookup() {
    Collection<LookupUsage> usages = Lists.newArrayList(
        new LookupUsage(1,  "Animalia", null, null, KINGDOM, ACCEPTED, ANIMALIA, false),
        new LookupUsage(2,  "Oenanthe", "Vieillot", "1816", GENUS, ACCEPTED, ANIMALIA, false),
        new LookupUsage(3,  "Oenanthe", "Linnaeus", "1753", GENUS, ACCEPTED, PLANTAE, false),
        new LookupUsage(4,  "Oenanthe aquatica", "Poir.", null, SPECIES, ACCEPTED, PLANTAE, false),
        new LookupUsage(5,  "Oenanthe aquatica", "Senser", "1957", SPECIES, ACCEPTED, PLANTAE, false),
        new LookupUsage(6,  "Oenanthe aquatica", null, null, SPECIES, ACCEPTED, PLANTAE, true),
        new LookupUsage(7,  "Rodentia", "Bowdich", "1821", ORDER, ACCEPTED, ANIMALIA, false),
        new LookupUsage(8,  "Rodentia", null, null, GENUS, ACCEPTED, ANIMALIA, true),
        new LookupUsage(9,  "Abies alba", null, null, SPECIES, ACCEPTED, PLANTAE, false),
        new LookupUsage(10, "Abies alba", "Mumpf.", null, SPECIES, ACCEPTED, PLANTAE, true),
        new LookupUsage(11, "Abies alba", null, "1778", SPECIES, ACCEPTED, PLANTAE, true),
        new LookupUsage(12, "Picea alba", null, "1778", SPECIES, ACCEPTED, PLANTAE, true),
        new LookupUsage(13, "Picea", null, null, GENUS, ACCEPTED, PLANTAE, true),
        new LookupUsage(14, "Carex cayouettei", null, null, SPECIES, ACCEPTED, PLANTAE, true),
        new LookupUsage(15, "Carex comosa × Carex lupulina", null, null, SPECIES, ACCEPTED, PLANTAE, true),
        new LookupUsage(16, "Aeropyrum coil-shaped virus", null, null, UNRANKED, ACCEPTED, VIRUSES, true),
        new LookupUsage(17, "BOLD:AAJ6407", null, null, UNRANKED, ACCEPTED, ANIMALIA, false),
        new LookupUsage(18, "SH486523.07FU", null, null, UNRANKED, SYNONYM, FUNGI, false)
    );
    return IdLookupImpl.temp().load(usages);
  }

  @Test
  public void testIterator() throws Exception {
    for (LookupUsage u : l) {
      System.out.println(u);
    }
  }

  @Test
  public void testNorm() throws Exception {
    assertEquals("malvastrum yelow vein virus satelite dna s", IdLookupImpl.norm("Malvastrum yellow vein virus satellite DNA ß"));

    assertNull(IdLookupImpl.norm(null));
    assertNull(IdLookupImpl.norm(""));
    assertNull(IdLookupImpl.norm("  "));
    assertEquals("abies", IdLookupImpl.norm("Abies"));
    assertEquals("abies", IdLookupImpl.norm("ABiES"));
    assertEquals("abies alba", IdLookupImpl.norm("Abiés  alba"));
    assertEquals("albino", IdLookupImpl.norm("Albiño"));
    assertEquals("donatia novaezelandiae", IdLookupImpl.norm("Donatia novae-zelandiæ"));
    assertEquals("abies olsen", IdLookupImpl.norm("Abies ölsen"));
    assertEquals("abies oeftaen", IdLookupImpl.norm("Abies œftæn"));
    assertEquals("carex caioueti", IdLookupImpl.norm("Carex ×cayouettei"));
    assertEquals("carex comosa carex lupulina", IdLookupImpl.norm("Carex comosa × Carex lupulina"));
    assertEquals("aeropyrum coilshaped vira", IdLookupImpl.norm("Aeropyrum coil-shaped virus"));
    assertEquals("†lachnus boneti", IdLookupImpl.norm("†Lachnus bonneti"));
    assertEquals("bold:aaj6407", IdLookupImpl.norm("BOLD:AAJ6407"));
    assertEquals("sh486523.07fu", IdLookupImpl.norm("SH486523.07FU"));
  }

  @Test
  public void testLookup() throws IOException, SQLException {
    assertEquals(18, l.size());
    assertEquals(1, l.match("Animalia", KINGDOM, ANIMALIA).getKey());

    assertEquals(7, l.match("Rodentia", ORDER, ANIMALIA).getKey());
    assertNull(l.match("Rodentia", FAMILY, ANIMALIA));
    assertNull(l.match("Rodentia", ORDER, PLANTAE));
    assertNull(l.match("Rodenti", ORDER, ANIMALIA));

    assertEquals(7, l.match("Rodentia", "Bowdich", "1821", ORDER, ACCEPTED, ANIMALIA).getKey());
    assertEquals(7, l.match("Rodentia", "Bowdich", "1221", ORDER, ACCEPTED, ANIMALIA).getKey());
    assertEquals(7, l.match("Rodentia", "Bowdich", null, ORDER, ACCEPTED, ANIMALIA).getKey());
    assertEquals(7, l.match("Rodentia", null, "1821", ORDER, ACCEPTED, ANIMALIA).getKey());
    assertEquals(7, l.match("Rodentia", "Bow.", null, ORDER, ACCEPTED, ANIMALIA).getKey());
    assertEquals(7, l.match("Rodentia", "Bow", "1821", ORDER, ACCEPTED, ANIMALIA).getKey());
    assertEquals(7, l.match("Rodentia", "B", "1821", ORDER, ACCEPTED, ANIMALIA).getKey());
    assertNull(l.match("Rodentia", "Mill.", "1823", ORDER, ACCEPTED, ANIMALIA));

    assertEquals(2, l.match("Oenanthe", null, null, GENUS, ACCEPTED, ANIMALIA).getKey());
    assertEquals(2, l.match("Oenanthe", "Vieillot", null, GENUS, ACCEPTED, ANIMALIA).getKey());
    assertEquals(2, l.match("Oenanthe", "V", null, GENUS, ACCEPTED, ANIMALIA).getKey());
    assertEquals(2, l.match("Oenanthe", "Vieillot", null, GENUS, ACCEPTED, INCERTAE_SEDIS).getKey());
    assertEquals(3, l.match("Oenanthe", null, null, GENUS, ACCEPTED, PLANTAE).getKey());
    assertEquals(3, l.match("Œnanthe", null, null, GENUS, ACCEPTED, PLANTAE).getKey());
    assertEquals(2, l.match("Oenanthe", null, null, GENUS, ACCEPTED, INCERTAE_SEDIS).getKey());
    assertNull(l.match("Oenanthe", "Camelot", null, GENUS, ACCEPTED, ANIMALIA));

    assertEquals(4, l.match("Oenanthe aquatica", "Poir", null, SPECIES, ACCEPTED, PLANTAE).getKey());
    assertEquals(6, l.match("Oenanthe aquatica", null, null, SPECIES, ACCEPTED, PLANTAE).getKey());
    assertNull(l.match("Oenanthe aquatica", null, null, SPECIES, ACCEPTED, FUNGI));

    // it is allowed to add an author to the single current canonical name if it doesnt have an author yet!
    assertEquals(9, l.match("Abies alba", null, null, SPECIES, ACCEPTED, PLANTAE).getKey());
    assertEquals(9, l.match("Abies alba", null, "1789", SPECIES, ACCEPTED, PLANTAE).getKey());
    assertEquals(9, l.match("Abies alba", "Mill.", null, SPECIES, ACCEPTED, PLANTAE).getKey());
    assertEquals(9, l.match("Abies alba", "Miller", null, SPECIES, ACCEPTED, PLANTAE).getKey());
    assertEquals(11, l.match("Abies alba", "Döring", "1778", SPECIES, ACCEPTED, PLANTAE).getKey());
    assertEquals(10, l.match("Abies alba", "Mumpf.", null, SPECIES, ACCEPTED, PLANTAE).getKey());

    // try unparsable names
    assertEquals(14, l.match("Carex cayouettei", null, null, SPECIES, ACCEPTED, PLANTAE).getKey());
    assertEquals(15, l.match("Carex comosa × Carex lupulina", null, null, SPECIES, ACCEPTED, PLANTAE).getKey());
    assertEquals(16, l.match("Aeropyrum coil-shaped virus", null, null, UNRANKED, ACCEPTED, VIRUSES).getKey());
    assertEquals(16, l.match("Aeropyrum coil-shaped virus", null, null, SPECIES, ACCEPTED, VIRUSES).getKey());
    assertNull(l.match("Aeropyrum coil-shaped virus", null, null, UNRANKED, ACCEPTED, FUNGI));

    // BOLD / SH OTUs
    assertEquals(17, l.match("BOLD:AAJ6407", null, null, SPECIES, ACCEPTED, null).getKey());
    assertEquals(17, l.match("BOLD:AAJ6407", null, null, UNRANKED, ACCEPTED, ANIMALIA).getKey());
    assertNull(l.match("BOLD:AAJ6408", null, null, UNRANKED, ACCEPTED, FUNGI));

    assertEquals(18, l.match("SH486523.07FU", null, null, SPECIES, ACCEPTED, null).getKey());
    assertEquals(18, l.match("SH486523.07FU", null, null, UNRANKED, ACCEPTED, FUNGI).getKey());
    assertNull(l.match("SH486523.01FU", null, null, UNRANKED, ACCEPTED, FUNGI));
    
  }
}