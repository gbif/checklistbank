package org.gbif.checklistbank.ws.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class BaseClientIT {

  protected final String apiUrl;

  protected BaseClientIT() throws IOException {
    Properties props = new Properties();
    InputStream is = ClassLoader.getSystemResourceAsStream("checklistbank-test.properties");
    props.load(is);

    apiUrl = props.getProperty("checklistbank.api.url");
  }
}
