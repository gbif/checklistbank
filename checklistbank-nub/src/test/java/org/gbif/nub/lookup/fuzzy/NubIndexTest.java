package org.gbif.nub.lookup.fuzzy;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.nameparser.NameParserGbifV1;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

public class NubIndexTest {

  private static NubIndex index;

  @BeforeClass
  public static void buildMatcher() throws IOException {
    HigherTaxaComparator syn = new HigherTaxaComparator();
    syn.loadClasspathDicts("dicts");
    index = NubIndex.newMemoryIndex(readTestNames());
  }

  public static List<NameUsageMatch> readTestNames() throws IOException {
    List<NameUsageMatch> usages = Lists.newArrayList();

    NameParser parser = new NameParserGbifV1();
    try(InputStream testFile = Resources.getResource("testNames.txt").openStream()) {
      CSVReader reader = CSVReaderFactory.build(testFile, "UTF8", "\t", null, 0);
      while (reader.hasNext()) {
        String[] row = reader.next();
        NameUsageMatch n = new NameUsageMatch();
        n.setUsageKey(Integer.valueOf(row[0]));
        n.setScientificName(row[1]);
        n.setCanonicalName(parser.parseToCanonical(n.getScientificName(), null));
        n.setFamily(row[2]);
        n.setOrder(row[3]);
        n.setClazz(row[4]);
        n.setPhylum(row[5]);
        n.setKingdom(row[6]);
        boolean isSynonym = Boolean.parseBoolean(row[7]);
        n.setStatus(isSynonym ? TaxonomicStatus.SYNONYM : TaxonomicStatus.ACCEPTED);
        n.setRank(VocabularyUtils.lookupEnum(row[8], Rank.class));
        usages.add(n);
      }

      Preconditions.checkArgument(usages.size() == 10, "Wrong number of test names");
    }
    return usages;
  }

  @Test
  public void testMatchByName() throws Exception {
    final Integer abiesAlbaKey = 7;
    NameUsageMatch m = index.matchByUsageId(abiesAlbaKey);
    assertEquals(abiesAlbaKey, m.getUsageKey());
    assertEquals("Abies alba Mill.", m.getScientificName());
    assertEquals(Rank.SPECIES, m.getRank());
    assertFalse(m.isSynonym());

    m = index.matchByName("Abies alba", true, 2).get(0);
    assertEquals(abiesAlbaKey, m.getUsageKey());

    m = index.matchByName("abies  alba", true, 2).get(0);
    assertEquals(abiesAlbaKey, m.getUsageKey());

    m = index.matchByName("Abbies alba", true, 2).get(0);
    assertEquals(abiesAlbaKey, m.getUsageKey());

    m = index.matchByName("abyes alba", true, 2).get(0);
    assertEquals(abiesAlbaKey, m.getUsageKey());

    m = index.matchByName(" apies  alba", true, 2).get(0);
    assertEquals(abiesAlbaKey, m.getUsageKey());

    // sciname soundalike filter enables this
    m = index.matchByName("Abies alllbbbbaaa", true, 2).get(0);
    assertEquals(abiesAlbaKey, m.getUsageKey());

    m = index.matchByName("Aebies allba", true, 2).get(0);
    assertEquals(abiesAlbaKey, m.getUsageKey());


    // fuzzy searches use a minPrefix=1
    assertTrue(index.matchByName("Obies alba", true, 2).isEmpty());

    assertTrue(index.matchByName("Abies elba", false, 2).isEmpty());

    // synonym matching
    m = index.matchByName("Picea abies", false, 2).get(0);
    assertTrue(m.isSynonym());

  }
}
