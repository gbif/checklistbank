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

import org.gbif.api.model.checklistbank.*;
import org.gbif.api.model.checklistbank.search.*;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.SearchResponse;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RequestMapping("species")
public interface SpeciesResourceClient {

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<NameUsage> list(
      @RequestParam(value = "datasetKey", required = false) Set<UUID> datasetKeys,
      @RequestParam(value = "sourceId", required = false) String sourceId,
      @RequestParam(value = "name", required = false) String canonicalName,
      @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  NameUsage get(@PathVariable("id") int usageKey);

  @RequestMapping(
      value = "{id}/metrics",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  NameUsageMetrics getMetrics(@RequestParam(value = "id", required = false) int usageKey);

  @RequestMapping(
      value = "{id}/name",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  ParsedName getParsedName(@RequestParam(value = "id", required = false) int usageKey);

  @RequestMapping(
      value = "{id}/verbatim",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  VerbatimNameUsage getVerbatim(@RequestParam(value = "id", required = false) int usageKey);

  @RequestMapping(
      value = "{id}/children",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<NameUsage> listChildren(
      @RequestParam(value = "id", required = false) int parentKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}/synonyms",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<NameUsage> listSynonyms(
      @RequestParam(value = "id", required = false) int usageKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}/vernacularNames",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<VernacularName> listVernacularNamesByNameUsage(
      @RequestParam(value = "id", required = false) int usageKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}/typeSpecimens",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<TypeSpecimen> listTypeSpecimensByNameUsage(
      @PathVariable("id") int usageKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}/speciesProfiles",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<SpeciesProfile> listSpeciesProfilesByNameUsage(
      @PathVariable("id") int usageKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}/references",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<Reference> listReferencesByNameUsage(
      @PathVariable("id") int usageKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}/media",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<NameUsageMediaObject> listImagesByNameUsage(
      @PathVariable("id") int usageKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}/descriptions",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<Description> listDescriptionsByNameUsage(
      @PathVariable("id") int usageKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}/toc",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  TableOfContents get(@PathVariable("id") Integer key);

  @RequestMapping(
      value = "{id}/distributions",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<Distribution> listDistributionsByNameUsage(
      @PathVariable("id") int usageKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}/identifier",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<Identifier> listIdentifierByNameUsage(
      @PathVariable("id") int usageKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "{id}/related",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<NameUsage> listRelatedByNameUsage(
      @PathVariable("id") int usageKey,
      @SpringQueryMap Pageable page,
      @RequestParam(value = "datasetKey", required = false) Set<UUID> datasetKeys);

  @RequestMapping(
      value = "{id}/combinations",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<NameUsage> listCombinations(@PathVariable("id") int basionymKey);

  @RequestMapping(
      value = "{id}/parents",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<NameUsage> listParentsByNameUsage(
      @PathVariable("id") int usageKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "root/{datasetKey}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<NameUsage> listRootUsages(
      @PathVariable("datasetKey") UUID datasetKey, @SpringQueryMap Pageable page);

  @RequestMapping(
      value = "search",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> search(
      @SpringQueryMap NameUsageSearchRequest searchRequest);

  @RequestMapping(
      value = "suggest",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<NameUsageSuggestResult> suggest(
      @SpringQueryMap NameUsageSuggestRequest searchSuggestRequest);
}
