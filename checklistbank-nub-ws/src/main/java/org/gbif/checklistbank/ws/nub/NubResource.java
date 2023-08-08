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

import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gbif.api.model.checklistbank.NameUsageMatch;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.gbif.checklistbank.utils.ParameterUtils.first;

@RestController
@RequestMapping(
  value = "/species",
  produces = {MediaType.APPLICATION_JSON_VALUE, "application/x-javascript"}
)
public class NubResource {

  private final NameUsageMatchingService2 matchingService;
  private final IdLookup lookup;

  @Autowired
  public NubResource(NameUsageMatchingService2 matchingService, IdLookup lookup) {
    this.matchingService = matchingService;
    this.lookup = lookup;
  }

  @Hidden
  @GetMapping(value = "lookup")
  public LookupUsage lookup(@RequestParam(value = "name", required = false) String canonicalName,
                            @RequestParam(value = "authorship", required = false) String authorship,
                            @RequestParam(value = "year", required = false) String year,
                            @RequestParam(value = "rank", required = false) Rank rank,
                            @RequestParam(value = "status", required = false) TaxonomicStatus status,
                            @RequestParam(value = "kingdom", required = false) Kingdom kingdom) {
    return lookup.match(canonicalName, authorship, year, rank, status, kingdom);
  }

  @Operation(
    operationId = "matchNames",
    summary = "Fuzzy name match service",
    description = "Fuzzy matches scientific names against the GBIF Backbone Taxonomy with the optional " +
      "classification provided. If a classification is provided and strict is not set to true, the default matching " +
      "will also try to match against these if no direct match is found for the name parameter alone.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0130"))
  )
  @Tag(name = "Searching names")
  @Parameters(
    value = {
      @Parameter(
        name = "name",
        description = "The scientific name to fuzzy match against. May include the authorship and year"
      ),
      @Parameter(name = "scientificName", hidden = true),
      @Parameter(
        name = "authorship",
        description = "The scientific name authorship to fuzzy match against."
      ),
      @Parameter(name = "scientificNameAuthorship", hidden = true),
      @Parameter(
        name = "rank",
        description = "Filters by taxonomic rank as given in our https://api.gbif.org/v1/enumeration/basic/Rank[Rank enum].",
        schema = @Schema(implementation = Rank.class)
      ),
      @Parameter(name = "taxonRank", hidden = true),
      @Parameter(
        name = "kingdom",
        description = "Kingdom to match."
      ),
      @Parameter(
        name = "phylum",
        description = "Phylum to match."
      ),
      @Parameter(
        name = "order",
        description = "Order to match."
      ),
      @Parameter(
        name = "class",
        description = "Class to match."
      ),
      @Parameter(
        name = "family",
        description = "Family to match."
      ),
      @Parameter(
        name = "genus",
        description = "Genus to match."
      ),
      @Parameter(
          name = "genericName",
          description = "Generic part of the name to match when given as atomised parts instead of the full name parameter."
      ),
      @Parameter(
        name = "specificEpithet",
        description = "Specific epithet to match."
      ),
      @Parameter(
        name = "infraspecificEpithet",
        description = "Infraspecific epithet to match."
      ),
      @Parameter(name = "classification", hidden = true),
      @Parameter(
        name = "strict",
        description = "If true it fuzzy matches only the given name, but never a taxon in the upper classification."
      ),
      @Parameter(
        name = "verbose",
        description = "If true it shows alternative matches which were considered but then rejected."
      )
    }
  )
  @ApiResponse(responseCode = "200", description = "Name usage suggestions found")
  @GetMapping(value = "match")
  public NameUsageMatch match(@RequestParam(value = "usageKey", required = false) Integer usageKey,
                              @RequestParam(value = "name", required = false) String scientificName2,
                              @RequestParam(value = "scientificName", required = false) String scientificName,
                              @RequestParam(value = "authorship", required = false) String authorship2,
                              @RequestParam(value = "scientificNameAuthorship", required = false) String authorship,
                              @RequestParam(value = "rank", required = false) String rank2,
                              @RequestParam(value = "taxonRank", required = false) String rank,
                              @RequestParam(value = "genericName", required = false) String genericName,
                              @RequestParam(value = "specificEpithet", required = false) String specificEpithet,
                              @RequestParam(value = "infraspecificEpithet", required = false) String infraspecificEpithet,
                              LinneanClassification classification,
                              @RequestParam(value = "strict", required = false) Boolean strict,
                              @RequestParam(value = "verbose", required = false) Boolean verbose) {
    return matchingService.match2(usageKey, first(scientificName, scientificName2), first(authorship, authorship2),
        genericName, specificEpithet, infraspecificEpithet,
        parseRank(first(rank, rank2)), classification, null, bool(strict), bool(verbose));
  }

  @Hidden
  @GetMapping(value = "match2")
  public NameUsageMatch2 match2(@RequestParam(value = "usageKey", required = false) Integer usageKey,
                                @RequestParam(value = "name", required = false) String scientificName2,
                                @RequestParam(value = "scientificName", required = false) String scientificName,
                                @RequestParam(value = "authorship", required = false) String authorship2,
                                @RequestParam(value = "scientificNameAuthorship", required = false) String authorship,
                                @RequestParam(value = "rank", required = false) String rank2,
                                @RequestParam(value = "taxonRank", required = false) String rank,
                                @RequestParam(value = "genericName", required = false) String genericName,
                                @RequestParam(value = "specificEpithet", required = false) String specificEpithet,
                                @RequestParam(value = "infraspecificEpithet", required = false) String infraspecificEpithet,
                                LinneanClassification classification,
                                // higher taxon ids to exclude from matching, see https://github.com/gbif/portal-feedback/issues/4361
                                @RequestParam(value = "exclude", required = false) Set<Integer> exclude,
                                @RequestParam(value = "strict", required = false) Boolean strict,
                                @RequestParam(value = "verbose", required = false) Boolean verbose) {
    return matchingService.v2(matchingService.match2(
        usageKey, first(scientificName, scientificName2), first(authorship, authorship2),
        genericName, specificEpithet, infraspecificEpithet,
        parseRank(first(rank, rank2)),
        classification, exclude, bool(strict), bool(verbose)));
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
