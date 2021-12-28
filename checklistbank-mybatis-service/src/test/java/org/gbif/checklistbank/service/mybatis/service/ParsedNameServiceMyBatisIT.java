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
package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.nameparser.NameParserGbifV1;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ParsedNameServiceMyBatisIT extends MyBatisServiceITBase {

  private NameParser parser = new NameParserGbifV1();

  private final ParsedNameService service;

  @Autowired
  public ParsedNameServiceMyBatisIT(DataSource dataSource, ParsedNameService parsedNameService) {
    super(dataSource);
    this.service = parsedNameService;
  }

  @Test
  public void testCreateOrGet() throws Exception {
    ParsedName pn = new ParsedName();
    pn.setScientificName("Abies alba Mill.");
    pn.setGenusOrAbove("Abies");
    pn.setAuthorship("Mill.");
    pn.setSpecificEpithet("alba");
    pn.setType(NameType.SCIENTIFIC);
    assertNull(pn.getKey());

    ParsedName pn2 = service.createOrGet(pn, true);
    assertNotNull(pn2.getKey());
    assertEquals("Abies alba Mill.", pn2.getScientificName());
    assertEquals("Abies alba", pn2.canonicalName());
    assertEquals("Abies", pn2.getGenusOrAbove());
    assertEquals("alba", pn2.getSpecificEpithet());
    assertEquals("Mill.", pn2.getAuthorship());

    pn = service.createOrGet(parse("Abies alba Mill."), true);
    assertEquals("Abies alba Mill.", pn.getScientificName());
    assertEquals("Abies alba", pn.canonicalName());
    assertEquals("Abies", pn.getGenusOrAbove());
    assertEquals("alba", pn.getSpecificEpithet());
    assertEquals("Mill.", pn.getAuthorship());

    pn = service.createOrGet(parse("Abies sp."), true);
    assertEquals("Abies sp.", pn.getScientificName());
    assertEquals("Abies spec.", pn.canonicalName());
    assertEquals("Abies", pn.getGenusOrAbove());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getSpecificEpithet());

    pn = service.createOrGet(parse("×Abies Mill."), true);
    assertEquals("×Abies Mill.", pn.getScientificName());
    assertEquals("Abies", pn.canonicalName());
    assertEquals("Abies", pn.getGenusOrAbove());
    assertNull(pn.getRank());
    assertNull(pn.getSpecificEpithet());
    assertEquals(NamePart.GENERIC, pn.getNotho());

    pn = service.createOrGet(parse("? hostilis Gravenhorst, 1829"), true);
    assertEquals("? hostilis Gravenhorst, 1829", pn.getScientificName());
    assertEquals("? hostilis", pn.canonicalName());
    assertEquals("?", pn.getGenusOrAbove());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertEquals("hostilis", pn.getSpecificEpithet());
  }

  private ParsedName parse(String x) {
    try {
      return parser.parse(x, null);
    } catch (UnparsableException e) {
      ParsedName pn = new ParsedName();
      pn.setScientificName(x);
      pn.setType(e.type);
      return pn;
    }
  }

  @Test
  public void testOrphaned() throws Exception {
    assertEquals(1, service.deleteOrphaned());
  }

  @Test
  //TODO: it throws a time-out error
  public void testReparse() throws Exception {
    assertEquals(6, service.reparseAll());
  }
}
