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

import java.util.Arrays;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ClbDbLoadTestDataBeforeAll implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    if(!hasClbDbLoadTestDataBeforeEach(extensionContext)) {
      ClbDbLoadTestDataBeforeEach.before(extensionContext);
    }
  }

  /**
   * Hash and ExtendedWith annotation with the value ClbDbLoadTestDataBeforeEach.
   */
  private boolean hasClbDbLoadTestDataBeforeEach(ExtensionContext extensionContext) {
    return extensionContext.getTestClass()
            .map(c -> Arrays.stream(c.getAnnotationsByType(ExtendWith.class))
                      .anyMatch(ew -> Arrays.stream(ew.value())
                                      .anyMatch(cl -> cl == ClbDbLoadTestDataBeforeEach.class)
                      )
            ).orElse(false);
  }
}
