package org.gbif.checklistbank.authorship;

import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.GBIFNameParser;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Another autho comparator test that runs over files of names taken from the real GBIF backbone.
 * Each file contains a group of names that share the same terminal epithet within a family.
 * See http://dev.gbif.org/issues/browse/POR-398 for more.
 */
public class BasionymSorterTest {
  private final NameParser parser = new GBIFNameParser();
  private final BasionymSorter sorter = new BasionymSorter();

  @Test
  public void testGroupPlantBasionyms() throws Exception {
    List<ParsedName> names = Lists.newArrayList();

    names.add(parser.parse("Gymnolomia microcephala var. abbreviata (B.L.Rob. & Greenm.) B.L.Rob. & Greenm.", null));
    names.add(parser.parse("Leucheria abbreviata (Bertero) Steud.", null));
    names.add(parser.parse("Centaurea phrygia subsp. abbreviata (K. Koch) Dostál", null));
    names.add(parser.parse("Centaurea abbreviata (K.Koch) Hand.-Mazz.", null));
    names.add(parser.parse("Jacea abbreviata (K.Koch) Soják", null));
    names.add(parser.parse("Artemisia abbreviata (Krasch. ex Korobkov) Krasnob.", null));
    names.add(parser.parse("Artemisia lagopus subsp. abbreviata Krasch. ex Korobkov", null));
    names.add(parser.parse("Bigelowia leiosperma var. abbreviata M.E.Jones", null));
    names.add(parser.parse("Brickellia oblongifolia var. abbreviata A.Gray", null));
    names.add(parser.parse("Calea abbreviata Pruski & Urbatsch", null));
    names.add(parser.parse("Centaurea salicifolia subsp. abbreviata K. Koch", null));
    names.add(parser.parse("Chabraea abbreviata Colla ex Bertero", null));
    names.add(parser.parse("Chaetanthera stuebelii Hieron. var. abbreviata Cabrera", null));
    names.add(parser.parse("Conyza abbreviata Wall.", null));
    names.add(parser.parse("Cousinia abbreviata Tscherneva", null));
    names.add(parser.parse("Gymnolomia patens var. abbreviata B.L.Rob. & Greenm.", null));
    names.add(parser.parse("Gynura abbreviata F.G.Davies", null));
    names.add(parser.parse("Jacea abbreviata subsp. abbreviata", null));
    names.add(parser.parse("Nassauvia abbreviata Dusén", null));
    names.add(parser.parse("Nassauvia abbreviata var. abbreviata", null));
    names.add(parser.parse("Scorzonera latifolia var. abbreviata Lipsch.", null));
    names.add(parser.parse("Vernonia abbreviata DC.", null));

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(4, groups.size());
    for (BasionymGroup<ParsedName> g : groups) {
      assertFalse(g.getRecombinations().isEmpty());
      switch (g.getRecombinations().get(0).getBracketAuthorship()) {
        case "B.L.Rob. & Greenm.":
          assertEquals(1, g.getRecombinations().size());
          assertNotNull(g.getBasionym());
          break;
        case "Bertero":
          assertEquals(1, g.getRecombinations().size());
          assertNotNull(g.getBasionym());
          break;
        case "K. Koch":
          assertEquals(3, g.getRecombinations().size());
          assertNotNull(g.getBasionym());
          break;
        case "Krasch. ex Korobkov":
          assertEquals(1, g.getRecombinations().size());
          assertNotNull(g.getBasionym());
          break;
        default:
          fail("Unknown basionym group " + g.getRecombinations().get(0));
      }
    }
  }

  /**
   * Here we have a real case from the Asteraceae where 2 different authors with the same surname exist.
   * A.Nelson and E.E.Nelson must be kept separate!
   * http://kiki.huh.harvard.edu/databases/botanist_search.php?botanistid=628
   * http://kiki.huh.harvard.edu/databases/botanist_search.php?botanistid=519
   */
  @Test
  public void testGroupPlantBasionyms2() throws Exception {
    List<ParsedName> names = Lists.newArrayList();

    names.add(parser.parse("Triniteurybia aberrans (A. Nelson) Brouillet, Urbatsch & R.P. Roberts", null));
    names.add(parser.parse("Haplopappus aberrans (A.Nelson) H.M.Hall", null));
    names.add(parser.parse("Sideranthus aberrans (A.Nelson) Rydb.", null));
    names.add(parser.parse("Tonestus aberrans (A.Nelson) G.L.Nesom & D.R.Morgan", null));
    names.add(parser.parse("Hysterionica aberrans (Cabrera) Cabrera", null));
    names.add(parser.parse("Antennaria luzuloides ssp. aberrans (E.E. Nelson) Bayer & Stebbins", null));
    names.add(parser.parse("Logfia aberrans (Wagenitz) Anderb.", null));
    names.add(parser.parse("Antennaria argentea subsp. aberrans", null));
    names.add(parser.parse("Filago aberrans Wagenitz", null));
    names.add(parser.parse("Hysterionica aberrans var. aberrans", null));
    names.add(parser.parse("Hysterionica bakeri var. aberrans Cabrera", null));
    names.add(parser.parse("Macronema aberrans A.Nelson", null));
    names.add(parser.parse("Senecio aberrans Greenm.", null));
    names.add(parser.parse("Taraxacum aberrans Hagend. & al.", null));

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(4, groups.size());
    for (BasionymGroup<ParsedName> g : groups) {
      assertFalse(g.getRecombinations().isEmpty());
      switch (g.getRecombinations().get(0).getBracketAuthorship()) {
        case "A. Nelson":
          assertEquals(4, g.getRecombinations().size());
          assertNotNull(g.getBasionym());
          break;
        case "Cabrera":
          assertEquals(1, g.getRecombinations().size());
          assertNotNull(g.getBasionym());
          break;
        case "E.E. Nelson":
          assertEquals(1, g.getRecombinations().size());
          assertNull(g.getBasionym());
          break;
        case "Wagenitz":
          assertEquals(1, g.getRecombinations().size());
          assertNotNull(g.getBasionym());
          break;
        default:
          fail("Unknown basionym group " + g.getRecombinations().get(0));
      }
    }
  }

  @Test
  public void testGroupPlantBasionyms3() throws Exception {
    List<ParsedName> names = Lists.newArrayList();

    names.add( parser.parse("Negundo aceroides subsp. violaceus (G.Kirchn.) W.A.Weber", null) );
    names.add( parser.parse("Negundo aceroides subsp. violaceus (Kirchner) W.A. Weber", null) );

    names.add(parser.parse("Negundo aceroides subsp. violaceum (Booth ex G.Kirchn.) Holub", null));
    names.add(parser.parse("Negundo aceroides subsp. violaceum (Booth ex Kirchner) Holub", null));

    names.add(parser.parse("Negundo aceroides var. violaceum G.Kirchn. in Petzold & G.Kirchn.", null));
    names.add(parser.parse("Acer violaceum (Kirchner) Simonkai", null));
    names.add(parser.parse("Acer negundo var. violaceum (G. Kirchn.) H. Jaeger", null));

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(1, groups.size());
    BasionymGroup<ParsedName> g = groups.iterator().next();
    assertFalse(g.getRecombinations().isEmpty());
    assertEquals(6, g.getRecombinations().size());
    assertNotNull(g.getBasionym());
    assertEquals("G.Kirchn. in Petzold & G.Kirchn.", g.getBasionym().authorshipComplete());
  }

  @Test
  public void testGroupWithDifferentInitials() throws Exception {
    List<ParsedName> names = Lists.newArrayList();

    names.add(parser.parse("Negundo aceroides subsp. violaceum (Booth ex G.Kirchn.) Holub", null));
    names.add(parser.parse("Negundo aceroides subsp. violaceum (Booth ex Kirchn.) Holub", null));

    names.add(parser.parse("Negundo aceroides var. violaceum G.Kirchn. in Petzold & G.Kirchn.", null));
    names.add(parser.parse("Acer violaceum (T.Kirchn.) Simonkai", null));
    names.add(parser.parse("Acer negundo var. violaceum (G. Kirchn.) H. Jaeger", null));

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(3, groups.size());
    for (BasionymGroup<ParsedName> g : groups) {
      assertFalse(g.getRecombinations().isEmpty());
      switch (g.getRecombinations().get(0).getBracketAuthorship()) {
        case "Booth ex G.Kirchn.":
          assertEquals(2, g.getRecombinations().size());
          assertNotNull(g.getBasionym());
          break;
        case "T.Kirchn.":
          // author comparison has to be very strict and must treat different initials as relevant
          assertEquals(1, g.getRecombinations().size());
          assertNull(g.getBasionym());
          break;
        case "Booth ex Kirchn.":
          // Kirchn. is the abbreviation for Emil Otto Oskar Kirchner
          assertEquals(1, g.getRecombinations().size());
          assertNull(g.getBasionym());
          break;
        default:
          fail("Unknown basionym group " + g.getRecombinations().get(0));
      }
    }
  }

  @Test
  public void testGroupAuthorTeams() throws Exception {
    List<ParsedName> names = Lists.newArrayList();

    names.add(parser.parse("Negundo aceroides var. californicum (Torr. & A.Gray) Sarg.", null));
    names.add(parser.parse("Acer negundo var. californicum (Torr. & Gray) Sarg.", null));
    names.add(parser.parse("Acer californicum Torr et Gray", null));

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(1, groups.size());
    BasionymGroup<ParsedName> g = groups.iterator().next();
    assertEquals(2, g.getRecombinations().size());
    assertEquals("Acer californicum Torr et Gray", g.getBasionym().getScientificName());
  }

  @Test
  public void testAtrocincta() throws Exception {
    List<ParsedName> names = Lists.newArrayList();

    names.add(parser.parse("Anthophora atrocincta Lepeletier, 1841", Rank.SPECIES));
    names.add(parser.parse("Amegilla atrocincta (Lepeletier)", Rank.SPECIES));

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(1, groups.size());
    BasionymGroup<ParsedName> g = groups.iterator().next();
    assertEquals(1, g.getRecombinations().size());
    assertEquals("Anthophora atrocincta Lepeletier, 1841", g.getBasionym().getScientificName());
  }

  @Test
  public void testPlumipes() throws Exception {
    List<ParsedName> names = Lists.newArrayList();

    names.add(parser.parse("Anthophora plumipes (Fabricius)", Rank.SPECIES));
    names.add(parser.parse("Apis plumipes Fabricius, 1781", Rank.SPECIES));
    names.add(parser.parse("Centris plumipes (Fabricius)", Rank.SPECIES));

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(1, groups.size());
    BasionymGroup<ParsedName> g = groups.iterator().next();
    assertEquals(2, g.getRecombinations().size());
    assertEquals("Apis plumipes Fabricius, 1781", g.getBasionym().getScientificName());
  }

  /**
   * Test what happens if a group contains 2 or more basionyms.
   */
  @Test
  public void testMultipleBasionyms() throws Exception {
    List<ParsedName> names = Lists.newArrayList();

    names.add(parser.parse("Negundo violaceum G.Kirchn.", null));
    names.add(parser.parse("Negundo aceroides var. violaceum G.Kirchn. in Petzold & G.Kirchn.", null));
    names.add(parser.parse("Acer violaceum (G Kirchn.) Simonkai", null));
    names.add(parser.parse("Acer negundo var. violaceum (G. Kirchn.) H. Jaeger", null));

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertTrue(groups.isEmpty());
  }

  @Test
  public void testGroupAnimalBasionyms() throws Exception {
    List<ParsedName> names = Lists.newArrayList();

    names.add(parser.parse("Microtus parvulus (A. H. Howell, 1916)", null));
    names.add(parser.parse("Microtus pinetorum parvulus (A. H. Howell, 1916)", null));
    names.add(parser.parse("Pitymys parvulus A. H. Howell, 1916", null));

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(1, groups.size());
    BasionymGroup<ParsedName> g = groups.iterator().next();
    assertEquals(2, g.getRecombinations().size());
    assertNotNull(g.getBasionym());
    assertEquals("A. H. Howell", g.getBasionym().getAuthorship());
    assertEquals("1916", g.getBasionym().getYear());
  }

  @Test
  public void testGroupAnimalBasionyms2() throws Exception {
    List<ParsedName> names = Lists.newArrayList();

    names.add(parser.parse("Heliodoxa rubinoides aequatorialis (Gould, 1860)", null));
    names.add(parser.parse("Androdon aequatorialis Gould, 1863", null));
    names.add(parser.parse("Campylopterus largipennis aequatorialis Gould, 1861", null));

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    // multiple basionyms, no clear group!
    assertEquals(0, groups.size());
  }

  @Test
  /**
   * create test files from current nub with this SQL:
   * \copy (select coalesce(infra_specific_epithet, specific_epithet) as epi, scientific_name from name_usage u join name n on name_fk=n.id where u.dataset_key='d7dddbf4-2cf0-4f39-9b2a-bb099caae36c' and u.family_fk=5386 order by 1,2) to 'fabaceae.txt'
   * \copy (select coalesce(infra_specific_epithet, specific_epithet) as epi, scientific_name from name_usage u join name n on name_fk=n.id where u.dataset_key='d7dddbf4-2cf0-4f39-9b2a-bb099caae36c' and u.family_fk=3065 order by 1,2) to 'asteraceae.txt'
   * \copy (select coalesce(infra_specific_epithet, specific_epithet) as epi, scientific_name from name_usage u join name n on name_fk=n.id where u.dataset_key='d7dddbf4-2cf0-4f39-9b2a-bb099caae36c' and u.family_fk=212 order by 1,2) to 'aves.txt'
   * \copy (select coalesce(infra_specific_epithet, specific_epithet) as epi, scientific_name from name_usage u join name n on name_fk=n.id where u.dataset_key='d7dddbf4-2cf0-4f39-9b2a-bb099caae36c' and u.family_fk=5719 order by 1,2) to 'molossidae.txt'
   * \copy (select coalesce(infra_specific_epithet, specific_epithet) as epi, scientific_name from name_usage u join name n on name_fk=n.id where u.dataset_key='d7dddbf4-2cf0-4f39-9b2a-bb099caae36c' and u.family_fk=5510 order by 1,2) to 'muridae.txt'
   */
  public void testGroupBasionymFiles() throws Exception {
    assertInRage(70, 90, "molossidae.txt");
    assertInRage(450, 470, "muridae.txt");
    assertInRage(10150, 10250, "aves.txt");
    assertInRage(14700, 14800, "fabaceae.txt");
    assertInRage(22650, 22750, "asteraceae.txt");
  }

  private void assertInRage(int min, int max, String filename) throws Exception {
    int count = testGroupBasionymFile("names/"+filename);
    assertTrue(filename + " with too little basionym groups", min <= count);
    assertTrue(filename + " with too many basionym groups", max >= count);
  }

  private int testGroupBasionymFile(String filename) throws Exception {
    int epithets = 0;
    int basionyms = 0;

    EpithetGroupIterator iter = new EpithetGroupIterator(Resources.getResource(filename).openStream());
    while (iter.hasNext()) {
      List<ParsedName> names = iter.next();
      Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
      //String epithet = names.get(0).getInfraSpecificEpithet() == null ? names.get(0).getSpecificEpithet() : names.get(0).getInfraSpecificEpithet();
      //System.out.println(groups.size() + " groups for " + epithet);
      epithets++;
      basionyms = basionyms + groups.size();
    }
    System.out.println("\n\n" + basionyms + " basionym groups found in " + epithets + " epithet groups for file " + filename);
    return basionyms;
  }

  class EpithetGroupIterator implements Iterator<List<ParsedName>> {
    private final CSVReader reader;
    private List<ParsedName> next;
    private String[] lastRow = null;

    EpithetGroupIterator(InputStream names) throws IOException {
      reader = CSVReaderFactory.buildTabReader(names, "UTF8", 0);
      next = readNextGroup();
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public List<ParsedName> next() {
      List<ParsedName> curr = next;
      next = readNextGroup();
      return curr;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    private List<ParsedName> readNextGroup() {
      List<ParsedName> names = Lists.newArrayList();
      String epithet = null;

      while (reader.hasNext() || lastRow != null) {
        String[] row;
        if (lastRow != null) {
          row = lastRow;
          lastRow = null;
        } else {
          row = reader.next();
        }
        if (row == null || row.length < 2 || row[1].startsWith("?")) {
          // ignore basionym placeholders (?)
          continue;
        }

        try {
          ParsedName p = parser.parse(row[1], null);
          if (epithet != null && !epithet.trim().equalsIgnoreCase(row[0])) {
            // a new group, store this row for next call
            lastRow = row;
            return names.isEmpty() ? null : names;
          }
          if (epithet == null) {
            epithet = row[0];
          }
          names.add(p);

        } catch (UnparsableException e) {
          if (e.type.isParsable()) {
            throw new RuntimeException(e);
          }
        }

      }
      return null;
    }
  }
}