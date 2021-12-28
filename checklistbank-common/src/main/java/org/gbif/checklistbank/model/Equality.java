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
package org.gbif.checklistbank.model;

public enum Equality {
  EQUAL, DIFFERENT, UNKNOWN;

  public Equality and (Equality other) {
    switch (this) {
      case UNKNOWN:
        return other;
      case DIFFERENT:
        return DIFFERENT;
      case EQUAL:
        return other == DIFFERENT ? DIFFERENT : EQUAL;
      default:
        throw new IllegalStateException();
    }
  }
}
