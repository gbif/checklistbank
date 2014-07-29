package org.gbif.checklistbank.service.mybatis;


import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import java.util.UUID;

import com.google.common.base.Strings;
import com.google.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;

public class ParsedNameServiceMyBatis implements ParsedNameService {
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
  public ParsedName createOrGet(String scientificName) {
    checkArgument(!Strings.isNullOrEmpty(scientificName), "A name string is required");
    ParsedName pn = mapper.getByName(scientificName);
    if (pn == null) {
      try {
        pn = parser.parse(scientificName);
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
