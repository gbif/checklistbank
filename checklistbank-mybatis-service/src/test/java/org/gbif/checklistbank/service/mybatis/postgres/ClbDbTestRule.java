package org.gbif.checklistbank.service.mybatis.postgres;


import org.gbif.api.model.Constants;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.dbunit.DefaultDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * A TestRule for Database driven Integration tests executing some dbunit file beforehand.
 */
public class ClbDbTestRule implements TestRule {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final Map<String, Object> DB_UNIT_CLB_PROPERTIES = new ImmutableMap.Builder<String, Object>()
            .put(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new ClbDataTypeFactory())
            .put("http://www.dbunit.org/features/caseSensitiveTableNames", true)
            .put(DatabaseConfig.PROPERTY_ESCAPE_PATTERN, "\"?\"").build();

    private static final String DEFAULT_PROPERTY_FILE = "checklistbank.properties";
    private static final String DEFAULT_DBUNIT_FILE = "squirrels-full.xml";
    private static final String PREFIX = "checklistbank.db.dataSource.";
    private static final List<String> TABLES = ImmutableList.of("name_usage", "name", "name_usage_metrics", "raw_usage", "citation");

    private final String dbUnitFileName;
    private final Map<String, Integer> sequenceCounters;
    private final Properties properties;

    private Connection connection;
    private DefaultDatabaseTester databaseTester;

    /**
     * Prepares an empty CLB db before any test is run, truncating tables and resetting sequence counters.
     */
    public static ClbDbTestRule empty() {
        return new ClbDbTestRule(null, ImmutableMap.<String, Integer>builder()
                .put("citation_id_seq", 1)
                .put("dataset_metrics_id_seq", 1)
                .put("description_id_seq", 1)
                .put("distribution_id_seq", 1)
                .put("identifier_id_seq", 1)
                .put("literature_id_seq", 1)
                .put("media_id_seq", 1)
                .put("name_usage_id_seq", Constants.NUB_MAXIMUM_KEY+1)
                .put("name_id_seq", 1)
                .put("species_info_id_seq", 1)
                .put("typification_id_seq", 1)
                .put("vernacular_name_id_seq", 1)
                .build());
    }

    /**
     * Prepares a squirrels test db before any test is run, adding data and adjusting sequence counters.
     */
    public static ClbDbTestRule squirrels() {
        return new ClbDbTestRule(DEFAULT_DBUNIT_FILE, ImmutableMap.<String, Integer>builder()
                .put("citation_id_seq", 32)
                .put("dataset_metrics_id_seq", 5)
                .put("description_id_seq", 28)
                .put("distribution_id_seq", 29)
                .put("identifier_id_seq", 106)
                .put("literature_id_seq", 23)
                .put("media_id_seq", 100021)
                .put("name_usage_id_seq", 110000000)
                .put("name_id_seq", 200000)
                .put("species_info_id_seq", 4)
                .put("typification_id_seq", 16)
                .put("vernacular_name_id_seq", 100011)
                .build());
    }

    /**
     * @param dbUnitFileName the optional unqualified filename within the dbUnit package to be used in setting up
     *                       the db
     * @param sequenceCounters
     */
    private ClbDbTestRule(@Nullable String dbUnitFileName, Map<String, Integer> sequenceCounters) {
        this.dbUnitFileName = dbUnitFileName;
        this.sequenceCounters = sequenceCounters;
        try {
            properties = PropertiesUtil.loadProperties(DEFAULT_PROPERTY_FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection connect() {
        try {
            String url = "jdbc:postgresql://" + properties.getProperty(PREFIX + "serverName") + "/" + properties.getProperty(PREFIX + "databaseName");
            return DriverManager.getConnection(url, properties.getProperty(PREFIX + "user"), properties.getProperty(PREFIX + "password"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try {
                    before();
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    /**
     * @return the existing, single db connection. Keep this open, it will be closed by this rule after the tests have run.
     */
    public Connection getConnection() {
        return connection;
    }

    private void before() throws Exception {
        SLF4JBridgeHandler.install();
        connection = connect();
        if (dbUnitFileName == null) {
            truncateDb();
        } else {
            runDbUnit();
        }
        updateSequences();
    }

    /**
     * Truncates all tables in the clb db
     */
    private void truncateDb() throws SQLException {
        log.debug("Truncating CLB db");
        for (String table : TABLES) {
            try (java.sql.Statement st = connection.createStatement()) {
                st.execute("TRUNCATE " + table + " CASCADE");
            }
        }
    }

    /**
     * Update postgres sequence counters.
     */
    private void updateSequences() {
        log.debug("Resetting clb sequences");
        try {
            for (Map.Entry<String, Integer> seq : sequenceCounters.entrySet()) {
                try (java.sql.Statement st = connection.createStatement()) {
                    st.execute("ALTER SEQUENCE " + seq.getKey() + " RESTART " + seq.getValue());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void runDbUnit() throws Exception {
        log.debug("Updating database with " + dbUnitFileName);
        // DbUnit
        IDatabaseConnection dbUnitConnection = new DatabaseConnection(connection);
        for (Map.Entry<String, Object> prop : DB_UNIT_CLB_PROPERTIES.entrySet()) {
            dbUnitConnection.getConfig().setProperty(prop.getKey(), prop.getValue());
        }
        databaseTester = new DefaultDatabaseTester(dbUnitConnection);

        FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
        builder.setColumnSensing(true);
        IDataSet dataSet = builder.build(Resources.getResource("dbunit" + File.separatorChar + dbUnitFileName));

        databaseTester.setDataSet(dataSet);
        databaseTester.onSetup();

        // dbunit seems to close the connection
        if (connection.isClosed()) {
            connection = connect();
        }
    }


    private void after() throws Exception {
        if (databaseTester != null) {
            databaseTester.onTearDown();
        }
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Extending the regular postgres data type factory with our custom clb enumerations.
     */
    public static class ClbDataTypeFactory extends PostgresqlDataTypeFactory {
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
