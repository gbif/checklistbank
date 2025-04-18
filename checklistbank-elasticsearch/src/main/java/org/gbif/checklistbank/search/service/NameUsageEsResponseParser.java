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
package org.gbif.checklistbank.search.service;

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.checklistbank.index.model.NameUsageAvro;
import org.gbif.common.search.EsResponseParser;

public class NameUsageEsResponseParser
    extends EsResponseParser<NameUsageSearchResult, NameUsageAvro, NameUsageSearchParameter> {

  private NameUsageEsResponseParser() {
    super(new NameUsageSearchResultConverter(), new NameUsageEsFieldMapper());
  }

  public static NameUsageEsResponseParser create() {
    return new NameUsageEsResponseParser();
  }
}
