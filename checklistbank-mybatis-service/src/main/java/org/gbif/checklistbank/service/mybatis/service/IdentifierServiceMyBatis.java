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
