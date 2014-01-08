package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.service.checklistbank.DescriptionService;
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
 * Description resource.
 */
@Path("/description")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
public class DescriptionResource {

  private static final Logger LOG = LoggerFactory.getLogger(DescriptionResource.class);
  private final DescriptionService descriptionService;

  @Inject
  public DescriptionResource(DescriptionService descriptionService) {
    this.descriptionService = descriptionService;
  }

  /**
   * This retrieves a Description by its key from ChecklistBank.
   *
   * @return requested Description or null if none could be found
   */
  @GET
  @Path("{id}")
  @NullToNotFound
  public Description get(@PathParam("id") Integer key) {
    return descriptionService.get(key);
  }
}
