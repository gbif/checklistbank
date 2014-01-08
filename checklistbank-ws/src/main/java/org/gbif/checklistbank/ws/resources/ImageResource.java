package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.Image;
import org.gbif.api.service.checklistbank.ImageService;
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
@Path("/image")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
public class ImageResource {

  private static final Logger LOG = LoggerFactory.getLogger(ImageResource.class);
  private final ImageService imageService;

  @Inject
  public ImageResource(ImageService imageService) {
    this.imageService = imageService;
  }

  /**
   * This retrieves an Image by its key from ChecklistBank.
   *
   * @return requested Image or null if none could be found
   */
  @GET
  @Path("{id}")
  @NullToNotFound
  public Image get(@PathParam("id") Integer key) {
    return imageService.get(key);
  }
}
