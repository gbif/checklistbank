package org.gbif.checklistbank.ws.nub;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Rank;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/species")
@Singleton
public class NubResource {

  private static final Logger LOG = LoggerFactory.getLogger(NubResource.class);

  private final NameUsageMatchingService matchingService;

  @Inject
  public NubResource(NameUsageMatchingService matchingService) {
    this.matchingService = matchingService;
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("match")
  public NameUsageMatch match(@QueryParam("name") String scientificName, @QueryParam("rank") String rank,
    @QueryParam("kingdom") String kingdom, @QueryParam("phylum") String phylum,
    @QueryParam("class") String clazz, @QueryParam("order") String order, @QueryParam("family") String family,
    @QueryParam("genus") String genus, @QueryParam("subgenus") String subgenus,
    @QueryParam("strict") Boolean strict, @QueryParam("verbose") Boolean verbose) {

    Rank r = parse(Rank.class, rank);

    LinneanClassification classification = new NameUsageMatch();
    classification.setKingdom(kingdom);
    classification.setPhylum(phylum);
    classification.setClazz(clazz);
    classification.setOrder(order);
    classification.setFamily(family);
    classification.setGenus(genus);
    classification.setSubgenus(subgenus);

    return  matchingService.match(scientificName, r, classification, bool(strict), bool(verbose));
  }

  private <T extends Enum> T parse(Class<T> clazz, String value) {
    if (!Strings.isNullOrEmpty(value)) {
      try {
        return VocabularyUtils.lookupEnum(value, clazz);
      } catch (Exception e) {
        // we expect badly formed enums and catch the exception
        LOG.debug("Bad {} given: {}", clazz.getSimpleName(), value);
      }
    }
    return null;
  }

  private boolean bool(Boolean bool) {
    return bool==null ? false : bool;
  }

}
