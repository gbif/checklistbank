package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.checklistbank.model.Citation;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.CitationMapper;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import org.apache.ibatis.exceptions.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitationServiceMyBatis implements CitationService {
  private static final Logger LOG = LoggerFactory.getLogger(ParsedNameServiceMyBatis.class);
  private CitationMapper mapper;

  @Autowired
  CitationServiceMyBatis(CitationMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Integer createOrGet(String citation) {
    return createOrGet(citation, null, null);
  }

  @Override
  public Integer createOrGet(String citation, String doi, String link) {
    if (Strings.isNullOrEmpty(citation)) {
      return null;
    }
    try {
      return createOrGetThrowing(citation, doi, link);
    } catch (PersistenceException e) {
      // we have a unique constraint in the database which can throw an exception when we
      // concurrently write the same name into the table
      // try to read and ignore exception if we can read the name
      LOG.info("Inserting citation >>>{}<<< failed, try to re-read", citation);
      return createOrGetThrowing(citation, doi, link);
    }
  }

  @Transactional
  public Integer createOrGetThrowing(String citation, @Nullable String doi, @Nullable String link) {
    Integer key = mapper.getByCitation(citation);
    if (key == null) {
      Citation c = new Citation(citation);
      c.setDoi(doi);
      c.setLink(link);
      mapper.insert(c);
      key = c.getKey();
    }
    return key;
  }
}
