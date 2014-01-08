package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.service.checklistbank.ReferenceService;
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
 * Reference resource.
 */
@Path("/reference")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
public class ReferenceResource {

  private static final Logger LOG = LoggerFactory.getLogger(ReferenceResource.class);
  private final ReferenceService referenceService;

  @Inject
  public ReferenceResource(ReferenceService referenceService) {
    this.referenceService = referenceService;
  }

  /**
   * This retrieves an Reference by its key from ChecklistBank.
   *
   * @return requested Reference or null if none could be found
   */
  @GET
  @Path("{id}")
  @NullToNotFound
  public Reference get(@PathParam("id") Integer key) {
    return referenceService.get(key);
  }
}
