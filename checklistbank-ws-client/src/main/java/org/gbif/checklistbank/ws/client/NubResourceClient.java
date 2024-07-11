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

import org.springframework.cloud.openfeign.SpringQueryMap;
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
      @RequestParam(value = "name", required = false) String scientificName2,
      @RequestParam(value = "scientificName", required = false) String scientificName,
      @RequestParam(value = "authorship", required = false) String authorship2,
      @RequestParam(value = "scientificNameAuthorship", required = false) String authorship,
      @RequestParam(value = "rank", required = false) String rank2,
      @RequestParam(value = "taxonRank", required = false) String rank,
      @RequestParam(value = "specificEpithet", required = false) String specificEpithet,
      @RequestParam(value = "infraspecificEpithet", required = false) String infraspecificEpithet,
      @SpringQueryMap LinneanClassification classification,
      @RequestParam(value = "strict", required = false) Boolean strict,
      @RequestParam(value = "verbose", required = false) Boolean verbose);

  @Override
  default NameUsageMatch match(
      String scientificName,
      Rank rank,
      LinneanClassification classification,
      boolean strict,
      boolean verbose) {
    return match(
        null,
        scientificName,
        null,
        null,
        null,
        rank.getMarker(),
        null,
        null,
        classification,
        strict,
        verbose);
  }

  @RequestMapping(
      value = "match2",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  NameUsageMatch2 match2(
      @RequestParam(value = "name", required = false) String scientificName2,
      @RequestParam(value = "scientificName", required = false) String scientificName,
      @RequestParam(value = "authorship", required = false) String authorship2,
      @RequestParam(value = "scientificNameAuthorship", required = false) String authorship,
      @RequestParam(value = "rank", required = false) String rank2,
      @RequestParam(value = "taxonRank", required = false) String rank,
      @RequestParam(value = "genericName", required = false) String genericName,
      @RequestParam(value = "specificEpithet", required = false) String specificEpithet,
      @RequestParam(value = "infraspecificEpithet", required = false) String infraspecificEpithet,
      @SpringQueryMap LinneanClassification classification,
      @RequestParam(value = "strict", required = false) Boolean strict,
      @RequestParam(value = "verbose", required = false) Boolean verbose);

  default NameUsageMatch2 match2(
      String scientificName,
      String authorship,
      String rank,
      String genericName,
      String specificEpithet,
      String infraspecificEpithet,
      LinneanClassification classification,
      Boolean strict,
      Boolean verbose) {
    return match2(
        scientificName,
        scientificName,
        authorship,
        authorship,
        rank,
        rank,
        genericName,
        specificEpithet,
        infraspecificEpithet,
        classification,
        strict,
        verbose);
  }
}
