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

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;

/**
 * Adds a parsed name instance to the name usage class for cases when both are needed.
 * Uses inheritance so mappers can return this easily.
 */
public class ParsedNameUsage extends NameUsage {
  private ParsedName parsedName;

  public ParsedName getParsedName() {
    return parsedName;
  }

  public void setParsedName(ParsedName parsedName) {
    this.parsedName = parsedName;
  }
}
