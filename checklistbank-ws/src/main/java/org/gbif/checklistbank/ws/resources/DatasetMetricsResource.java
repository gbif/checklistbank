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
package org.gbif.checklistbank.ws.resources;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.service.checklistbank.DatasetMetricsService;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * DatasetMetrics resource.
 */
@Tag(name = "Datasets") // Part of the Registry documentation.
@RestController
@RequestMapping(
  value = "/dataset",
  produces = {MediaType.APPLICATION_JSON_VALUE, "application/x-javascript"}
)
public class DatasetMetricsResource {

  private final DatasetMetricsService service;

  @Autowired
  public DatasetMetricsResource(DatasetMetricsService service) {
    this.service = service;
  }

  @Operation(
    operationId = "getDatasetMetrics",
    summary = "Checklist dataset metrics",
    description = "Get various metrics for a checklist. Metrics include the number of species, the number of synonyms, " +
      "counts by rank, counts by vernacular name language, etc."
  )
  @Parameter(
    name = "key",
    description = "Dataset key.",
    in = ParameterIn.PATH)
  @GetMapping("{key}/metrics")
  @NullToNotFound("/dataset/{key}/metrics")
  public DatasetMetrics get(@PathVariable("key") UUID key) {
    return service.get(key);
  }

  @Hidden
  @GetMapping("{key}/metrics/history")
  public List<DatasetMetrics> list(@PathVariable("key") UUID key) {
    return service.list(key);
  }
}
