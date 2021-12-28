/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.ParsedNameMapper;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

@Service
public class ParsedNameServiceMyBatis implements ParsedNameService {
  private static final Logger LOG = LoggerFactory.getLogger(ParsedNameServiceMyBatis.class);
  private ParsedNameMapper mapper;
  private NameParser parser;

  @Autowired
  ParsedNameServiceMyBatis(ParsedNameMapper mapper, NameParser parser) {
    this.mapper = mapper;
    this.parser = parser;
  }

  public ParsedName get(int key) {
    return mapper.get(key);
  }

  @Override
  public ParsedName createOrGet(ParsedName preParsed, final boolean update) {
    if (preParsed == null || Strings.isNullOrEmpty(preParsed.getScientificName())) {
      return null;
    }
    Preconditions.checkNotNull(
        preParsed.getType(), preParsed.getScientificName() + " lacks name type");

    try {
      return createOrGetThrowing(preParsed, update);
    } catch (PersistenceException e) {
      // we have a unique constraint in the database which can throw an exception when we
      // concurrently write the same name into the table
      // try to read and ignore exception if we can read the name
      LOG.warn("Inserting name >>>{}<<< failed, try to re-read", preParsed.getScientificName());
      return createOrGetThrowing(preParsed, update);
    }
  }

  @Transactional
  private ParsedName createOrGetThrowing(ParsedName preParsed, boolean update)
      throws PersistenceException {
    ParsedName pn = mapper.getByName(preParsed.getScientificName(), preParsed.getRank());
    if (pn == null) {
      // try to write the name to postgres
      mapper.create(preParsed);
    } else if (update && !pn.equals(preParsed)) {
      // is it different?
      preParsed.setKey(pn.getKey());
      mapper.update(preParsed);

    } else {
      return pn;
    }
    return preParsed;
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
    ReparseHandler handler = new ReparseHandler(parser, mapper);
    mapper.processNames(handler);
    LOG.info(
        "Reparsed all {} names, {} changed, {} failed: hybrids={}, virus={}, placeholder={}, noname={}",
        handler.counter,
        handler.changed,
        handler.failed,
        handler.hybrids,
        handler.virus,
        handler.placeholder,
        handler.noname);
    return handler.changed;
  }

  public static class ReparseHandler implements ResultHandler<ParsedName> {
    int counter = 0;
    int changed = 0;
    int failed = 0;
    int hybrids = 0;
    int virus = 0;
    int placeholder = 0;
    int blacklisted = 0;
    int noname = 0;

    private final NameParser parser;
    private final ParsedNameMapper mapper;

    public ReparseHandler(NameParser parser, ParsedNameMapper mapper) {
      this.parser = parser;
      this.mapper = mapper;
    }

    @Override
    public void handleResult(ResultContext<? extends ParsedName> context) {
      ParsedName p1 = context.getResultObject();
      counter++;
      ParsedName p2;
      try {
        p2 = parser.parse(p1.getScientificName(), p1.getRank());

      } catch (UnparsableException e) {
        p2 = new ParsedName();
        p2.setScientificName(p1.getScientificName());
        p2.setRank(p1.getRank());
        p2.setType(e.type);

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
          case BLACKLISTED:
            blacklisted++;
            break;
        }
      }

      p2.setKey(p1.getKey());
      if (!p1.equals(p2)) {
        LOG.debug("Updating reparsed name {} -> {}", p1, p2);
        mapper.update(p2);
        changed++;
      }
      if (counter % 100000 == 0) {
        LOG.info(
            "Reparsed {} names, {} changed, {} failed: hybrids={}, virus={}, placeholder={}, blacklisted={}, noname={}",
            counter,
            changed,
            failed,
            hybrids,
            virus,
            placeholder,
            blacklisted,
            noname);
      }
    }
  }
}
