package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.vocabulary.MediaType;

import java.net.URI;
import java.util.Date;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultimediaMapperTest extends MapperITBase {

  private final MultimediaMapper mapper;

  @Autowired
  public MultimediaMapperTest(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      MultimediaMapper multimediaMapper,
      DataSource dataSource) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        dataSource,
        true);
    this.mapper = multimediaMapper;
  }

  @Test
  public void testMapper() throws Exception {
    assertTrue(mapper.listByChecklistUsage(usageKey, new PagingRequest()).isEmpty());
    assertTrue(mapper.listByNubUsage(usageKey, new PagingRequest()).isEmpty());

    NameUsageMediaObject obj = new NameUsageMediaObject();
    obj.setAudience("audience");
    obj.setContributor("contrib");
    obj.setCreated(new Date());
    obj.setCreator("creator");
    obj.setDescription("descr");
    obj.setFormat("format");
    obj.setIdentifier(URI.create("https://my.id/sharks"));
    obj.setLicense("license");
    obj.setPublisher("publisher");
    obj.setReferences(URI.create("https://my.tv/sharks"));
    obj.setRightsHolder("rights holder");
    obj.setTitle("title");
    obj.setType(MediaType.Sound);
    // these should get ignored
    obj.setSource("sourcy s");
    obj.setSourceTaxonKey(123);

    mapper.insert(usageKey, obj, citationKey1);

    NameUsageMediaObject obj2 = mapper.listByChecklistUsage(usageKey, new PagingRequest()).get(0);
    assertObject(obj, obj2, citation1, null);

    obj2 = mapper.listByNubUsage(nubKey, new PagingRequest()).get(0);
    // these are now nub source usage values
    assertObject(obj, obj2, datasetTitle, usageKey);
  }

  private void assertObject(
      NameUsageMediaObject obj, NameUsageMediaObject obj2, String source, Integer sourceTaxonKey) {
    assertEquals(obj.getAudience(), obj2.getAudience());
    assertEquals(obj.getContributor(), obj2.getContributor());
    assertEquals(obj.getCreated(), obj2.getCreated());
    assertEquals(obj.getCreator(), obj2.getCreator());
    assertEquals(obj.getDescription(), obj2.getDescription());
    assertEquals(obj.getFormat(), obj2.getFormat());
    assertEquals(obj.getIdentifier(), obj2.getIdentifier());
    assertEquals(obj.getLicense(), obj2.getLicense());
    assertEquals(obj.getPublisher(), obj2.getPublisher());
    assertEquals(obj.getReferences(), obj2.getReferences());
    assertEquals(obj.getRightsHolder(), obj2.getRightsHolder());
    assertEquals(obj.getTitle(), obj2.getTitle());

    assertEquals(source, obj2.getSource());
    assertEquals(sourceTaxonKey, obj2.getSourceTaxonKey());
  }
}
