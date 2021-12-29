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
package org.gbif.checklistbank.index.model;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.model.UsageExtensions;

import java.util.List;

import javax.annotation.Nullable;

/**
 * A name usage with all associated data we index in solr
 */
public class SolrUsage {
  public final NameUsage usage;
  public final List<Integer> parents;
  public final @Nullable UsageExtensions extensions;

  public SolrUsage(NameUsage usage, List<Integer> parents, UsageExtensions extensions) {
    this.usage = usage;
    this.parents = parents;
    this.extensions = extensions;
  }
}