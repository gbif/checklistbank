package org.gbif.checklistbank.iterable;

import com.google.common.io.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class CsvResources {

  public static Stream<String[]> stream(String resourceName) throws IOException {
    URL url = Resources.getResource(resourceName);
    return stream(url.openStream());
  }
  
  public static Stream<String[]> stream(InputStream stream) {
    BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    return br.lines().map(l -> l.split(";", -1));
  }
}
