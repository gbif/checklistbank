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
package org.gbif.checklistbank.logging;

import java.util.UUID;

import org.slf4j.MDC;

/**
 *
 */
public class LogContext {

  public static final String DATASET_MDC = "datasetKey";

  public static void startDataset(String key) {
    MDC.put(DATASET_MDC, key);
  }

  public static void startDataset(UUID key) {
    MDC.put(DATASET_MDC, key.toString());
  }

  public static void endDataset() {
    MDC.remove(DATASET_MDC);
  }

}
