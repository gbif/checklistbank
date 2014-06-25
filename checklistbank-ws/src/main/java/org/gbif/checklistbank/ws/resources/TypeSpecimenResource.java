package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
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
 * TypeSpecimen resource.
 */
@Path("/typeSpecimen")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
public class TypeSpecimenResource {

  private static final Logger LOG = LoggerFactory.getLogger(TypeSpecimenResource.class);
  private final TypeSpecimenService typeSpecimenService;

  @Inject
  public TypeSpecimenResource(TypeSpecimenService typeSpecimenService) {
    this.typeSpecimenService = typeSpecimenService;
  }

  /**
   * This retrieves an TypeSpecimen by its key from ChecklistBank.
   *
   * @return requested TypeSpecimen or null if none could be found
   */
  @GET
  @Path("{id}")
  @NullToNotFound
  public TypeSpecimen get(@PathParam("id") Integer key) {
    return typeSpecimenService.get(key);
  }
}
