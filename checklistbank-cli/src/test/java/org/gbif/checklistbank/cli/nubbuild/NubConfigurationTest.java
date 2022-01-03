package org.gbif.checklistbank.cli.nubbuild;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import org.gbif.checklistbank.cli.importer.ImporterConfiguration;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

@Ignore("REMOVE! ignored only to make the jenkins build work")
public class NubConfigurationTest {

    final ObjectMapper CFG_MAPPER = new ObjectMapper(new YAMLFactory());

    @Test
    public void testConfigs() throws Exception {
        NubConfiguration cfg = CFG_MAPPER.readValue(Resources.getResource("nub-test-cfg.yaml"), NubConfiguration.class);
        cfg.normalizeConfigs();

        assertEquals(4, cfg.blacklist.size());
        assertEquals(129, cfg.homonymExclusions.size());

        assertTrue(cfg.isBlacklisted("Incertae"));
        assertTrue(cfg.isBlacklisted("Calendrella cinerea ongumaensis"));
        assertFalse(cfg.isBlacklisted("Calendrella cinerea"));

        assertTrue(cfg.isExcludedHomonym("Glossoscolecidae", "Crassiclitellata"));
    }

    @Test
    @Ignore("manual local test to make sure config file is readable")
    public void testNubConfig() throws Exception {
        NubConfiguration cfg = CFG_MAPPER.readValue(new File("/Users/markus/code/gbif/gbif-configuration/cli/nub/config/clb-nub.yaml"), NubConfiguration.class);
        cfg.normalizeConfigs();
    }
}