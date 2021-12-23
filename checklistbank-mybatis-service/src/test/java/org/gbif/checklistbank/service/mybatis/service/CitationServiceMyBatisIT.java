package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.checklistbank.service.CitationService;
import org.gbif.utils.text.StringUtils;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CitationServiceMyBatisIT extends MyBatisServiceITBase {

  private final CitationService service;

  @Autowired
  public CitationServiceMyBatisIT(DataSource dataSource, CitationService citationService) {
    super(dataSource);
    this.service = citationService;
  }

  @Test
  public void testLargeCitations() throws Exception {
    String citation = StringUtils.randomString(100000);
    final Integer cid = service.createOrGet(citation);
    assertNotNull(cid);

    final Integer cid2 = service.createOrGet(citation);
    assertEquals(cid2, cid);
  }
}
