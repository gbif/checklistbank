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
package org.gbif.checklistbank.elasticsearch;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration specific to interfacing with elastic search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Configuration setting to access and index data in Elasticsearch.
 */
public class ElasticsearchConfiguration {

  /**
   * Comma separate list of host names: http://es1:9200/,http://es2:9200/.
   */
  private String host;

  private int connectionTimeOut = 60000;

  private int socketTimeOut = 60000;

  private int connectionRequestTimeOut = 120000;

  /**
   * Alias to which the index will point to.
   */
  private String alias;

  /**
   * Index name for indexing tasks, if absent it will be later generated using the format alias_timestamp.
   */
  private String index;

  /**
   * Number of shards used when creating the index.
   */
  private int numberOfShards;

  /**
   * Classpath path to the mappings file.
   */
  private String mappingsFile;

  /**
   * Classpath path to the settings file.
   */
  private String settingsFile;

}
