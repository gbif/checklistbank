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
package org.gbif.checklistbank;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Neo4jContainer;

public class Neo4jExtension implements BeforeAllCallback, AfterAllCallback {

  private final Neo4jContainer neo4jContainer = new Neo4jContainer("neo4j:3.5.29");

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    neo4jContainer.withWorkingDirectory(FileUtils.getTempDirectory().getAbsolutePath());
    neo4jContainer.start();
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    neo4jContainer.stop();
  }

  public Neo4jContainer getNeo4jContainer() {
    return neo4jContainer;
  }
}
