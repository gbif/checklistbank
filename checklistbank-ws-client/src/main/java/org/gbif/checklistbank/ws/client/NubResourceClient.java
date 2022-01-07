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
package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.v2.NameUsageMatch2;
import org.gbif.api.vocabulary.Rank;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("species")
public interface NubResourceClient extends NameUsageMatchingService {

  @RequestMapping(
      value = "match",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  NameUsageMatch match(
      @RequestParam("name") String scientificName2,
      @RequestParam("scientificName") String scientificName,
      @RequestParam("authorship") String authorship2,
      @RequestParam("scientificNameAuthorship") String authorship,
      @RequestParam("rank") String rank2,
      @RequestParam("taxonRank") String rank,
      @RequestParam("specificEpithet") String specificEpithet,
      @RequestParam("infraspecificEpithet") String infraspecificEpithet,
      LinneanClassification classification, // TODO: argument resolver needed?
      @RequestParam("strict") Boolean strict,
      @RequestParam("verbose") Boolean verbose);

  @Override
  default NameUsageMatch match(
      String scientificName,
      Rank rank,
      LinneanClassification classification,
      boolean strict,
      boolean verbose) {
    return match(null, scientificName, null, null, null,
        rank.getMarker(), null, null, classification, strict, verbose);
  }

  @RequestMapping(
      value = "match2",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  NameUsageMatch2 match2(
      @RequestParam("name") String scientificName2,
      @RequestParam("scientificName") String scientificName,
      @RequestParam("authorship") String authorship2,
      @RequestParam("scientificNameAuthorship") String authorship,
      @RequestParam("rank") String rank2,
      @RequestParam("taxonRank") String rank,
      @RequestParam("specificEpithet") String specificEpithet,
      @RequestParam("infraspecificEpithet") String infraspecificEpithet,
      LinneanClassification classification, // TODO: argument resolver needed?
      @RequestParam("strict") Boolean strict,
      @RequestParam("verbose") Boolean verbose);
}
