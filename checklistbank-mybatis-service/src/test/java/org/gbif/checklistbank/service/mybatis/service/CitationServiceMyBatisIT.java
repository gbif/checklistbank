package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.checklistbank.service.CitationService;
import org.gbif.utils.text.StringUtils;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CitationServiceMyBatisIT extends MyBatisServiceITBase2 {

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