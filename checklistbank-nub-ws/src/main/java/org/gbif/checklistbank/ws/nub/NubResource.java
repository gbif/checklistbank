/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.ws.nub;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.v2.NameUsageMatch2;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.common.parsers.RankParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.nub.lookup.NameUsageMatchingService2;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.LookupUsage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;

@RestController
@RequestMapping(
  value = "/species",
  produces = {MediaType.APPLICATION_JSON_VALUE, "application/x-javascript"}
)
public class NubResource {

  private static final List<Rank> REVERSED_DWC_RANKS = new ArrayList<>(Rank.DWC_RANKS);
  static {
    Collections.reverse(REVERSED_DWC_RANKS);
  }
  private final NameUsageMatchingService2 matchingService;
  private final IdLookup lookup;

  @Autowired
  public NubResource(NameUsageMatchingService2 matchingService, IdLookup lookup) {
    this.matchingService = matchingService;
    this.lookup = lookup;
  }

  @GetMapping( value = "lookup")
  public LookupUsage lookup(@RequestParam(value = "name", required = false) String canonicalName,
                            @RequestParam(value = "authorship", required = false) String authorship,
                            @RequestParam(value = "year", required = false) String year,
                            @RequestParam(value = "rank", required = false) Rank rank,
                            @RequestParam(value = "status", required = false) TaxonomicStatus status,
                            @RequestParam(value = "kingdom", required = false) Kingdom kingdom) {
    return lookup.match(canonicalName, authorship, year, rank, status, kingdom);
  }

  @GetMapping( value = "match")
  public NameUsageMatch match(@RequestParam(value = "name", required = false) String scientificName2,
                              @RequestParam(value = "scientificName", required = false) String scientificName,
                              @RequestParam(value = "authorship", required = false) String authorship2,
                              @RequestParam(value = "scientificNameAuthorship", required = false) String authorship,
                              @RequestParam(value = "rank", required = false) String rank2,
                              @RequestParam(value ="taxonRank", required = false) String rank,
                              @RequestParam(value = "specificEpithet", required = false) String specificEpithet,
                              @RequestParam(value = "infraspecificEpithet", required = false) String infraspecificEpithet,
                              LinneanClassification classification,
                              @RequestParam(value = "strict", required = false) Boolean strict,
                              @RequestParam(value ="verbose", required = false) Boolean verbose) {
    Rank r = parseRank(first(rank, rank2));
    return match(first(scientificName, scientificName2),
                 first(authorship, authorship2),
                 specificEpithet,
                 infraspecificEpithet,
                 r, classification, bool(strict), bool(verbose)
    );
  }

  @GetMapping( value = "match2")
  public NameUsageMatch2 match2(@RequestParam(value ="name", required = false) String scientificName2,
                                @RequestParam(value ="scientificName", required = false) String scientificName,
                                @RequestParam(value ="authorship", required = false) String authorship2,
                                @RequestParam(value ="scientificNameAuthorship", required = false) String authorship,
                                @RequestParam(value ="rank", required = false) String rank2,
                                @RequestParam(value = "taxonRank", required = false) String rank,
                                @RequestParam(value = "specificEpithet", required = false) String specificEpithet,
                                @RequestParam(value = "infraspecificEpithet", required = false) String infraspecificEpithet,
                                LinneanClassification classification,
                                @RequestParam(value ="strict", required = false) Boolean strict,
                                @RequestParam(value ="verbose", required = false) Boolean verbose) {
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
    return bool != null && bool;
  }

}
