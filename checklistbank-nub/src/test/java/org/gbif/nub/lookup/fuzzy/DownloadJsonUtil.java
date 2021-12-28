package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.file.InputStreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.Assertions;

/**
 * Manual utility to generate a list of unique canonical names from the test index json files
 * and more importantly to download new species match json files for a list of names to query the backbone for.
 */
public class DownloadJsonUtil {
  public static Set<String> extract() {
    Set<String> names = new TreeSet<>();
    InputStreamUtils isu = new InputStreamUtils();
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    int id = 0;
    while (id < 150) {
      String file = "index/nub"+id+".json";
      InputStream json = isu.classpathStream(file);
      if (json != null) {
        try {
          NameUsageMatch m = mapper.readValue(json, NameUsageMatch.class);
          if (m.getUsageKey() != null) {
            names.add(m.getCanonicalName());
          } else {
            for (NameUsageMatch m2 : m.getAlternatives()) {
              if (m2.getUsageKey() != null) {
                names.add(m2.getCanonicalName());
                break;
              }
            }
          }

          if (m.getAlternatives() != null && m.getAlternatives().size() >= 4) {
            if (m.getAlternatives().get(3).getUsageKey() != null) {
              names.add(m.getAlternatives().get(3).getCanonicalName());
            }
            if (m.getAlternatives().size() >= 10) {
              if (m.getAlternatives().get(9).getUsageKey() != null) {
                names.add(m.getAlternatives().get(9).getCanonicalName());
              }
            }
          }

        } catch (IOException e) {
          Assertions.fail("Failed to read " + file + ": " + e.getMessage());
        }
      }
      id++;
    }
    return names;
  }

  public static void download(Set<String> names) throws IOException {
    HttpUtil http = new HttpUtil(new DefaultHttpClient());
    int i=1;
    for (String n : names) {
      File json = new File("/Users/markus/nub/nub"+ i++ +".json");
      String url = "http://api.gbif-uat.org/v1/species/match?verbose=true&name="+n.replaceAll(" ", "%20");
      System.out.println(url);
      http.download(url, json);
    }
  }

  public static void main(String[] args) throws IOException {
    download(extract());
  }

}
