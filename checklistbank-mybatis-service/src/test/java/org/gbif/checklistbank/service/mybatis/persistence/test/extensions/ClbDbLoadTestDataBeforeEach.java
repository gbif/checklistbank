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
package org.gbif.checklistbank.service.mybatis.persistence.test.extensions;

import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbLoadTestDb;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class ClbDbLoadTestDataBeforeEach implements BeforeEachCallback {

  static void before(ExtensionContext extensionContext) throws Exception {
    ApplicationContext context = SpringExtension.getApplicationContext(extensionContext);
    DataSource dataSource = context.getBean(DataSource.class);
    ClbLoadTestDb  clbLoadTestDb;
    TestData.DATAFILE testData = getTestData(extensionContext);
    if (TestData.DATAFILE.SQUIRRELS == testData) {
      clbLoadTestDb = ClbLoadTestDb.squirrels(dataSource);
    } else if (TestData.DATAFILE.PUMA == testData) {
      clbLoadTestDb = ClbLoadTestDb.puma(dataSource);
    } else { //TestData.DATAFILE.EMPTY default
      clbLoadTestDb = ClbLoadTestDb.empty(dataSource);
    }
    clbLoadTestDb.before();
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    before(extensionContext);
  }

  static TestData.DATAFILE getTestData(ExtensionContext extensionContext) {
    return extensionContext.getTestClass().map(c -> c.getAnnotation(TestData.class))
            .map(TestData::value).orElse(TestData.DATAFILE.EMPTY);
  }
}