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

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.TableOfContents;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.checklistbank.model.TocEntry;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.DescriptionMapper;

import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Implements a DescriptionService using MyBatis. */
@Service
public class DescriptionServiceMyBatis extends NameUsageComponentServiceMyBatis<Description>
    implements DescriptionService {

  protected static final String ENGLISH = "en";
  private final DescriptionMapper descriptionMapper;

  @Autowired
  DescriptionServiceMyBatis(DescriptionMapper descriptionMapper) {
    super(descriptionMapper);
    this.descriptionMapper = descriptionMapper;
  }

  @Override
  public TableOfContents getToc(int usageKey) {
    TableOfContents toc = new TableOfContents();

    List<TocEntry> entries;
    if (isNub(usageKey)) {
      entries = descriptionMapper.listTocEntriesByNub(usageKey);
    } else {
      entries = descriptionMapper.listTocEntriesByUsage(usageKey);
    }

    for (TocEntry e : entries) {
      toc.addDescription(e.getKey(), e.getLanguage(), e.getTopic());
    }

    return toc;
  }

  @Nullable
  @Override
  public Description get(int key) {
    return descriptionMapper.get(key);
  }
}
