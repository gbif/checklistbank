package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;
import org.gbif.ws.client.BaseWsGetClient;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the DatasetMetricsService.
 */
public class DatasetMetricsWsClient extends BaseWsGetClient<DatasetMetrics, UUID> implements DatasetMetricsService {
  private static final String METRICS_PATH = "metrics";

  private final GenericType<List<DatasetMetrics>> listType = new GenericType<List<DatasetMetrics>>() {};

  @Inject
  public DatasetMetricsWsClient(@ChecklistBankWs WebResource resource) {
    super(DatasetMetrics.class, resource.path(Constants.DATASET_PATH), null);
  }


  /**
   * Gets a DatasetMetrics by its dataset key.
   *
   * @param datasetKey key of dataset to get the metrics for
   *
   * @return requested DatasetMetrics or null if none could be found
   */
  @Override
  public DatasetMetrics get(UUID datasetKey) {
    return get(datasetKey.toString(), METRICS_PATH);
  }

  @Nullable
  @Override
  public List<DatasetMetrics> list(UUID datasetKey) {
    return get(listType, datasetKey.toString(), METRICS_PATH, "history");
  }

}
