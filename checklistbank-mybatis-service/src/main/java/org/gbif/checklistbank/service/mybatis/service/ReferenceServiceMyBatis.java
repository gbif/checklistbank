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

import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.service.checklistbank.ReferenceService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.ReferenceMapper;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Implements a ReferenceService using MyBatis. */
@Service
public class ReferenceServiceMyBatis extends NameUsageComponentServiceMyBatis<Reference>
    implements ReferenceService {

  @Autowired
  ReferenceServiceMyBatis(ReferenceMapper referenceMapper) {
    super(referenceMapper);
  }

  @Override
  public Map<Integer, List<Reference>> listRange(int usageKeyStart, int usageKeyEnd) {
    throw new UnsupportedOperationException("listRange not supported");
  }
}
