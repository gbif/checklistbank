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

import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.MultimediaService;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.MultimediaMapper;
import org.gbif.checklistbank.utils.MediaTypeUtils;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Implements an ImageService using MyBatis. */
@Service
public class MultimediaServiceMyBatis extends NameUsageComponentServiceMyBatis<NameUsageMediaObject>
    implements MultimediaService {

  @Autowired
  public MultimediaServiceMyBatis(MultimediaMapper multimediaMapper) {
    super(multimediaMapper);
  }

  @Override
  public Map<Integer, List<NameUsageMediaObject>> listRange(int usageKeyStart, int usageKeyEnd) {
    throw new UnsupportedOperationException("listRange not supported");
  }

  @Override
  public PagingResponse<NameUsageMediaObject> listByUsage(int usageKey, @Nullable Pageable page) {
    PagingResponse<NameUsageMediaObject> result = super.listByUsage(usageKey, page);
    // TODO: avoid live interpretations until we store the type properly
    for (NameUsageMediaObject m : result.getResults()) {
      MediaTypeUtils.detectType(m);
    }
    return result;
  }
}
