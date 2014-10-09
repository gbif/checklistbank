package org.gbif.checklistbank.ws;

import java.net.URI;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class UrlBindingModule extends AbstractModule {

  private URI uri;
  private String baseResourceUrl;

  public UrlBindingModule(URI uri, String baseResourceUrl) {
    this.uri = uri;
    this.baseResourceUrl = baseResourceUrl;
  }

  /**
   * Configures a {@link com.google.inject.Binder} via the exposed methods.
   */
  @Override
  protected void configure() {
    bindConstant().annotatedWith(Names.named("checklistbank.ws.url")).to(uri.toString() + baseResourceUrl);
  }
}
