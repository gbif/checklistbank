package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.common.Count;
import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.List;
import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;

/**
 * DatasetMetrics resource.
 */
@Path("/dataset")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
public class DatasetMetricsResource {

  @Inject
  private DatasetMetricsService service;

  @GET
  @Path("{key}/metrics")
  @NullToNotFound
  public DatasetMetrics get(@PathParam("key") UUID key) {
    return service.get(key);
  }

  @GET
  @Path("{key}/metrics/history")
  public List<DatasetMetrics> list(@PathParam("key") UUID key) {
    return service.list(key);
  }

  @GET
  @Path("{key}/metrics/constituents")
  public List<Count<UUID>> constituents(@PathParam("key") UUID key) {
    return service.constituents(key);
  }
}
