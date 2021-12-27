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
package org.gbif.checklistbank.utils;

import org.gbif.checklistbank.config.ClbConfiguration;

/**
 *
 */
public class ClbConfigurationUtils {

  public static ClbConfiguration local() {
    ClbConfiguration cfg = new ClbConfiguration();
    cfg.serverName = "localhost";
    cfg.databaseName = "checklistbank";
    cfg.user = "markus";
    cfg.password = "";
    return cfg;
  }

  public static ClbConfiguration uat() {
    ClbConfiguration cfg = local();
    cfg.serverName = "pg1.gbif-uat.org";
    cfg.databaseName = "uat_checklistbank";
    cfg.user = "clb";
    cfg.password = "---";
    return cfg;
  }

}
