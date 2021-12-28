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
 * Bundles a parsed name instance with a name usage instance when both are needed.
 */
public class ParsedNameUsageCompound {
  public final NameUsage usage;
  public final ParsedName parsedName;

  public ParsedNameUsageCompound(NameUsage usage, ParsedName parsedName) {
    this.usage = usage;
    this.parsedName = parsedName;
  }
}
