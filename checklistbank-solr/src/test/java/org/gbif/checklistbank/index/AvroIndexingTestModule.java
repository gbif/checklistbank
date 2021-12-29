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

import org.gbif.checklistbank.index.guice.AvroIndexingModulePrivate;

import java.util.Properties;

import com.google.inject.AbstractModule;

/**
 * Guice module that initializes the required classes and dependencies for the CLB indexer
 * using a mocked dataset service.
 */
public class AvroIndexingTestModule extends AbstractModule {

  private final Properties properties;

  public AvroIndexingTestModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    // Installs the MyBatis service layer
    //install(new ChecklistBankServiceMyBatisModule(properties));

    // Installs private indexing module
    install(new AvroIndexingModulePrivate(properties));
  }
}
