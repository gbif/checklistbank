package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.model.UsageCount;
import org.gbif.checklistbank.service.mybatis.mapper.UsageCountMapper;
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
 * A secret species resource to visualize taxonomies in treemaps.
 */
@Path("/treemap")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
public class TreemapResource {

  private final UsageCountMapper usageCountMapper;


  @Inject
  public TreemapResource(UsageCountMapper usageCountMapper) {
    this.usageCountMapper = usageCountMapper;
  }

  @GET
  @Path("root")
  public List<UsageCount> nubRoot() {
    return root(Constants.NUB_DATASET_KEY);
  }

  @GET
  @Path("root/{uuid}")
  public List<UsageCount> root(@PathParam("uuid") UUID datasetKey) {
    return usageCountMapper.root(datasetKey);
  }

  @GET
  @Path("children/{id}")
  public List<UsageCount> children(@PathParam("id") int parentKey) {
    return usageCountMapper.children(parentKey);
  }

}
