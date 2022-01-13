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
package org.gbif.checklistbank.cli.registry;

import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;

public class RegistryCommand extends ServiceCommand {

  private final RegistryConfiguration configuration = new RegistryConfiguration();

  public RegistryCommand() {
    super("registry");
  }

  @Override
  protected Service getService() {
    return new RegistryService(configuration);
  }

  @Override
  protected Object getConfigurationObject() {
    return configuration;
  }

}
