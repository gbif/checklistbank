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
package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.utils.NameParsers;
import org.gbif.checklistbank.ws.util.LineReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The scientific name parser exposed in the API.
 */
@RestController
@RequestMapping(
  value = "/parser/name",
  produces = {org.springframework.http.MediaType.APPLICATION_JSON_VALUE, "application/x-javascript"}
)
public class NameParserResource {

  private static final Logger LOG = LoggerFactory.getLogger(NameParserResource.class);
  private static final Splitter NEW_LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings().trimResults();

  /** Parsing names as GET query parameters. */
  @Operation(
    operationId = "parseName",
    summary = "Parse a scientific name",
    description = "Parses a scientific name string and returns the ParsedName version of it.\n\n" +
      "Accepts multiple parameters each with a single name. Make sure you URL encode the names properly."
  )
  @Tag(name = "Name parser")
  @Parameter(
    name = "name",
    description = "A scientific name to parse. Repeat to parse several names",
    explode = Explode.TRUE
  )
  @ApiResponse(responseCode = "200", description = "Parsed name usages")
  @GetMapping
  public List<ParsedName> parseGet(
      @RequestParam(value = "name", required = false) List<String> names) {
    if (names == null || names.isEmpty()) {
      return new ArrayList<>();
    }
    return parse(names.iterator());
  }

  /**
   * Parsing names as a JSON array.
   */
  @Operation(
    operationId = "parseName",
    summary = "Parse a list of scientific names",
    description = "Parses a list of scientific names supplied as a JSON list, a form request or a plain text file " +
      "with Unix (\\n) line endings.  In all cases the names should use UTF-8 encoding.\n" +
      "\n" +
      "```\n" +
      "curl -X POST -H 'Content-Type: application/json' --data-binary @scientific_names.json https://api.gbif.org/v1/species/parser/name\n" +
      "curl -F names=@scientific_names.txt https://api.gbif.org/v1/species/parser/name\n" +
      "curl -X POST -H 'Content-Type: text/plain' --data-binary @scientific_names.txt https://api.gbif.org/v1/species/parser/name\n" +
      "```\n" +
      ""
  )
  @Tag(name = "Name parser")
  @ApiResponse(responseCode = "200", description = "Parsed name usages")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<ParsedName> parseJson(
    @RequestBody @Parameter(description = "A list of names to parse, such as `[\"First name\", \"Second name\"]`.") List<String> names) {
    return parse(names.iterator());
  }

  /**
   * Parsing names by uploading a plain UTF-8 text file using one line per scientific name.
   * <pre>
   * curl -F names=@scientific_names.txt https://api.gbif.org/v1/species/parser/name
   * </pre>
   */
  @Operation(operationId = "parseName")
  @Tag(name = "Name parser")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public List<ParsedName> parseFile(
    @RequestPart("names") @Parameter(description = "Scientific names to parse, one per line, with Unix (\\n) newlines.") MultipartFile namesFile)
    throws IOException {
    if (namesFile == null) {
      LOG.debug("No names file uploaded");
      return Lists.newArrayList();
    }
    LineReader iter = new LineReader(namesFile.getInputStream(), StandardCharsets.UTF_8);
    return parse(iter.iterator());
  }


  /**
   * Parsing names by posting plain text content using one line per scientific name.
   * Make sure to preserve new lines (\n) in the posted data, for example use --data-binary with curl:
   * <pre>
   * curl -X POST -H "Content-Type:text/plain" --data-binary @scientific_names.txt https://api.gbif.org/v1/species/parser/name
   * </pre>
   */
  @Operation(operationId = "parseName")
  @Tag(name = "Name parser")
  @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
  public List<ParsedName> parsePlainText(@RequestBody @Parameter(description = "Scientific names to parse, one per line, with Unix (\\n) newlines.") String names) {
    return parse(NEW_LINE_SPLITTER.split(Strings.nullToEmpty(names)).iterator());
  }

  private List<ParsedName> parse(Iterator<String> iter) {
    int counter = 0;
    int unparsable = 0;
    List<ParsedName> pnames = Lists.newArrayList();

    while (iter.hasNext()) {
      final String name = iter.next();
      ParsedName pn = NameParsers.INSTANCE.parseQuietly(name);
      pnames.add(pn);
      counter++;
      if (!pn.getType().isParsable()) {
        unparsable++;
      }
    }

    LOG.debug("Parsed {} names out of which {} are unparsable", counter, unparsable);
    return pnames;
  }
}
