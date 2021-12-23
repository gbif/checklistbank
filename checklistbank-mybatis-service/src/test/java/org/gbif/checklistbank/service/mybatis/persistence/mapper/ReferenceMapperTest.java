package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.common.paging.PagingRequest;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReferenceMapperTest extends MapperITBase {

  private final ReferenceMapper mapper;

  @Autowired
  public ReferenceMapperTest(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      ReferenceMapper referenceMapper,
      DataSource dataSource) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        dataSource,
        true);
    this.mapper = referenceMapper;
  }

  @Test
  public void testMapper() throws Exception {
    assertTrue(mapper.listByChecklistUsage(usageKey, new PagingRequest()).isEmpty());
    assertTrue(mapper.listByNubUsage(usageKey, new PagingRequest()).isEmpty());

    Reference obj = new Reference();
    obj.setCitation(citation2);
    obj.setDoi(citation2doi);
    obj.setLink(citation2link);
    obj.setRemarks("few remarks");
    obj.setType("no type");
    // deprecated fields
    obj.setTitle("my title");
    obj.setAuthor("Mecka");
    obj.setDate("1988, March 15");
    // these should get ignored
    obj.setSource("sourcy s");
    obj.setSourceTaxonKey(123);

    mapper.insert(usageKey, citationKey2, obj);

    Reference obj2 = mapper.listByChecklistUsage(usageKey, new PagingRequest()).get(0);
    assertObject(obj, obj2, null, null);

    obj2 = mapper.listByNubUsage(nubKey, new PagingRequest()).get(0);
    // these are now nub source usage values
    assertObject(obj, obj2, datasetTitle, usageKey);
  }

  private void assertObject(Reference obj, Reference obj2, String source, Integer sourceTaxonKey) {
    assertEquals(obj.getCitation(), obj2.getCitation());
    assertEquals(obj.getDoi(), obj2.getDoi());
    assertEquals(obj.getLink(), obj2.getLink());
    assertEquals(obj.getRemarks(), obj2.getRemarks());
    assertEquals(obj.getType(), obj2.getType());

    assertNull(obj2.getAuthor());
    assertNull(obj2.getDate());
    assertNull(obj2.getTitle());

    assertEquals(source, obj2.getSource());
    assertEquals(sourceTaxonKey, obj2.getSourceTaxonKey());
  }
}
