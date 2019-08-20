package org.gbif.checklistbank.nub;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.gbif.checklistbank.iterable.CsvResources;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.utils.file.InputStreamUtils;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
