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
package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import org.gbif.api.vocabulary.NameUsageIssue;

/**
 * This type handler is based on a text[] postgres type.
 */
public class ArraySetIssueTypeHandler extends ArraySetTypeHandler<NameUsageIssue> {

  public ArraySetIssueTypeHandler() {
    super("text");
  }

  @Override
  protected NameUsageIssue convert(String x) {
    return NameUsageIssue.valueOf(x);
  }
}
