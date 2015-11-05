package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import org.junit.Test;

/**
 * Simply checks no exceptions occurr when loading the standard squirrelts dataset.
 */
public class DbLoaderTest {

    @Test
    public void testLoad() throws Exception {
        Properties properties = PropertiesUtil.loadProperties(ClbDbTestRule.DEFAULT_PROPERTY_FILE);
        DbLoader.load(DbLoader.connect(properties), "squirrels", true);
    }
}