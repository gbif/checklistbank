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
package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.service.checklistbank.DatasetMetricsService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/** Client-side implementation to the DatasetMetricsService. */
@RequestMapping("dataset")
public interface DatasetMetricsClient extends DatasetMetricsService {

  /**
   * Gets a DatasetMetrics by its dataset key.
   *
   * @param datasetKey key of dataset to get the metrics for
   * @return requested DatasetMetrics or null if none could be found
   */
  @RequestMapping(
      value = "{key}/metrics",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  DatasetMetrics get(@PathVariable("key") UUID datasetKey);

  @RequestMapping(
      value = "{key}/metrics/history",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<DatasetMetrics> list(@PathVariable("key") UUID datasetKey);
}
