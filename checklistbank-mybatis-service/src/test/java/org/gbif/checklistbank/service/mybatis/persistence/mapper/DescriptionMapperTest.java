package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.vocabulary.Language;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DescriptionMapperTest extends MapperITBase {

  private final DescriptionMapper mapper;

  @Autowired
  public DescriptionMapperTest(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      DataSource dataSource,
      DescriptionMapper descriptionMapper) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        dataSource,
        true);
    this.mapper = descriptionMapper;
  }

  @Test
  public void testMapper() throws Exception {
    assertTrue(mapper.listByChecklistUsage(usageKey, new PagingRequest()).isEmpty());
    assertTrue(mapper.listByNubUsage(usageKey, new PagingRequest()).isEmpty());

    Description obj = new Description();
    obj.setContributor("contribtr");
    obj.setCreator("creatr");
    obj.setDescription("description");
    obj.setLanguage(Language.ABKHAZIAN);
    obj.setLicense("license");
    obj.setType("myType");
    // these should get ignored
    obj.setSource("sourcy s");
    obj.setSourceTaxonKey(123);

    mapper.insert(usageKey, obj, citationKey1);

    Description obj2 = mapper.listByChecklistUsage(usageKey, new PagingRequest()).get(0);
    assertObject(obj, obj2, citation1, null);

    obj2 = mapper.listByNubUsage(nubKey, new PagingRequest()).get(0);
    // these are now nub source usage values
    assertObject(obj, obj2, datasetTitle, usageKey);
  }

  private void assertObject(
      Description obj, Description obj2, String source, Integer sourceTaxonKey) {
    assertEquals(obj.getContributor(), obj2.getContributor());
    assertEquals(obj.getCreator(), obj2.getCreator());
    assertEquals(obj.getDescription(), obj2.getDescription());
    assertEquals(obj.getLanguage(), obj2.getLanguage());
    assertEquals(obj.getLicense(), obj2.getLicense());
    assertEquals(obj.getType(), obj2.getType());

    assertEquals(source, obj2.getSource());
    assertEquals(sourceTaxonKey, obj2.getSourceTaxonKey());
  }
}
