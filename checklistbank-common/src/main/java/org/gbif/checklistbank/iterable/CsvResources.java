package org.gbif.checklistbank.iterable;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import com.google.common.base.Splitter;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;
import org.gbif.utils.file.InputStreamUtils;

public class CsvResources {
  private static final Splitter csvSplitter = Splitter.on(";").trimResults();
  
  public static Stream<String[]> stream(String resourceName) throws IOException {
    URL url = Resources.getResource(resourceName);
    return stream(url.openStream());
  }
  
  public static Stream<String[]> stream(InputStream stream) {
    BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    return br.lines().map(l -> l.split(";", -1));
  }
}
