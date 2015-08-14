package org.gbif.checklistbank.service.mybatis;


import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.mapper.ParsedNameMapper;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import java.util.UUID;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.apache.ibatis.exceptions.PersistenceException;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParsedNameServiceMyBatis implements ParsedNameService {
  private static final Logger LOG = LoggerFactory.getLogger(ParsedNameServiceMyBatis.class);
  private ParsedNameMapper mapper;
  private NameParser parser;

  @Inject
  ParsedNameServiceMyBatis(ParsedNameMapper mapper, NameParser parser) {
    this.mapper = mapper;
    this.parser = parser;
  }

  @Override
  public ParsedName get(int key) {
    return mapper.get(key);
  }

  @Override
  public ParsedName createOrGet(String scientificName, Rank rank) {
    if (Strings.isNullOrEmpty(scientificName)) {
      return null;
    }
    try {
      return createOrGetThrowing(scientificName, rank);
    } catch (PersistenceException e) {
      // we have a unique constraint in the database which can throw an exception when we concurrently write the same name into the table
      // try to read and ignore exception if we can read the name
      LOG.warn("Inserting name >>>{}<<< failed, try to re-read", scientificName);
      return createOrGetThrowing(scientificName, rank);
    }
  }

  @Transactional
  private ParsedName createOrGetThrowing(String scientificName, Rank rank) throws PersistenceException {
    ParsedName pn = mapper.getByName(scientificName);
    if (pn == null) {
      try {
        pn = parser.parse(scientificName, rank);
      } catch (UnparsableException e) {
        pn = new ParsedName();
        pn.setScientificName(scientificName);
        pn.setType(e.type);
      }
      mapper.create(pn, pn.canonicalName());
    }
    return pn;
  }

  @Override
  public PagingResponse<ParsedName> listNames(UUID datasetKey, Pageable page) {
    return new PagingResponse<ParsedName>(page, null, mapper.list(datasetKey, page));
  }

}
