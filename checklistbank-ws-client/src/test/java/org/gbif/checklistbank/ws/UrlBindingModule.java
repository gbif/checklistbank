package org.gbif.checklistbank.ws;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import java.net.URI;

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
    bindConstant().annotatedWith(Names.named("checklistbank.search.ws.url")).to(uri.toString() + baseResourceUrl);
    bindConstant().annotatedWith(Names.named("checklistbank.ws.url")).to(uri.toString() + baseResourceUrl);
  }
}
