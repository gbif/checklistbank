package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.util.ExtraMediaTypes;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VernacularName resource.
 */
@Path("/vernacular_name")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
public class VernacularNameResource {

  private static final Logger LOG = LoggerFactory.getLogger(VernacularNameResource.class);
  private final VernacularNameService vernacularNameService;

  @Inject
  public VernacularNameResource(VernacularNameService vernacularNameService) {
    this.vernacularNameService = vernacularNameService;
  }

  /**
   * This retrieves an VernacularName by its key from ChecklistBank.
   *
   * @return requested VernacularName or null if none could be found
   */
  @GET
  @Path("{id}")
  @NullToNotFound
  public VernacularName get(@PathParam("id") Integer key) {
    return vernacularNameService.get(key);
  }
}
