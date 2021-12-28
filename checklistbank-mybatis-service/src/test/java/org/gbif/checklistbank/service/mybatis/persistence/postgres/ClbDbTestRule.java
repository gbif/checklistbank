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
package org.gbif.checklistbank.service.mybatis.persistence.postgres;


import org.gbif.api.model.Constants;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.common.collect.ImmutableMap;

/**
 * A TestRule for Database driven Integration tests executing some dbSetup file beforehand.
 */
public class ClbDbTestRule implements BeforeEachCallback {

  public static final UUID SQUIRRELS_DATASET_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");

  protected final Logger log = LoggerFactory.getLogger(getClass());
  private final String tsvFolder;
  private final Map<String, Integer> sequenceCounters;
  private final DataSource dataSource;

  /**
   * Prepares an empty CLB db before any test is run, truncating tables and resetting sequence counters.
   */
  public static ClbDbTestRule empty(DataSource dataSource) {
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
        .build(), dataSource);
  }

  /**
   * Prepares a squirrels test db before any test is run, adding data and adjusting sequence counters.
   */
  public static ClbDbTestRule squirrels(DataSource dataSource) {
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
        .build(), dataSource);
  }

  /**
   * Prepares a squirrels test db before any test is run, adding data and adjusting sequence counters.
   */
  public static ClbDbTestRule puma(DataSource dataSource) {
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
        .build(), dataSource);
  }

  /**
   * @param tsvFolder the optional unqualified filename within the dbUnit package to be used in setting up
   *                  the db
   */
  private ClbDbTestRule(@Nullable String tsvFolder, Map<String, Integer> sequenceCounters, DataSource dataSource) {
    this.dataSource = dataSource;
    this.tsvFolder = tsvFolder;
    this.sequenceCounters = sequenceCounters;
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    before();
  }

  public void before() throws Exception {
    SLF4JBridgeHandler.install();
    try(Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      if (tsvFolder != null) {
        DbLoader.load(connection, tsvFolder, true);
      } else {
        DbLoader.truncate(connection, "squirrels");
      }
      updateSequences(connection);
      connection.setAutoCommit(true);
    }
  }

  /**
   * Update postgres sequence counters.
   */
  private void updateSequences(Connection connection) {
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

}
