package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.test.DatabaseDrivenTestRule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.sql.Types;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;

/**
 * Provides a JUnit {@link org.junit.rules.TestRule} to allow database driven integration tests in ChecklistBank.
 * You need to add this rule to each of your tests where you want to use it. One example on the usage:
 * <pre>
 * {@code
 *
 * @param <T> the class of the service you need for this test. This is initialized using Guice and backed by a database
 *            as specified in the properties provided.
 */
public class DatabaseDrivenChecklistBankTestRule<T> extends DatabaseDrivenTestRule<T> {

  private static final String DEFAULT_PROPERTY_FILE = "checklistbank.properties";
  private static final String DEFAULT_DBUNIT_FILE = "squirrels-full.xml";

  public static final Map<String, Object> DB_UNIT_CLB_PROPERTIES = new ImmutableMap.Builder<String, Object>()
    .put(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new ClbDataTypeFactory())
    .put("http://www.dbunit.org/features/caseSensitiveTableNames", true)
    .put(DatabaseConfig.PROPERTY_ESCAPE_PATTERN, "\"?\"").build();

  private static Properties buildDefaultProperties() {
    try {
      return PropertiesUtil.loadProperties(DEFAULT_PROPERTY_FILE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param serviceClass   the class for the service we want to wire up and test
   * @param properties     the verbatim configuration properties
   * @param dbUnitFileName the optional unqualified filename within the dbunit package to be used in setting up the db
   */
  public DatabaseDrivenChecklistBankTestRule(Properties properties, Class<T> serviceClass, String dbUnitFileName) {
    super(new ChecklistBankServiceMyBatisModule(properties),
      InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME, serviceClass, dbUnitFileName,
      DB_UNIT_CLB_PROPERTIES);
  }

  public DatabaseDrivenChecklistBankTestRule(Class<T> serviceClass) {
    this(buildDefaultProperties(), serviceClass, DEFAULT_DBUNIT_FILE);
  }

  /**
   * Creates a db driven test rule using the default checklistbank.properties and the squirrels dbunit file.
   */
  public DatabaseDrivenChecklistBankTestRule() {
    this(null);
  }

  /**
   * Extending the regular postgres data type factory with our custom clb enumerations.
   */
  public static class ClbDataTypeFactory extends PostgresqlDataTypeFactory{
    private static Set<String> ENUM_NAMES = ImmutableSet.of("cites_appendix", "establishment_means", "identifier_type",
      "kingdom", "life_stage", "name_part", "name_type", "nomenclatural_status", "occurrence_status", "origin_type",
      "rank", "sex", "taxonomic_status", "threat_status", "type_designation_type", "type_status");

    @Override
    public boolean isEnumType(String sqlTypeName) {
      if (ENUM_NAMES.contains(sqlTypeName)) {
        return true;
      }
      return false;
    }

    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
      if (sqlType == Types.OTHER && "hstore".equals(sqlTypeName)) {
        return new HstoreType();
      }
      if (sqlType == Types.ARRAY) {
        return new ArrayType(sqlTypeName.substring(1));
      }
      return super.createDataType(sqlType, sqlTypeName);
    }
  }



}
