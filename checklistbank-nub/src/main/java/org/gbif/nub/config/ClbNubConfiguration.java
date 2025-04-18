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
package org.gbif.nub.config;

import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.service.SpringServiceConfig;
import org.gbif.checklistbank.utils.PropertiesUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import lombok.Data;

/**
 * A configuration for the checklist bank database connection pool as used by the mybatis layer.
 * Knows how to insert a service guice module.
 */
@SuppressWarnings("PublicField")
@Data
@Component
public class ClbNubConfiguration {

  private static final String PROPERTY_PREFIX = "checklistbank.db.";
  private static final Set<String> DATASOURCE_SET =
      Sets.newHashSet("serverName", "databaseName", "user", "password");
  private static final Set<String> IGNORE =
      Sets.newHashSet("parserTimeout", "syncThreads", "workMem");
  private static final String CONNECTION_INIT_SQL_PROP = "connectionInitSql";

  public static final String IMPORT_THREADS_PROP = "checklistbank.import.threads";
  private static final String WORK_MEM_PROP = "checklistbank.pg.workMem";

  @NotNull
  @Value("${checklistbank.datasource.url}")
  public String databaseUrl;

  @NotNull
  @Value("${checklistbank.datasource.username}")
  public String user;

  @NotNull
  @Value("${checklistbank.datasource.password}")
  public String password;

  @Value("${checklistbank.datasource.hikari.maximumPoolSize:8}")
  public int maximumPoolSize = 8;

  /**
   * TThe minimum number of idle connections that the pool tries to maintain. If the idle
   * connections dip below this value, the pool will make a best effort to add additional
   * connections quickly and efficiently. However, for maximum performance and responsiveness to
   * spike demands, it is recommended to set this value not too low. Beware that postgres statically
   * allocates the work_mem for each session which can eat up memory a lot.
   */
  @Value("${checklistbank.datasource.hikari.minimumIdle:1}")
  public int minimumIdle = 1;

  /**
   * This property controls the maximum amount of time that a connection is allowed to sit idle in
   * the pool. A connection will never be retired as idle before this timeout. A value of 0 means
   * that idle connections are never removed from the pool.
   */
  @Value("${checklistbank.datasource.hikari.idleTimeout:60000}")
  public int idleTimeout = min(1);

  /**
   * This property controls the maximum lifetime of a connection in the pool. When a connection
   * reaches this timeout it will be retired from the pool. An in-use connection will never be
   * retired, only when it is closed will it then be removed. A value of 0 indicates no maximum
   * lifetime (infinite lifetime), subject of course to the idleTimeout setting.
   */
  @Value("${checklistbank.datasource.hikari.maxLifetime:900000}")
  public int maxLifetime = min(15);

  @Value("${checklistbank.datasource.hikari.leakDetectionThreshold:180000}")
  public int leakDetectionThreshold = min(3);

  /**
   * The postgres work_mem session setting in MB that should be used for each connection. A value of
   * zero or below does not set anything and thus uses the global postgres settings
   */
  @Value("${checklistbank.datasource.hikari.connectionInitSql:SET work_mem='64MB'}")
  public String connectionInitSql = "SET work_mem='64MB'";

  @Value("${checklistbank.datasource.hikari.connectionTimeout:5000}")
  public int connectionTimeout = sec(5);

  @Value("${checklistbank.parser.timeout:20000}")
  public int parserTimeout = sec(20);

  @Value("${checklistbank.import.threads:1}")
  public int syncThreads = 1;

  /** @return converted minutes in milliseconds */
  private static int min(int minutes) {
    return minutes * 60_000;
  }

  /** @return converted seconds in milliseconds */
  private static int sec(int seconds) {
    return seconds * 1_000;
  }

  public Properties toProps(boolean withPrefix) {
    final String prefix = withPrefix ? PROPERTY_PREFIX : "";
    Properties props = new Properties();
    props.put(prefix + "dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
    if (withPrefix) {
      props.put(SpringServiceConfig.PARSER_TIMEOUT_PROP, String.valueOf(parserTimeout));
      props.put(IMPORT_THREADS_PROP, String.valueOf(syncThreads));
      props.put(WORK_MEM_PROP, String.valueOf(connectionInitSql));
    }
    if (connectionInitSql != null) {
      props.put(prefix + CONNECTION_INIT_SQL_PROP, connectionInitSql);
    }
    for (Field field : ClbNubConfiguration.class.getDeclaredFields()) {
      if (!field.isSynthetic()
          && Modifier.isPublic(field.getModifiers())
          && !Modifier.isStatic(field.getModifiers())) {
        try {
          if (IGNORE.contains(field.getName())) {
            // ignore
          } else if (DATASOURCE_SET.contains(field.getName())) {
            props.put(prefix + "dataSource." + field.getName(), String.valueOf(field.get(this)));
          } else {
            props.put(prefix + field.getName(), String.valueOf(field.get(this)));
          }
        } catch (IllegalAccessException e) {
          // cant happen, we check for public access
          throw new RuntimeException(e);
        }
      }
    }
    return props;
  }

  public static ClbNubConfiguration fromProperties(Properties props) {
    ClbNubConfiguration cfg = new ClbNubConfiguration();
    cfg.parserTimeout = PropertiesUtils.getIntProp(props, SpringServiceConfig.PARSER_TIMEOUT_PROP, cfg.parserTimeout);
    cfg.syncThreads = PropertiesUtils.getIntProp(props, IMPORT_THREADS_PROP, cfg.syncThreads);
    cfg.connectionInitSql = props.getProperty(WORK_MEM_PROP, cfg.connectionInitSql);

    for (Field field : ClbNubConfiguration.class.getDeclaredFields()) {
      if (!field.isSynthetic()
          && Modifier.isPublic(field.getModifiers())
          && !Modifier.isStatic(field.getModifiers())) {
        try {
          if (!IGNORE.contains(field.getName())) {
            String prefix;
            if (DATASOURCE_SET.contains(field.getName())) {
              prefix = PROPERTY_PREFIX + "dataSource.";
            } else {
              prefix = PROPERTY_PREFIX;
            }
            Class<?> clazz = field.getType();
            if (int.class == clazz) {
              field.setInt(
                  cfg,
                  Integer.parseInt(
                      props.getProperty(prefix + field.getName(), String.valueOf(field.get(cfg)))));
            } else {
              field.set(
                  cfg, props.getProperty(prefix + field.getName(), String.valueOf(field.get(cfg))));
            }
          }

        } catch (IllegalAccessException e) {
          // cant happen, we check for public access
          throw new RuntimeException(e);
        }
      }
    }
    return cfg;
  }

  /** @return a new simple postgres jdbc connection */
  public Connection connect() throws SQLException {
    return DriverManager.getConnection(databaseUrl, user, password);
  }

  public static ClbNubConfiguration fromClbConfiguration(ClbConfiguration clbConfiguration) {
    ClbNubConfiguration cfg = new ClbNubConfiguration();
    cfg.databaseUrl = clbConfiguration.getDbUrl();
    cfg.user = clbConfiguration.getUser();
    cfg.password = clbConfiguration.getPassword();
    cfg.maximumPoolSize = clbConfiguration.getMaximumPoolSize();
    cfg.minimumIdle = clbConfiguration.getMinimumIdle();
    cfg.idleTimeout = clbConfiguration.getIdleTimeout();
    cfg.maxLifetime = clbConfiguration.getMaxLifetime();
    cfg.leakDetectionThreshold = clbConfiguration.getLeakDetectionThreshold();
    cfg.connectionInitSql = clbConfiguration.getConnectionInitSql();
    cfg.connectionTimeout = clbConfiguration.getConnectionTimeout();
    cfg.parserTimeout = clbConfiguration.getParserTimeout();
    cfg.syncThreads = clbConfiguration.getSyncThreads();

    return cfg;
  }

}
