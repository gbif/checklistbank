package org.gbif.checklistbank.ws.guice;

import org.gbif.nameparser.NameParser;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Binds and exposes a name parser reading the generic and suprageneric names from rs.gbif.org dictionaries.
 */
public class NameParserModule extends AbstractModule {

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  public NameParser provideParser() {
    NameParser parser = new NameParser();
    parser.getNormalisedNameParser().readMonomialsRsGbifOrg();
    return parser;
  }
}
