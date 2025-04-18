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

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeDesignationType;
import org.gbif.api.vocabulary.TypeStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TypeSpecimenMapperTest extends MapperITBase {

  private final TypeSpecimenMapper mapper;

  @Autowired
  public TypeSpecimenMapperTest(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      TypeSpecimenMapper typeSpecimenMapper) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        true);
    this.mapper = typeSpecimenMapper;
  }

  @Test
  public void testMapper() throws Exception {
    assertTrue(mapper.listByChecklistUsage(usageKey, new PagingRequest()).isEmpty());
    assertTrue(mapper.listByNubUsage(usageKey, new PagingRequest()).isEmpty());

    TypeSpecimen obj = new TypeSpecimen();
    obj.setScientificName("Abies alba");
    obj.setTypeDesignatedBy("Markus");
    obj.setTypeDesignationType(TypeDesignationType.ORIGINAL_DESIGNATION);
    obj.setTaxonRank(Rank.SPECIES);
    // these are legacy properties not stored in CLB - we only store type species/genus records, not
    // specimens as these are occurrences!
    obj.setTypeStatus(TypeStatus.TYPE_SPECIES);
    obj.setCitation(citation2);
    obj.setLocality("locality");
    obj.setCatalogNumber("catNum177");
    // these should get ignored
    obj.setSource("sourcy s");
    obj.setSourceTaxonKey(123);

    mapper.insert(usageKey, obj, citationKey1);

    TypeSpecimen obj2 = mapper.listByChecklistUsage(usageKey, new PagingRequest()).get(0);
    assertEquals(obj.getScientificName(), obj2.getScientificName());
    assertEquals(obj.getTypeDesignatedBy(), obj2.getTypeDesignatedBy());
    assertEquals(obj.getTypeDesignationType(), obj2.getTypeDesignationType());
    assertEquals(obj.getTaxonRank(), obj2.getTaxonRank());
    // deprecated fields
    assertNull(obj2.getTypeStatus());
    assertNull(obj2.getCitation());
    assertNull(obj2.getLocality());
    assertNull(obj2.getCatalogNumber());
    assertNull(obj2.getRecordedBy());
    // these are handled special
    assertEquals(citation1, obj2.getSource());
    assertNull(obj2.getSourceTaxonKey());

    TypeSpecimen obj3 = mapper.listByNubUsage(nubKey, new PagingRequest()).get(0);
    assertEquals(obj.getScientificName(), obj3.getScientificName());
    assertEquals(obj.getTypeDesignatedBy(), obj3.getTypeDesignatedBy());
    assertEquals(obj.getTypeDesignationType(), obj3.getTypeDesignationType());
    assertEquals(obj.getTaxonRank(), obj3.getTaxonRank());
    // deprecated fields
    assertNull(obj3.getTypeStatus());
    assertNull(obj3.getCitation());
    assertNull(obj3.getLocality());
    assertNull(obj3.getCatalogNumber());
    assertNull(obj3.getRecordedBy());
    // these are now nub source usage values
    assertEquals(datasetTitle, obj3.getSource());
    assertEquals((Integer) usageKey, obj3.getSourceTaxonKey());
  }
}
