package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.v2.NameUsageMatch2;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("species")
public interface NubResourceClient {

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