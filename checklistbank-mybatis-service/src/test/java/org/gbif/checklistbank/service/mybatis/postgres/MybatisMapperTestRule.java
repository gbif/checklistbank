package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * Provides a JUnit {@link org.junit.rules.TestRule} to allow database driven integration tests for mybatis mappers
 * in ChecklistBank.
 */
public class MybatisMapperTestRule<T> extends DatabaseDrivenTestRule<T> {

  private static final String DEFAULT_PROPERTY_FILE = "checklistbank.properties";
  private static final String PREFIX = "checklistbank.db.";
  private final List<String> tables;

  /**
   * @param mapperClass   the class for the service we want to wire up and test
   */
  private MybatisMapperTestRule(Class<T> mapperClass, String ... tables) {
    super(new InternalChecklistBankServiceMyBatisModule(buildDefaultProperties()),
      InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME, mapperClass, null,
      Maps.<String, Object>newHashMap());
      this.tables = ImmutableList.copyOf(tables);
  }

  public static <T> MybatisMapperTestRule<T> empty(Class<T> mapperClass, String ... tables) {
      return new MybatisMapperTestRule<T>(mapperClass, tables);
  }

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

    @Override
    /**
     * Update sequence counters and truncate tables.
     */
    protected void runFinally() {
        try (Connection con = dataSource.getConnection()) {
            for (String table : tables) {
                try (Statement st = con.createStatement()) {
                    st.execute("TRUNCATE " + table + " CASCADE");
                }
                try (Statement st = con.createStatement()) {
                    st.execute("ALTER SEQUENCE " + table + "_id_seq RESTART 1");
                } catch (SQLException e) {
                    // ignore
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
