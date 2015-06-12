package org.gbif.checklistbank.service.mybatis;


import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.model.Citation;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.mybatis.mapper.CitationMapper;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.apache.ibatis.exceptions.PersistenceException;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

public class CitationServiceMyBatis implements CitationService {
  private static final Logger LOG = LoggerFactory.getLogger(ParsedNameServiceMyBatis.class);
  private CitationMapper mapper;

  @Inject
  CitationServiceMyBatis(CitationMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public String get(int key) {
    return mapper.get(key);
  }

  @Override
  public Integer createOrGet(String citation) {
    checkArgument(!Strings.isNullOrEmpty(citation), "A name string is required");
    try {
      return createOrGetThrowing(citation);
    } catch (PersistenceException e) {
      // we have a unique constraint in the database which can throw an exception when we concurrently write the same name into the table
      // try to read and ignore exception if we can read the name
      LOG.info("Inserting name {} failed, try to re-read", citation);
      return createOrGetThrowing(citation);
    }
  }

  @Transactional
  public Integer createOrGetThrowing(String citation) {
    Integer key = mapper.getByCitation(citation);
    if (key == null) {
      Citation c = new Citation(citation);
      mapper.insert(c);
      key = c.getKey();
    }
    return key;
  }

}
