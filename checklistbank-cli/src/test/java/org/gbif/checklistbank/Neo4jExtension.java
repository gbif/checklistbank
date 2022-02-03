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
