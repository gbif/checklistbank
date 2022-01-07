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
package org.gbif.checklistbank.cli.shell;

import org.gbif.cli.service.ServiceCommand;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;

/**
 * Command that opens an embedded neo4j with a bolt connector so it can be accessed remotely-
 * For example with the neo4j browser or the cypher-shell:
 * ./cypher-shell -u neo4j -p <password>
 */
public class ShellCommand extends ServiceCommand {
  private final ShellConfiguration cfg = new ShellConfiguration();

  public ShellCommand() {
    super("shell");
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

  @Override
  protected Service getService() {
    return new NeoService();
  }

  private class NeoService extends AbstractIdleService {
    GraphDatabaseService neo;

    @Override
    protected void startUp() throws Exception {
      final File storeDir = cfg.neo.neoDir(cfg.key);
      Preconditions.checkArgument(storeDir.exists(), "No neo4j store directory existing at " + storeDir.getAbsolutePath());
      System.out.println("Connecting to neo4j store at " + storeDir.getAbsolutePath());

      // shell command without bolt makes no sense
      cfg.neo.bolt = true;
      neo = cfg.neo.newEmbeddedDb(storeDir, false).newGraphDatabase();
      System.out.println("Opening neo4j bolt connector on port " + cfg.neo.port);
    }

    @Override
    protected void shutDown() throws Exception {
      if (neo != null) {
        neo.shutdown();
      }
    }
  }

}
