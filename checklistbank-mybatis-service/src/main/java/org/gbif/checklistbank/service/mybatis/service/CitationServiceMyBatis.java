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

import org.gbif.checklistbank.model.Citation;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.CitationMapper;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;

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
    } catch (DataIntegrityViolationException e) {
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
