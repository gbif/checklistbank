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
package org.gbif.checklistbank.cli.config;

import org.gbif.common.search.solr.SolrConfig;
import org.gbif.common.search.solr.SolrServerType;

import javax.validation.constraints.Min;

import com.beust.jcommander.Parameter;

public class SolrConfiguration {

  @Parameter(names = {"--solr-server-type"})
  public SolrServerType serverType = SolrServerType.EMBEDDED;

  @Parameter(names = {"--solr-server-home"})
  public String serverHome;

  @Parameter(names = {"--solr-collection"})
  public String collection = "collection1";

  @Parameter(names = {"--solr-delete-on-exit"})
  public boolean deleteOnExit = false;

  @Parameter(names = {"--solr-id-field"})
  public String idField;

  @Parameter(names = {"--solr-sync-threads"})
  @Min(1)
  public int syncThreads = 1;

  public SolrConfig toSolrConfig() {
    SolrConfig solrConfig = new SolrConfig();
    solrConfig.setServerType(serverType);
    solrConfig.setServerHome(serverHome);
    solrConfig.setCollection(collection);
    solrConfig.setDeleteOnExit(deleteOnExit);
    solrConfig.setIdField(idField);

    return solrConfig;
  }
}
