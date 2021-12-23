package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.IdentifierService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.IdentifierMapper;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Implements a DistributionService using MyBatis. */
@Service
public class IdentifierServiceMyBatis implements IdentifierService {
  private final IdentifierMapper mapper;

  @Autowired
  IdentifierServiceMyBatis(IdentifierMapper identifierMapper) {
    mapper = identifierMapper;
  }

  public Identifier get(int key) {
    return mapper.get(key);
  }

  @Override
  public PagingResponse<Identifier> listByUsage(int usageKey, @Nullable Pageable page) {
    if (page == null) {
      page = new PagingRequest();
    }

    return new PagingResponse<Identifier>(page, null, mapper.listByUsage(usageKey, page));
  }
}
