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
package org.gbif.checklistbank.index;

import org.gbif.checklistbank.index.backfill.SolrBackfill;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension to load data into Solr.
 */
public class SolrLoadRule implements BeforeEachCallback {

  private SolrBackfill backFill;
  private DataSource dataSource;

  public SolrLoadRule(SolrBackfill backFill, DataSource dataSource) {
    this.backFill = backFill;
    this.dataSource = dataSource;
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    ClbDbTestRule.squirrels(dataSource).before();
    backFill.run();
  }
}
