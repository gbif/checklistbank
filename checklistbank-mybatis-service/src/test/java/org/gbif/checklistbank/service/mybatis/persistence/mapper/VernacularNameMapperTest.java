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
package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.LifeStage;
import org.gbif.api.vocabulary.Sex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VernacularNameMapperTest extends MapperITBase {

  private final VernacularNameMapper mapper;

  @Autowired
  public VernacularNameMapperTest(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      VernacularNameMapper vernacularNameMapper) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        true);
    this.mapper = vernacularNameMapper;
  }

  @Test
  public void testMapper() throws Exception {
    assertNull(mapper.getByChecklistUsage(usageKey, Language.ENGLISH.getIso2LetterCode()));
    assertNull(mapper.getByNubUsage(usageKey, Language.ENGLISH.getIso2LetterCode()));
    assertTrue(mapper.listByChecklistUsage(usageKey, new PagingRequest()).isEmpty());
    assertTrue(mapper.listByNubUsage(usageKey, new PagingRequest()).isEmpty());

    VernacularName v = new VernacularName();
    v.setCountry(Country.AFGHANISTAN);
    v.setArea("kundus");
    v.setLanguage(Language.AFAR);
    v.setLifeStage(LifeStage.EMRYO);
    v.setPlural(false);
    v.setPreferred(true);
    v.setSex(Sex.FEMALE);
    v.setVernacularName("Afghanische Gans");
    // these should get ignored
    v.setSource("sourcy s");
    v.setSourceTaxonKey(123);

    mapper.insert(usageKey, v, citationKey1);

    VernacularName v2 = mapper.getByChecklistUsage(usageKey, Language.AFAR.getIso2LetterCode());
    assertEquals(v.getVernacularName(), v2.getVernacularName());
    assertEquals(v.getLanguage(), v2.getLanguage());
    assertEquals(v.getSex(), v2.getSex());
    assertEquals(v.getLifeStage(), v2.getLifeStage());
    assertEquals(v.isPlural(), v2.isPlural());
    assertEquals(v.isPreferred(), v2.isPreferred());
    assertEquals(v.getArea(), v2.getArea());
    assertEquals(v.getCountry(), v2.getCountry());
    // these are handled special
    assertEquals(citation1, v2.getSource());
    assertNull(v2.getSourceTaxonKey());

    VernacularName v3 = mapper.getByNubUsage(nubKey, Language.AFAR.getIso2LetterCode());
    assertEquals(v.getVernacularName(), v3.getVernacularName());
    assertEquals(v.getLanguage(), v3.getLanguage());
    assertEquals(v.getSex(), v3.getSex());
    assertEquals(v.getLifeStage(), v3.getLifeStage());
    assertEquals(v.isPlural(), v3.isPlural());
    assertEquals(v.isPreferred(), v3.isPreferred());
    assertEquals(v.getArea(), v3.getArea());
    assertEquals(v.getCountry(), v3.getCountry());
    // these are now nub source usage values
    assertEquals(datasetTitle, v3.getSource());
    assertEquals((Integer) usageKey, v3.getSourceTaxonKey());
  }
}
