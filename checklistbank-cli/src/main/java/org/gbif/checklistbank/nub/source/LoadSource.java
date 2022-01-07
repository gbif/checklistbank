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
package org.gbif.checklistbank.nub.source;

import java.util.concurrent.Callable;

/**
 * Job that initializes a NubSource
 */
class LoadSource implements Callable<NubSource> {
  private final NubSource src;

  public LoadSource(NubSource src) {
    this.src = src;
  }

  @Override
  public NubSource call() throws Exception {
    src.init(true, false);
    return src;
  }
}