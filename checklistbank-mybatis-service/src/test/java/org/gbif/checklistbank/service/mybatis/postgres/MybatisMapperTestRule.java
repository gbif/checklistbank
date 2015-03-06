package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.Properties;

import com.google.common.collect.Maps;

/**
 * Provides a JUnit {@link org.junit.rules.TestRule} to allow database driven integration tests for mybatis mappers
 * in ChecklistBank.
 */
public class MybatisMapperTestRule<T> extends DatabaseDrivenTestRule<T> {

  private static final String DEFAULT_PROPERTY_FILE = "checklistbank.properties";
  private static final String PREFIX = "checklistbank.db.";

  private static Properties buildDefaultProperties() {
    try {
      Properties properties = PropertiesUtil.loadProperties(DEFAULT_PROPERTY_FILE);
      for(String key : properties.stringPropertyNames()) {
        if (key.startsWith(PREFIX)) {
          properties.setProperty(key.substring(PREFIX.length()), properties.getProperty(key));
          properties.remove(key);
        }
      }
      return properties;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param mapperClass   the class for the service we want to wire up and test
   */
  public MybatisMapperTestRule(Class<T> mapperClass) {
    super(new InternalChecklistBankServiceMyBatisModule(buildDefaultProperties()),
      InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME, mapperClass, null,
      Maps.<String, Object>newHashMap());
  }
}
