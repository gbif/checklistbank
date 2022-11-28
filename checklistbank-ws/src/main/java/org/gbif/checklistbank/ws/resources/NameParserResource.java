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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.utils.NameParsers;
import org.gbif.checklistbank.ws.util.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
  @GetMapping
  public List<ParsedName> parseGet(
      @RequestParam(value = "name", required = false) List<String> names) {
    if (names == null || names.isEmpty()) {
      return new ArrayList<>();
    }
    return parse(names.iterator());
  }

  /**
   * Parsing names as a json array.
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<ParsedName> parseJson(@RequestBody List<String> names) {
    return parse(names.iterator());
  }

  /**
   * Parsing names by uploading a plain UTF-8 text file using one line per scientific name.
   * <pre>
   * curl -F names=@scientific_names.txt http://apidev.gbif.org/parser/name
   * </pre>
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public List<ParsedName> parseFile(@RequestPart("names") MultipartFile namesFile) throws IOException {
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
   * curl POST -H "Content-Type:text/plain" --data-binary @scientific_names.txt http://apidev.gbif.org/parser/name
   * </pre>
   */
  @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
  public List<ParsedName> parsePlainText(@RequestBody String names) {
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
