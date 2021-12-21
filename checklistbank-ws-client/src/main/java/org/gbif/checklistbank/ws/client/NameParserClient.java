package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.ParsedName;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("parser/name")
public interface NameParserClient {

  @RequestMapping(
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<ParsedName> parseGet(@RequestParam("name") List<String> names);

  @RequestMapping(
    method = RequestMethod.POST,
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<ParsedName> parseJson(@RequestBody List<String> names);

  @RequestMapping(
    method = RequestMethod.POST,
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<ParsedName> parseFile(@RequestParam("names") MultipartFile namesFile);


  @RequestMapping(
    method = RequestMethod.POST,
    consumes = MediaType.TEXT_PLAIN_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<ParsedName> parsePlainText(@RequestBody String names);

}
