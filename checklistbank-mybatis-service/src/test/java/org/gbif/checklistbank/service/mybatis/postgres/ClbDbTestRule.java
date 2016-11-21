package org.gbif.checklistbank.service.mybatis.postgres;


import org.gbif.api.model.Constants;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * A TestRule for Database driven Integration tests executing some dbSetup file beforehand.
 */
public class ClbDbTestRule implements TestRule {

  public static final String DEFAULT_PROPERTY_FILE = "checklistbank.properties";

  protected final Logger log = LoggerFactory.getLogger(getClass());
  private final String tsvFolder;
  private final Map<String, Integer> sequenceCounters;
  private final Properties properties;
  private Connection connection;

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
        .put("name_usage_id_seq", Constants.NUB_MAXIMUM_KEY + 1)
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
    return new ClbDbTestRule("squirrels", ImmutableMap.<String, Integer>builder()
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
   * Prepares a squirrels test db before any test is run, adding data and adjusting sequence counters.
   */
  public static ClbDbTestRule puma() {
    return new ClbDbTestRule("puma", ImmutableMap.<String, Integer>builder()
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
   * @param tsvFolder the optional unqualified filename within the dbUnit package to be used in setting up
   *                  the db
   */
  private ClbDbTestRule(@Nullable String tsvFolder, Map<String, Integer> sequenceCounters) {
    this.tsvFolder = tsvFolder;
    this.sequenceCounters = sequenceCounters;
    try {
      properties = PropertiesUtil.loadProperties(DEFAULT_PROPERTY_FILE);
    } catch (IOException e) {
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

  public void before() throws Exception {
    SLF4JBridgeHandler.install();
    connection = DbLoader.connect(properties);
    connection.setAutoCommit(false);
    if (tsvFolder != null) {
      DbLoader.load(connection, tsvFolder, true);
    } else {
      DbLoader.truncate(connection, "squirrels");
    }
    updateSequences();
    connection.setAutoCommit(true);
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
      connection.commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  public void after() throws Exception {
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
  }

}
