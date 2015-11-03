package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.nameparser.NameParser;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

public class ClbConfigurationTest {

    @Test(expected = CreationException.class)
    public void testBadTimeout() throws Exception {
        ClbConfiguration cfg = new ClbConfiguration();
        cfg.parserTimeout = 1;
        Injector inj = Guice.createInjector(cfg.createServiceModule());
        inj.getInstance(NameParser.class);
    }
}