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
package org.gbif.checklistbank.test.extensions;

import org.gbif.checklistbank.index.backfill.SolrBackfill;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbLoadTestDb;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Extension to load data into Solr and databse.
 */
public class SolrDbLoadBeforeAll implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    ApplicationContext ctx = SpringExtension.getApplicationContext(extensionContext);
    SolrBackfill solrBackfill = ctx.getBean(SolrBackfill.class);
    DataSource dataSource = ctx.getBean(DataSource.class);
    ClbLoadTestDb clbLoadTestDb = ClbLoadTestDb.squirrels(dataSource);
    clbLoadTestDb.before();
    solrBackfill.run();
  }
}