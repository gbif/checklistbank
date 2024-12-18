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
  List<ParsedName> parseGet(@RequestParam(value = "name", required = false) List<String> names);

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
  List<ParsedName> parseFile(@RequestPart("names") MultipartFile namesFile);


  @RequestMapping(
    method = RequestMethod.POST,
    consumes = MediaType.TEXT_PLAIN_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<ParsedName> parsePlainText(@RequestBody String names);

}
