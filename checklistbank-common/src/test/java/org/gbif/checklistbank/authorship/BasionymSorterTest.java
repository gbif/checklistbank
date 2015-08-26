package org.gbif.checklistbank.authorship;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Another autho comparator test that runs over files of names taken from the real GBIF backbone.
 * Each file contains a group of names that share the same terminal epithet within a family.
 * See http://dev.gbif.org/issues/browse/POR-398 for more.
 */
public class BasionymSorterTest {
    private final NameParser parser = new NameParser();
    private final BasionymSorter sorter = new BasionymSorter();

    @Test
    public void testGroupPlantBasionyms() throws Exception {
        List<ParsedName> names = Lists.newArrayList();

        names.add( parser.parse("Gymnolomia microcephala var. abbreviata (B.L.Rob. & Greenm.) B.L.Rob. & Greenm.", null) );
        names.add( parser.parse("Leucheria abbreviata (Bertero) Steud.", null) );
        names.add( parser.parse("Centaurea phrygia subsp. abbreviata (K. Koch) Dostál", null) );
        names.add( parser.parse("Centaurea abbreviata (K.Koch) Hand.-Mazz.", null) );
        names.add( parser.parse("Jacea abbreviata (K.Koch) Soják", null) );
        names.add( parser.parse("Artemisia abbreviata (Krasch. ex Korobkov) Krasnob.", null) );
        names.add( parser.parse("Artemisia lagopus subsp. abbreviata Krasch. ex Korobkov", null) );
        names.add( parser.parse("Bigelowia leiosperma var. abbreviata M.E.Jones", null) );
        names.add( parser.parse("Brickellia oblongifolia var. abbreviata A.Gray", null) );
        names.add( parser.parse("Calea abbreviata Pruski & Urbatsch", null) );
        names.add( parser.parse("Centaurea salicifolia subsp. abbreviata K. Koch", null) );
        names.add( parser.parse("Chabraea abbreviata Bertero ex Colla", null) );
        names.add( parser.parse("Chaetanthera stuebelii Hieron. var. abbreviata Cabrera", null) );
        names.add( parser.parse("Conyza abbreviata Wall.", null) );
        names.add( parser.parse("Cousinia abbreviata Tscherneva", null) );
        names.add( parser.parse("Gymnolomia patens var. abbreviata B.L.Rob. & Greenm.", null) );
        names.add( parser.parse("Gynura abbreviata F.G.Davies", null) );
        names.add( parser.parse("Jacea abbreviata subsp. abbreviata", null) );
        names.add( parser.parse("Nassauvia abbreviata Dusén", null) );
        names.add( parser.parse("Nassauvia abbreviata var. abbreviata", null) );
        names.add( parser.parse("Scorzonera latifolia var. abbreviata Lipsch.", null) );
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
     *
     * http://kiki.huh.harvard.edu/databases/botanist_search.php?botanistid=628
     * http://kiki.huh.harvard.edu/databases/botanist_search.php?botanistid=519
     */
    @Test
    public void testGroupPlantBasionyms2() throws Exception {
        List<ParsedName> names = Lists.newArrayList();

        names.add( parser.parse("Triniteurybia aberrans (A. Nelson) Brouillet, Urbatsch & R.P. Roberts", null) );
        names.add( parser.parse("Haplopappus aberrans (A.Nelson) H.M.Hall", null) );
        names.add( parser.parse("Sideranthus aberrans (A.Nelson) Rydb.", null) );
        names.add( parser.parse("Tonestus aberrans (A.Nelson) G.L.Nesom & D.R.Morgan", null) );
        names.add( parser.parse("Hysterionica aberrans (Cabrera) Cabrera", null) );
        names.add( parser.parse("Antennaria luzuloides ssp. aberrans (E.E. Nelson) Bayer & Stebbins", null) );
        names.add( parser.parse("Logfia aberrans (Wagenitz) Anderb.", null) );
        names.add( parser.parse("Antennaria argentea subsp. aberrans", null) );
        names.add( parser.parse("Filago aberrans Wagenitz", null) );
        names.add( parser.parse("Hysterionica aberrans var. aberrans", null) );
        names.add( parser.parse("Hysterionica bakeri var. aberrans Cabrera", null) );
        names.add( parser.parse("Macronema aberrans A.Nelson", null) );
        names.add( parser.parse("Senecio aberrans Greenm.", null) );
        names.add( parser.parse("Taraxacum aberrans Hagend. & al.", null) );

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
    public void testGroupAnimalBasionyms() throws Exception {
        List<ParsedName> names = Lists.newArrayList();

        names.add( parser.parse("Microtus parvulus (A. H. Howell, 1916)", null) );
        names.add( parser.parse("Microtus pinetorum parvulus (A. H. Howell, 1916)", null) );
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

        names.add( parser.parse("Heliodoxa rubinoides aequatorialis (Gould, 1860)", null) );
        names.add( parser.parse("Androdon aequatorialis Gould, 1863", null) );
        names.add(parser.parse("Campylopterus largipennis aequatorialis Gould, 1861", null));

        Collection<BasionymGroup<ParsedName>> groups = sorter.groupBasionyms(names);
        assertEquals(1, groups.size());
        BasionymGroup<ParsedName> g = groups.iterator().next();
        assertEquals(1, g.getRecombinations().size());
        assertNull(g.getBasionym());
        assertEquals("1860", g.getRecombinations().get(0).getBracketYear());
    }

    @Test
    public void testGroupBasionymFiles() throws Exception {
        assertEquals(52, testGroupBasionymFile("molossidae.txt"));
        assertEquals(317, testGroupBasionymFile("muridae.txt"));
        assertEquals(2840, testGroupBasionymFile("curculionidae.txt"));
        assertEquals(5901, testGroupBasionymFile("aves.txt"));
        assertEquals(22010, testGroupBasionymFile("asteraceae.txt"));
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
            basionyms = basionyms+groups.size();
        }
        System.out.println("\n\n" + basionyms + " basionym groups found in " + epithets + " epithet groups for file " + filename);
        return basionyms;
    }

    class EpithetGroupIterator implements Iterator<List<ParsedName>> {
        private final BufferedReader br;
        private List<ParsedName> next;

        EpithetGroupIterator(InputStream names) {
            br = new BufferedReader(new InputStreamReader(names));
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

        private List<ParsedName> readNextGroup() {
            List<ParsedName> names = Lists.newArrayList();
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    if (StringUtils.isBlank(line)) {
                        continue;
                    }
                    if (line.startsWith("#")) {
                        if (names.isEmpty()) {
                            continue;
                        }
                        return names;
                    }
                    try {
                        ParsedName p = parser.parse(line, null);
                        names.add(p);
                    } catch (UnparsableException e) {
                        throw new RuntimeException(e);
                    }
                }
                return names.isEmpty() ? null : names;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}