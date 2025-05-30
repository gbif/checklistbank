/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.io.File;

import org.junit.Test;
import org.junit.jupiter.api.Disabled;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NubConfigurationTest {

    final ObjectMapper CFG_MAPPER = new ObjectMapper(new YAMLFactory());

    @Test
    public void testConfigs() throws Exception {
        NubConfiguration cfg = CFG_MAPPER.readValue(Resources.getResource("nub-test-cfg.yaml"), NubConfiguration.class);
        cfg.normalizeConfigs();

        assertEquals(5, cfg.blacklist.size());
        assertEquals(129, cfg.homonymExclusions.size());

        SrcUsage u = new SrcUsage();
        u.scientificName="Incertae";
        assertTrue(cfg.isBlacklisted(u));

        u.scientificName="Calendrella cinerea ongumaensis";
        assertTrue(cfg.isBlacklisted(u));

        u.scientificName="Calendrella cinerea";
        assertFalse(cfg.isBlacklisted(u));

        u.scientificName="Phimenes de Saussure, 1855";
        assertTrue(cfg.isBlacklisted(u));

        u.scientificName="Phimenes";
        assertFalse(cfg.isBlacklisted(u));

        u.parsedName=new ParsedName();
        u.parsedName.setGenusOrAbove("Phimenes");
        assertFalse(cfg.isBlacklisted(u));
        u.parsedName.setAuthorship("de Saussure, 1855");
        System.out.println(u.parsedName.canonicalNameComplete());
        assertTrue(cfg.isBlacklisted(u));

        assertTrue(cfg.isExcludedHomonym("Glossoscolecidae", "Crassiclitellata"));
    }

    @Test
    @Disabled("manual local test to make sure config file is readable")
    public void testNubConfig() throws Exception {
        NubConfiguration cfg = CFG_MAPPER.readValue(new File("/Users/markus/code/gbif/gbif-configuration/cli/nub/config/clb-nub.yaml"), NubConfiguration.class);
        cfg.normalizeConfigs();
    }
}