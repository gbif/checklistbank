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
package org.gbif.checklistbank.authorship;

import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.NameParserGbifV1;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Another autho comparator test that runs over files of names taken from the real GBIF backbone.
 * Each file contains a group of names that share the same terminal epithet within a family.
 * See http://dev.gbif.org/issues/browse/POR-398 for more.
 */
public class BasionymSorterTest {
  private final NameParser parser = new NameParserGbifV1();
  private final BasionymSorter sorter = new BasionymSorter();

  private List<ParsedName> names(String... names) throws Exception {
    return Arrays.stream(names)
        .map(n -> {
          try {
            return parser.parse(n);
          } catch (UnparsableException e) {
            Throwables.propagate(e);
          }
          return null;
        })
        .collect(Collectors.toList());
  }

  @Test
  public void testGroupPlantBasionyms() throws Exception {
    List<ParsedName> names = names(
        "Gymnolomia microcephala var. abbreviata (B.L.Rob. & Greenm.) B.L.Rob. & Greenm.",
        "Leucheria abbreviata (Bertero) Steud.",
        "Centaurea phrygia subsp. abbreviata (K. Koch) Dostál",
        "Centaurea abbreviata (K.Koch) Hand.-Mazz.",
        "Jacea abbreviata (K.Koch) Soják",
        "Artemisia abbreviata (Krasch. ex Korobkov) Krasnob.",
        "Artemisia lagopus subsp. abbreviata Krasch. ex Korobkov",
        "Bigelowia leiosperma var. abbreviata M.E.Jones",
        "Brickellia oblongifolia var. abbreviata A.Gray",
        "Calea abbreviata Pruski & Urbatsch",
        "Centaurea salicifolia subsp. abbreviata K. Koch",
        "Chabraea abbreviata Colla ex Bertero",
        "Chaetanthera stuebelii Hieron. var. abbreviata Cabrera",
        "Conyza abbreviata Wall.",
        "Cousinia abbreviata Tscherneva",
        "Gymnolomia patens var. abbreviata B.L.Rob. & Greenm.",
        "Gynura abbreviata F.G.Davies",
        "Jacea abbreviata subsp. abbreviata",
        "Nassauvia abbreviata Dusén",
        "Nassauvia abbreviata var. abbreviata",
        "Scorzonera latifolia var. abbreviata Lipsch.",
        "Vernonia abbreviata DC."
    );

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
        case "K.Koch":
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
    List<ParsedName> names = names(
        "Triniteurybia aberrans (A. Nelson) Brouillet, Urbatsch & R.P. Roberts",
        "Haplopappus aberrans (A.Nelson) H.M.Hall",
        "Sideranthus aberrans (A.Nelson) Rydb.",
        "Tonestus aberrans (A.Nelson) G.L.Nesom & D.R.Morgan",
        "Hysterionica aberrans (Cabrera) Cabrera",
        "Antennaria luzuloides ssp. aberrans (E.E. Nelson) Bayer & Stebbins",
        "Logfia aberrans (Wagenitz) Anderb.",
        "Antennaria argentea subsp. aberrans",
        "Filago aberrans Wagenitz",
        "Hysterionica aberrans var. aberrans",
        "Hysterionica bakeri var. aberrans Cabrera",
        "Macronema aberrans A.Nelson",
        "Senecio aberrans Greenm.",
        "Taraxacum aberrans Hagend. & al."
    );

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(4, groups.size());
    for (BasionymGroup<ParsedName> g : groups) {
      assertFalse(g.getRecombinations().isEmpty());
      switch (g.getRecombinations().get(0).getBracketAuthorship()) {
        case "A.Nelson":
          assertEquals(4, g.getRecombinations().size());
          assertNotNull(g.getBasionym());
          break;
        case "Cabrera":
          assertEquals(1, g.getRecombinations().size());
          assertNotNull(g.getBasionym());
          break;
        case "E.E.Nelson":
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
    List<ParsedName> names = names(
        "Negundo aceroides subsp. violaceus (G.Kirchn.) W.A.Weber",
        "Negundo aceroides subsp. violaceus (Kirchner) W.A. Weber",

        "Negundo aceroides subsp. violaceum (Booth ex G.Kirchn.) Holub",
        "Negundo aceroides subsp. violaceum (Booth ex Kirchner) Holub",

        "Negundo aceroides var. violaceum G.Kirchn. in Petzold & G.Kirchn.",
        "Acer violaceum (Kirchner) Simonkai",
        "Acer negundo var. violaceum (G. Kirchn.) H. Jaeger"
    );

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(1, groups.size());
    BasionymGroup<ParsedName> g = groups.iterator().next();
    assertFalse(g.getRecombinations().isEmpty());
    assertEquals(6, g.getRecombinations().size());
    assertNotNull(g.getBasionym());
    assertEquals("G.Kirchn.", g.getBasionym().authorshipComplete());
  }

  @Test
  public void testGroupWithDifferentInitials() throws Exception {
    List<ParsedName> names = names(
        "Negundo aceroides subsp. violaceum (Booth ex G.Kirchn.) Holub",
        "Negundo aceroides subsp. violaceum (Booth ex Kirchn.) Holub",

        "Negundo aceroides var. violaceum G.Kirchn. in Petzold & G.Kirchn.",
        "Acer violaceum (T.Kirchn.) Simonkai",
        "Acer negundo var. violaceum (G. Kirchn.) H. Jaeger"
    );

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
    List<ParsedName> names = names(
        "Negundo aceroides var. californicum (Torr. & A.Gray) Sarg.",
        "Acer negundo var. californicum (Torr. & Gray) Sarg.",
        "Acer californicum Torr et Gray"
    );

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
    List<ParsedName> names = names(
        "Negundo violaceum G.Kirchn.",
        "Negundo aceroides var. violaceum G.Kirchn. in Petzold & G.Kirchn.",
        "Acer violaceum (G Kirchn.) Simonkai",
        "Acer negundo var. violaceum (G. Kirchn.) H. Jaeger"
    );

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertTrue(groups.isEmpty());
  }

  @Test
  public void testGroupAnimalBasionyms() throws Exception {
    List<ParsedName> names = names(
        "Microtus parvulus (A. H. Howell, 1916)",
        "Microtus pinetorum parvulus (A. H. Howell, 1916)",
        "Pitymys parvulus A. H. Howell, 1916"
    );

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    assertEquals(1, groups.size());
    BasionymGroup<ParsedName> g = groups.iterator().next();
    assertEquals(2, g.getRecombinations().size());
    assertNotNull(g.getBasionym());
    assertEquals("A.H.Howell", g.getBasionym().getAuthorship());
    assertEquals("1916", g.getBasionym().getYear());
  }

  @Test
  public void testGroupAnimalBasionyms2() throws Exception {
    List<ParsedName> names = names(
        "Heliodoxa rubinoides aequatorialis (Gould, 1860)",
        "Androdon aequatorialis Gould, 1863",
        "Clementoron aequatorialis Gould, 1864",
        // this one is 1 year apart so it matches the first recombination on top!
        "Campylopterus largipennis aequatorialis Gould, 1861"
    );

    Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
    // multiple basionyms, no clear group!
    assertEquals(1, groups.size());
    BasionymGroup<ParsedName> bg = groups.iterator().next();
    assertEquals("aequatorialis", bg.getEpithet());
    assertEquals("1861", bg.getBasionym().getYear());
    assertEquals("aequatorialis", bg.getBasionym().getInfraSpecificEpithet());
    assertEquals("Gould", bg.getAuthorship());
  }

  @Test
  @Disabled("this takes ages to run and does not test anything specific")
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
    assertInRage(10070, 10080, "aves.txt");
    assertInRage(14700, 14760, "fabaceae.txt");
    assertInRage(22650, 22725, "asteraceae.txt");
  }

  private void assertInRage(int min, int max, String filename) throws Exception {
    int count = testGroupBasionymFile("names/" + filename);
    assertTrue(min <= count, filename + " with too little basionym groups");
    assertTrue(max >= count, filename + " with too many basionym groups");
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