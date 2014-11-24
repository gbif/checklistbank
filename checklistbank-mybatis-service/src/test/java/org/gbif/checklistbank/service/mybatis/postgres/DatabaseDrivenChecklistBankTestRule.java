package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

  private final Map<String, Integer> sequenceCounters;

  private static Properties buildDefaultProperties() {
    try {
      return PropertiesUtil.loadProperties(DEFAULT_PROPERTY_FILE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  /**
   * Update sequence counters.
   */
  protected void runFinally() {
    try (Connection con = dataSource.getConnection()) {
      for (Map.Entry<String, Integer> seq : sequenceCounters.entrySet()) {
        try (Statement st = con.createStatement()) {
          st.execute("SELECT setval('" + seq.getKey() + "', " + seq.getValue() + ");");
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param serviceClass   the class for the service we want to wire up and test
   * @param properties     the verbatim configuration properties
   * @param sequenceCounters map of sequence names to their counter state to be changed after dbunit ran
   */
  public DatabaseDrivenChecklistBankTestRule(Properties properties, Class<T> serviceClass, Map<String, Integer> sequenceCounters) {
    super(new ChecklistBankServiceMyBatisModule(properties),
          InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME,
          serviceClass,
          DEFAULT_DBUNIT_FILE,
          DB_UNIT_CLB_PROPERTIES);
    this.sequenceCounters = sequenceCounters;
  }

  /**
   * Creates a db driven test rule using the default checklistbank.properties and the squirrels dbunit file.
   */
  public DatabaseDrivenChecklistBankTestRule(Class<T> serviceClass) {
    this(buildDefaultProperties(), serviceClass, ImmutableMap.<String, Integer>builder()
      .put("citation_id_seq", 32)
      .put("dataset_metrics_id_seq", 5)
      .put("description_id_seq", 28)
      .put("distribution_id_seq", 29)
      .put("identifier_id_seq", 106)
      .put("literature_id_seq", 23)
      .put("media_id_seq", 100021)
      .put("name_usage_id_seq", 110000000)
      .put("name_id_seq", 200000)
      .put("species_info_id_seq", 3)
      .put("typification_id_seq", 16)
      .put("vernacular_name_id_seq", 100011)
      .build());
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
