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
package org.gbif.nub.lookup.straight;

import org.gbif.ChecklistbankMyBatisServiceITBase;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.ParsedNameMapper;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.UsageMapper;
import org.gbif.nub.lookup.NubMatchingTestConfiguration;

import java.io.IOException;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Joiner;

import static org.gbif.api.vocabulary.Kingdom.*;
import static org.gbif.api.vocabulary.Rank.*;
import static org.gbif.api.vocabulary.TaxonomicStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class IdLookupImplITChecklistbank extends ChecklistbankMyBatisServiceITBase {

  private IdLookup matcher;
  private final ParsedNameMapper pnMapper;
  private final NameUsageMapper nuMapper;
  private final UsageMapper uMapper;
  private final ClbConfiguration cfg;

  private static final Joiner COMMA_JOINER = Joiner.on(", ").skipNulls();

  @Autowired
  public IdLookupImplITChecklistbank(
    DataSource dataSource, ParsedNameMapper pnMapper, NameUsageMapper nuMapper, UsageMapper uMapper,
    ClbConfiguration cfg
  ) {
    super(dataSource);
    this.pnMapper = pnMapper;
    this.nuMapper = nuMapper;
    this.uMapper = uMapper;
    this.cfg = cfg;
  }

  @Test
  public void load() throws Exception {
    // create 3 deleted & 1 non deleted nub usages
    createName("Dracula bella L.", SPECIES, true, pnMapper, nuMapper, uMapper);
    createName("Dracula bella Mill.", SPECIES, true, pnMapper, nuMapper, uMapper);
    createName("Dracula bella DC.", SPECIES, true, pnMapper, nuMapper, uMapper);
    createName("Dracula bella Engler.", SPECIES, false, pnMapper, nuMapper, uMapper);

    ClbConfiguration cfg = new ClbConfiguration();
    //TODO: set cfg values
    IdLookup l = IdLookupImpl.temp().load(cfg, true);
    for (LookupUsage u : l) {
      System.out.println(u);
    }

    assertEquals(3, l.deletedIds());
    assertEquals(6, l.size());
  }

  int createName(String name, Rank rank, boolean deleted, ParsedNameMapper pnMapper, NameUsageMapper nuMapper, UsageMapper uMapper) {
    ParsedName pn = new ParsedName();
    pn.setType(NameType.SCIENTIFIC);
    pn.setScientificName(name);
    pn.setRank(rank);
    pnMapper.create(pn);

    NameUsageWritable u = new NameUsageWritable();
    u.setDatasetKey(Constants.NUB_DATASET_KEY);
    u.setNameKey(pn.getKey());
    u.setRank(rank);
    u.setTaxonomicStatus(ACCEPTED);
    nuMapper.insert(u);

    if (deleted) {
      uMapper.deleteLogically(u.getKey());
    }
    return u.getKey();
  }

  @Test
  public void testMatching() throws IOException, InterruptedException {
    matcher = NubMatchingTestConfiguration.provideLookup();

    assertMatch("Abies", "", "", GENUS, PLANTAE, 1);

    assertNoMatch("Abies alba", null, null, SPECIES, ANIMALIA);
    assertNoMatch("Abies alba", null, null, GENUS, PLANTAE);
    assertNoMatch("Abies alba", null, null, SUBSPECIES, PLANTAE);
    assertMatch("Abies alba", null, null, SPECIES, PLANTAE, 2);
    assertMatch("Abies alba", "", "", SPECIES, PLANTAE, 2);
    assertMatch("Abies alba", "", "1768", SPECIES, PLANTAE, 2);
    assertMatch("Abies alba", "Mill.", "", SPECIES, PLANTAE, 2);
    assertMatch("Abies alba", "Miller", "", SPECIES, PLANTAE, 2);
    assertMatch("Abies alba", "Mill.", "1800", SPECIES, INCERTAE_SEDIS, 2);
    assertMatch("Abies alba", "M", "1768", SPECIES, INCERTAE_SEDIS, 2);
    assertNoMatch("Abies alba", "nothing but a year", "1768", SPECIES, INCERTAE_SEDIS);
    assertNoMatch("Abies alba", "DC", "", SPECIES, PLANTAE);
    assertNoMatch("Abies alba", "DeCandole", "1770", SPECIES, PLANTAE);
    assertNoMatch("Abies alba", "Linnaeus", "", SPECIES, PLANTAE);
    assertNoMatch("Abies alba", "L.", "1989", SPECIES, PLANTAE);
  }

  @Test
  public void testSquirrels() throws IOException, SQLException {
    IdLookup l = IdLookupImpl.temp().load(cfg, true);
    assertEquals(2, l.size());
    assertEquals(1, l.match("Animalia", KINGDOM, ANIMALIA).getKey());

    assertEquals(10, l.match("Rodentia", ORDER, ANIMALIA).getKey());
    assertNull(l.match("Rodentia", FAMILY, ANIMALIA));
    assertNull(l.match("Rodentia", ORDER, PLANTAE));
    assertNull(l.match("Rodenti", ORDER, ANIMALIA));

    assertEquals(10, l.match("Rodentia", "Bowdich", "1821", ORDER , ACCEPTED, ANIMALIA).getKey());
    assertEquals(10, l.match("Rodentia", "Bowdich", "1221", ORDER , ACCEPTED, ANIMALIA).getKey());
    assertEquals(10, l.match("Rodentia", "Bowdich", null, ORDER , ACCEPTED, ANIMALIA).getKey());
    assertEquals(10, l.match("Rodentia", null, "1821", ORDER , ACCEPTED, ANIMALIA).getKey());
    assertEquals(10, l.match("Rodentia", "Bow.", null, ORDER , ACCEPTED, ANIMALIA).getKey());
    assertEquals(10, l.match("Rodentia", "Bow", "1821", ORDER , ACCEPTED, ANIMALIA).getKey());
    assertEquals(10, l.match("Rodentia", "B", "1821", ORDER , ACCEPTED, ANIMALIA).getKey());
    assertNull(l.match("Rodentia", "Mill.", "1823", ORDER , ACCEPTED, ANIMALIA));
  }

  private LookupUsage assertMatch(String name, String authorship, String year, Rank rank, Kingdom kingdom, Integer expectedKey) {
    LookupUsage m = matcher.match(name, authorship, year, rank, ACCEPTED ,kingdom);
    if (expectedKey == null && m == null) {
      // all fine, as expected!
    } else if (m == null) {
      fail("\n" + name + " not matching, but expecting " + expectedKey);
    } else {
      System.out.println("\n" + name + " matches " + m.getCanonical() + " " + m.getAuthorship() + " [" + COMMA_JOINER.join(m.getKingdom(), m.getRank(), m.getKey()) + "]");
      if (expectedKey != null && m.getKey() != expectedKey) {
        printAltMatches(name);
      }
      assertEquals(expectedKey, (Integer)m.getKey());
    }
    return m;
  }

  private void printAltMatches(String name) {
    System.out.println("\nALTERNATIVES for " + name);
    for (LookupUsage m : matcher.match(name)) {
      System.out.println("\n" + m.getCanonical() + " " + m.getAuthorship() + " [" + COMMA_JOINER.join(m.getKingdom(), m.getRank(), m.getKey()) + "]");
    }
  }

  private void assertNoMatch(String name, String authorship, String year, Rank rank, Kingdom kingdom) {
    assertMatch(name, authorship, year, rank, kingdom, null);
  }

}
