package org.gbif.checklistbank.ws.nub;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.v2.NameUsageMatch2;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.common.parsers.RankParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.nub.lookup.NameUsageMatchingService2;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupPassThru;
import org.gbif.nub.lookup.straight.LookupUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Path("/species")
@Singleton
public class NubResource {

  private static final Logger LOG = LoggerFactory.getLogger(NubResource.class);
  private static final List<Rank> REVERSED_DWC_RANKS = new ArrayList<>(Rank.DWC_RANKS);
  static {
    Collections.reverse(REVERSED_DWC_RANKS);
  }
  private final NameUsageMatchingService2 matchingService;
  private final IdLookup lookup;

  @Inject
  public NubResource(NameUsageMatchingService2 matchingService, IdLookup lookup) {
    this.matchingService = matchingService;
    this.lookup = lookup;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("lookup")
  public LookupUsage lookup(@QueryParam("name") String canonicalName,
                            @QueryParam("authorship") String authorship,
                            @QueryParam("year") String year,
                            @QueryParam("rank") Rank rank,
                            @QueryParam("status") TaxonomicStatus status,
                            @QueryParam("kingdom") Kingdom kingdom,
                            @QueryParam("verbose") Boolean verbose) {
    return lookup.match(canonicalName, authorship, year, rank, status, kingdom);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("match")
  public NameUsageMatch match(@QueryParam("name") String scientificName2, @QueryParam("scientificName") String scientificName,
                              @QueryParam("authorship") String authorship2, @QueryParam("scientificNameAuthorship") String authorship,
                              @QueryParam("rank") String rank2, @QueryParam("taxonRank") String rank,
                              @QueryParam("specificEpithet") String specificEpithet,
                              @QueryParam("infraspecificEpithet") String infraspecificEpithet,
                              @Context LinneanClassification classification,
                              @QueryParam("strict") Boolean strict, @QueryParam("verbose") Boolean verbose) {
    Rank r = parseRank(first(rank, rank2));
    return match(first(scientificName, scientificName2),
                 first(authorship, authorship2),
                 specificEpithet,
                 infraspecificEpithet,
                 r, classification, bool(strict), bool(verbose)
    );
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("match2")
  public NameUsageMatch2 match2(@QueryParam("name") String scientificName2, @QueryParam("scientificName") String scientificName,
                                @QueryParam("authorship") String authorship2, @QueryParam("scientificNameAuthorship") String authorship,
                                @QueryParam("rank") String rank2, @QueryParam("taxonRank") String rank,
                                @QueryParam("specificEpithet") String specificEpithet,
                                @QueryParam("infraspecificEpithet") String infraspecificEpithet,
                                @Context LinneanClassification classification,
                                @QueryParam("strict") Boolean strict, @QueryParam("verbose") Boolean verbose) {
    Rank r = parseRank(first(rank, rank2));
    return matchingService.v2(match(first(scientificName, scientificName2),
        first(authorship, authorship2),
        specificEpithet,
        infraspecificEpithet,
        r, classification, bool(strict), bool(verbose)
    ));
  }

  private NameUsageMatch match(String scientificName, String authorship, String specificEpithet, String infraSpecificEpithet,
                       Rank rank, LinneanClassification classification, Boolean strict, Boolean verbose) {
    if (!Strings.isNullOrEmpty(scientificName)) {
      // prefer the given scientificName and add authorship if not there yet.
      // Ignore atomized name parameters
      scientificName = appendAuthorship(scientificName, authorship);

    } else if (classification != null) {
      Rank clRank = lowestRank(classification);
      if (clRank != null) {
        // atomized binomial?
        if (!Strings.isNullOrEmpty(classification.getGenus()) && !Strings.isNullOrEmpty(specificEpithet)) {
          ParsedName pn = new ParsedName();
          pn.setGenusOrAbove(classification.getGenus());
          pn.setInfraGeneric(classification.getSubgenus());
          pn.setSpecificEpithet(specificEpithet);
          pn.setInfraSpecificEpithet(infraSpecificEpithet);
          pn.setRank(rank);
          pn.setAuthorship(authorship);
          scientificName = pn.canonicalNameComplete();
        } else {
          rank = clRank;
          scientificName = appendAuthorship(classification.getHigherRank(clRank), authorship);
        }
      }
    }
    return matchingService.match(scientificName, rank, classification, bool(strict), bool(verbose));
  }

  static String appendAuthorship(String scientificName, String authorship){
    if (!Strings.isNullOrEmpty(scientificName)
        && !Strings.isNullOrEmpty(authorship)
        && !scientificName.toLowerCase().contains(authorship.toLowerCase())) {
      return scientificName + " " + authorship;
    }
    return scientificName;
  }

  static Rank lowestRank(LinneanClassification cl){
    for (Rank r : REVERSED_DWC_RANKS) {
      if (!Strings.isNullOrEmpty(cl.getHigherRank(r))) {
        return r;
      }
    }
    return null;
  }

  static String first(String... values){
    if (values != null) {
      for (String val : values) {
        if (!Strings.isNullOrEmpty(val)) {
          return val;
        }
      }
    }
    return null;
  }

  private Rank parseRank(String value) throws IllegalArgumentException {
    if (!Strings.isNullOrEmpty(value)) {
      ParseResult<Rank> pr = RankParser.getInstance().parse(value);
      if (pr.isSuccessful()) {
        return pr.getPayload();
      }
    }
    return null;
  }

  private boolean bool(Boolean bool) {
    return bool == null ? false : bool;
  }

}
