package org.gbif.checklistbank.service.mybatis;


import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.mapper.ParsedNameMapper;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
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
  public ParsedName createOrGet(ParsedName preParsed) {
    if (preParsed == null || Strings.isNullOrEmpty(preParsed.getScientificName())) {
      return null;
    }
    Preconditions.checkNotNull(preParsed.getType());
    try {
      return createOrGetThrowing(preParsed);
    } catch (PersistenceException e) {
      // we have a unique constraint in the database which can throw an exception when we concurrently write the same name into the table
      // try to read and ignore exception if we can read the name
      LOG.warn("Inserting name >>>{}<<< failed, try to re-read", preParsed.getScientificName());
      return createOrGetThrowing(preParsed);
    }
  }

  @Transactional
  private ParsedName createOrGetThrowing(ParsedName preParsed) throws PersistenceException {
    ParsedName pn = mapper.getByName(preParsed.getScientificName());
    if (pn == null) {
      // try to write the name to postgres
      write(preParsed);
      return preParsed;
    }
    return pn;
  }

  private void write(ParsedName pn) {
    mapper.create(pn, Strings.emptyToNull(pn.canonicalName()));
  }

  @Override
  public int deleteOrphaned() {
    int deletedAll = 0;
    Integer max = mapper.maxKey();
    if (max != null) {
      while (max > 0) {
        int min = max - 100000;
        int deleted = mapper.deleteOrphaned(min, max);
        LOG.debug("Deleted {} orphaned names in range {}-{}", deleted, min, max);
        max = min;
        deletedAll = deletedAll + deleted;
      }
    }
    return deletedAll;
  }


  @Override
  public int reparseAll() {
    ReparseHandler handler = new ReparseHandler();
    mapper.processNames(handler);
    LOG.info("Reparsed all {} names, {} changed, {} failed: hybrids={}, virus={}, placeholder={}, noname={}",
        handler.counter, handler.changed, handler.failed, handler.hybrids, handler.virus, handler.placeholder, handler.noname);
    return handler.counter;
  }

  private class ReparseHandler implements ResultHandler<ParsedName> {
    int counter = 0;
    int changed = 0;
    int failed = 0;
    int hybrids = 0;
    int virus = 0;
    int placeholder = 0;
    int noname = 0;

    @Override
    public void handleResult(ResultContext<? extends ParsedName> context) {
      ParsedName pn = context.getResultObject();
      counter++;
      try {
        ParsedName p2 = parser.parse(pn.getScientificName(), null);
        p2.setKey(pn.getKey());
        if (!pn.equals(p2)) {
          mapper.update(p2, p2.canonicalName());
          changed++;
        }
      } catch (UnparsableException e) {
        failed++;
        switch (e.type) {
          case HYBRID:
            hybrids++;
            break;
          case VIRUS:
            virus++;
            break;
          case PLACEHOLDER:
            placeholder++;
            break;
          case NO_NAME:
            noname++;
            break;
        }
      }
      if (counter % 100000 == 0) {
        LOG.info("Reparsed {} names, {} changed, {} failed: hybrids={}, virus={}, placeholder={}, noname={}", counter, changed, failed, hybrids, virus, placeholder, noname);
      }
    }
  }

}
