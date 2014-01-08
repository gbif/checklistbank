package org.gbif.nub.export;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;

import java.util.Properties;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

/**
 * Guice module setting up all dependencies to expose the NubGenerate.
 */
public class ExporterModule extends PrivateModule {
  private Properties properties;

  public ExporterModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    install(new ChecklistBankServiceMyBatisModule(properties));
    bind(NubGitExporter.class).in(Scopes.SINGLETON);
    expose(NubGitExporter.class);
    Names.bindProperties(binder(), properties);
  }

}
