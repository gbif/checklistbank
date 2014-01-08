package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.file.CSVReader;
import org.gbif.file.CSVReaderFactory;
import org.gbif.nameparser.NameParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NubIndexTest {

  private static NubIndex index;
  private static List<NameUsage> names;

  @BeforeClass
  public static void buildMatcher() throws IOException {
    HigherTaxaLookup syn = new HigherTaxaLookup();
    syn.loadClasspathDicts("dicts");

    names = readTestNames();

    index = new NubIndex();
    for (NameUsage u : names) {
      index.addNameUsage(u);
    }
  }

  public static List<NameUsage> readTestNames() throws IOException {
    List<NameUsage> usages = Lists.newArrayList();

    NameParser parser = new NameParser();

    InputStream in = Resources.newInputStreamSupplier(Resources.getResource("testNames.txt")).getInput();
    CSVReader reader = CSVReaderFactory.build(in, "UTF8", "\t", null, 0);
    for (String[] row : reader) {
      NameUsage n = new NameUsage();
      n.setKey(Integer.valueOf(row[0]));
      n.setScientificName(row[1]);
      n.setCanonicalName(parser.parseToCanonical(n.getScientificName()));
      n.setFamily(row[2]);
      n.setOrder(row[3]);
      n.setClazz(row[4]);
      n.setPhylum(row[5]);
      n.setKingdom(row[6]);
      n.setSynonym(Boolean.parseBoolean(row[7]));
      n.setRank( (Rank) VocabularyUtils.lookupEnum(row[8], Rank.class) );
      usages.add(n);
    }

    Preconditions.checkArgument(usages.size() == 10, "Wrong number of test names");

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
    m = index.matchByName("Aabbiess allba", true, 2).get(0);
    assertEquals(abiesAlbaKey, m.getUsageKey());

    m = index.matchByName("Aebies allba", true, 2).get(0);
    assertEquals(abiesAlbaKey, m.getUsageKey());


    // fuzzy searches use a minPrefix=1
    assertTrue(index.matchByName("Obies alba", true, 2).isEmpty());

    assertTrue(index.matchByName("Abies elba", false, 2).isEmpty());

  }
}
