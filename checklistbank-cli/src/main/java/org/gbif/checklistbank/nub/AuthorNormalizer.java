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
package org.gbif.checklistbank.nub;

import org.gbif.checklistbank.iterable.CsvResources;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * Used to normalize authors in the Backbone.
 * See https://github.com/gbif/checklistbank/issues/88
 */
public class AuthorNormalizer {
  private static final Logger LOG = LoggerFactory.getLogger(AuthorNormalizer.class);
  private static final String FILENAME = "backbone/authorships.csv";
  // externalize to a resource file when we get more lookups
  private final Map<String, String> lookup;
  
  static public AuthorNormalizer create() {
    return new AuthorNormalizer();
  }
  
  private AuthorNormalizer() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
    try {
      CsvResources.stream(FILENAME).forEach(row -> {
        if (row != null && row.length > 1) {
          builder.put(norm(row[0]), row[1]);
        }
      });
    } catch (IOException e) {
      LOG.warn("Failed to read assertion resource {}", FILENAME, e);
    }
    lookup = builder.build();
  }
  
  private static String norm(String x) {
    return x == null ? null : x.toLowerCase().trim();
  }
  
  private String lookup(String author) {
    return lookup.getOrDefault(norm(author), author);
  }

  public void normalize(SrcUsage u) {
    if (u.parsedName.hasAuthorship()) {
      u.parsedName.setAuthorship(lookup(u.parsedName.getAuthorship()));
      u.parsedName.setBracketAuthorship(lookup(u.parsedName.getBracketAuthorship()));
      // update scientific name?
      if (u.parsedName.isParsableType() && u.parsedName.isParsed()) {
        u.scientificName = u.parsedName.canonicalNameComplete();
      }
    }
  }
}
