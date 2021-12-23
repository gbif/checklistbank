package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import org.gbif.checklistbank.service.mybatis.persistence.ChecklistBankMyBatisConfiguration;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Simply checks no exceptions occurr when loading the standard squirrelts dataset.
 */
public class DbLoaderTest {

    private final AnnotationConfigApplicationContext
      ctx = new AnnotationConfigApplicationContext(ChecklistBankMyBatisConfiguration.class);

    @Test
    public void testLoad() throws Exception {
        Properties properties = PropertiesUtil.loadProperties(ClbDbTestRule.DEFAULT_PROPERTY_FILE);
        DbLoader.load(ctx.getBean(DataSource.class).getConnection(), "squirrels", true);
    }
}