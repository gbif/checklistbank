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
