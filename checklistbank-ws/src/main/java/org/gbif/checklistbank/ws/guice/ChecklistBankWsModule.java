package org.gbif.checklistbank.ws.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import java.util.Properties;

/**
 * Only binds properties to named values.
 */
public class ChecklistBankWsModule extends AbstractModule {

    private final Properties properties;

    public ChecklistBankWsModule(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure() {
        Names.bindProperties(binder(), properties);
    }
}
