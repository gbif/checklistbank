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
package org.gbif.checklistbank.index;

import org.gbif.checklistbank.index.model.NameUsageAvro;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;

import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

@Data
@Builder
public class NameUsagesEsIndexingClient {

  private final ElasticsearchClient elasticsearchClient;

  private final String indexName;

  @SneakyThrows
  private BulkResponse bulkRequest(List<BulkOperation> operations) {
    return elasticsearchClient.bulk(new BulkRequest.Builder().index(indexName).operations(operations).build());
  }

  private void throwIfError(DeleteByQueryResponse response) {
    if (!response.failures().isEmpty()) {
      throw new RuntimeException("Error creating " + response);
    }
  }

  private void throwIfError(BulkResponse response) {
    if (response.errors()) {
      throw new RuntimeException("Error creating " + response);
    }
  }

  @SneakyThrows
  public void deleteByDatasetKey(UUID datasetKey) {
    throwIfError(elasticsearchClient.deleteByQuery(dq -> dq.query(q -> q.match(m -> m.field("datasetKey").query(datasetKey.toString())))));
  }

  public void  bulkDelete(List<Integer> ids) {
    throwIfError(bulkRequest(ids.stream()
                                .map(id -> new BulkOperation.Builder().delete(new DeleteOperation.Builder().id(id.toString()).build()).build())
                                .collect(Collectors.toList())));
  }

  public void bulkAdd(List<NameUsageAvro> usages) {
    throwIfError(bulkRequest(usages.stream()
                               .map(u -> new BulkOperation.Builder().index(new IndexOperation.Builder<>().id(u.getKey().toString()).document(u).build()).build())
                               .collect(Collectors.toList())));
  }

}
