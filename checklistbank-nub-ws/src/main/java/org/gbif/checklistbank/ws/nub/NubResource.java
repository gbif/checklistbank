package org.gbif.checklistbank.ws.nub;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nub.lookup.NameUsageMatch2;
import org.gbif.nub.lookup.NameUsageMatchingService2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/species")
@Singleton
public class NubResource {

  private static final Logger LOG = LoggerFactory.getLogger(NubResource.class);

  private final NameUsageMatchingService2 matchingService;

  @Inject
  public NubResource(NameUsageMatchingService2 matchingService) {
    this.matchingService = matchingService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("match")
  public NameUsageMatch match(@QueryParam("name") String scientificName, @QueryParam("rank") String rank,
                              @Context LinneanClassification classification,
                              @QueryParam("strict") Boolean strict, @QueryParam("verbose") Boolean verbose) {
    Rank r = parse(Rank.class, rank);
    return matchingService.match(scientificName, r, classification, bool(strict), bool(verbose));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("match2")
  public NameUsageMatch2 match2(@QueryParam("name") String scientificName, @QueryParam("rank") String rank,
                                @Context LinneanClassification classification,
                                @QueryParam("strict") Boolean strict, @QueryParam("verbose") Boolean verbose) {
    Rank r = parse(Rank.class, rank);
    return matchingService.v2(matchingService.match(scientificName, r, classification, bool(strict), bool(verbose)));
  }

  private <T extends Enum> T parse(Class<T> clazz, String value) throws IllegalArgumentException {
    if (!Strings.isNullOrEmpty(value)) {
      return VocabularyUtils.lookupEnum(value, clazz);
    }
    return null;
  }

  private boolean bool(Boolean bool) {
    return bool == null ? false : bool;
  }

}
