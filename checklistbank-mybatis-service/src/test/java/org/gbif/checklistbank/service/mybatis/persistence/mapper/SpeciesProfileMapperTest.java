package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.common.paging.PagingRequest;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpeciesProfileMapperTest extends MapperITBase {

  private SpeciesProfileMapper mapper;

  @Autowired
  public SpeciesProfileMapperTest(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      DataSource dataSource,
      SpeciesProfileMapper mapper) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        dataSource,
        true);
    this.mapper = mapper;
  }

  @Test
  public void testMapper() throws Exception {
    assertTrue(mapper.listByChecklistUsage(usageKey, new PagingRequest()).isEmpty());
    assertTrue(mapper.listByNubUsage(usageKey, new PagingRequest()).isEmpty());

    SpeciesProfile obj = new SpeciesProfile();
    obj.setAgeInDays(55);
    obj.setExtinct(true);
    obj.setFreshwater(true);
    obj.setHabitat("macchia");
    obj.setHybrid(true);
    obj.setLifeForm("tree");
    obj.setLivingPeriod("Carniferous");
    obj.setMarine(false);
    obj.setMassInGram(4);
    obj.setSizeInMillimeter(3);
    obj.setTerrestrial(null);
    // these should get ignored
    obj.setSource("sourcy s");
    obj.setSourceTaxonKey(123);

    mapper.insert(usageKey, obj, citationKey1);

    SpeciesProfile obj2 = mapper.listByChecklistUsage(usageKey, new PagingRequest()).get(0);
    assertObject(obj, obj2, citation1, null);

    obj2 = mapper.listByNubUsage(nubKey, new PagingRequest()).get(0);
    // these are now nub source usage values
    assertObject(obj, obj2, datasetTitle, usageKey);
  }

  private void assertObject(
      SpeciesProfile obj, SpeciesProfile obj2, String source, Integer sourceTaxonKey) {
    assertEquals(obj.getAgeInDays(), obj2.getAgeInDays());
    assertEquals(obj.isExtinct(), obj2.isExtinct());
    assertEquals(obj.isFreshwater(), obj2.isFreshwater());
    assertEquals(obj.getHabitat(), obj2.getHabitat());
    assertEquals(obj.isHybrid(), obj2.isHybrid());
    assertEquals(obj.getLifeForm(), obj2.getLifeForm());
    assertEquals(obj.getLivingPeriod(), obj2.getLivingPeriod());
    assertEquals(obj.isMarine(), obj2.isMarine());
    assertEquals(obj.getMassInGram(), obj2.getMassInGram());
    assertEquals(obj.getSizeInMillimeter(), obj2.getSizeInMillimeter());
    assertEquals(obj.isTerrestrial(), obj2.isTerrestrial());

    assertEquals(source, obj2.getSource());
    assertEquals(sourceTaxonKey, obj2.getSourceTaxonKey());
  }
}
