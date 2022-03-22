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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

@Data
@Builder
public class NameUsagesEsIndexingClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
  }

  private final RestClient restClient;

  private final String indexName;

  @SneakyThrows
  public String indexRequest(String id, String jsonData) {
    return "{ \"index\" : {\"_id\" : \"" + id + "\" } }\n" + jsonData;
  }

  @SneakyThrows
  public String indexRequest(String id, Object value) {
    return indexRequest(id, MAPPER.writeValueAsString(value));
  }

  @SneakyThrows
  public String deleteRequest(String id) {
    return "{ \"delete\" : {\"_id\" : \"" + id + "\" } }";
  }

  @SneakyThrows
  public Response bulkRequest(String requests) {
    Request request = new Request("POST", indexName + "/_bulk");
    request.setJsonEntity(requests + "\n"); //bulk request must be terminated by a newline
    request.setOptions(RequestOptions.DEFAULT);
    return restClient.performRequest(request);
  }

  @SneakyThrows
  public Response deleteByQuery(String jsonQuery) {
    Request request = new Request("POST",indexName + "/_delete_by_query");
    request.setOptions(RequestOptions.DEFAULT);
    request.setJsonEntity(jsonQuery);
    return restClient.performRequest(request);
  }

  private void throwIfError(Response response) {
    if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
      throw new RuntimeException("Error creating " + response);
    }
  }

  public void deleteByDatasetKey(UUID datasetKey) {
    throwIfError(deleteByQuery("{\n"
                         + "  \"query\": {\n"
                         + "    \"match\": {\n"
                         + "      \"datasetKey\": \"" + datasetKey.toString() + "\"\n"
                         + "    }\n"
                         + "  }\n"
                         + "}"));
  }

  public void  bulkDelete(List<Integer> ids) {
    throwIfError(bulkRequest(ids.stream()
                                .map(id -> deleteRequest(id.toString()))
                                .collect(Collectors.joining("\n"))));
  }

  public void bulkAdd(List<NameUsageAvro> usages) {
    throwIfError(bulkRequest(usages.stream()
                               .map(u -> indexRequest(u.getKey().toString(), u))
                               .collect(Collectors.joining("\n"))));
  }


}
