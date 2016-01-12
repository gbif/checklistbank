package org.gbif.checklistbank.cli.common;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Sets;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A configuration for the checklist bank database connection pool
 * as used by the mybatis layer. Knows how to insert a service guice module.
 */
@SuppressWarnings("PublicField")
public class ClbConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(ClbConfiguration.class);
  private static final String PROPERTY_PREFIX = "checklistbank.db.";
  private static final Set<String> DATASOURCE_SET = Sets.newHashSet("serverName", "databaseName", "user", "password");
  private static final Set<String> IGNORE = Sets.newHashSet("parserTimeout", "syncThreads", "workMem");
  private static final String CONNECTION_INIT_SQL_PROP = "connectionInitSql";

  @NotNull
  @Parameter(names = "--clb-host")
  public String serverName = "localhost";

  @NotNull
  @Parameter(names = "--clb-db")
  public String databaseName;

  @NotNull
  @Parameter(names = "--clb-user")
  public String user;

  @NotNull
  @Parameter(names = "--clb-password", password = true)
  public String password;

  @Parameter(names = "--clb-maximum-pool-size")
  @Min(3)
  public int maximumPoolSize = 8;

  /**
   * TThe minimum number of idle connections that the pool tries to maintain.
   * If the idle connections dip below this value, the pool will make a best effort to add additional connections quickly and efficiently.
   * However, for maximum performance and responsiveness to spike demands, it is recommended to set this value not too low.
   * Beware that postgres statically allocates the work_mem for each session which can eat up memory a lot.
   */
  @Parameter(names = "--clb-minimum-idle")
  @Min(3)
  public int minimumIdle = 1;

  /**
   * This property controls the maximum amount of time that a connection is allowed to sit idle in the pool.
   * A connection will never be retired as idle before this timeout.
   * A value of 0 means that idle connections are never removed from the pool.
   */
  @Parameter(names = "--clb-idle-timeout")
  @Min(0)
  public int idleTimeout = min(1);

  /**
   * This property controls the maximum lifetime of a connection in the pool.
   * When a connection reaches this timeout it will be retired from the pool.
   * An in-use connection will never be retired, only when it is closed will it then be removed.
   * A value of 0 indicates no maximum lifetime (infinite lifetime), subject of course to the idleTimeout setting.
   */
  @Parameter(names = "--clb-max-lifetime")
  @Min(0)
  public int maxLifetime = min(15);

  /**
   * The postgres work_mem session setting in MB that should be used for each connection.
   * A value of zero or below does not set anything and thus uses the global postgres settings
   */
  @Parameter(names = "--clb-work-mem")
  public int workMem = 0;

  @Parameter(names = "--clb-connection-timeout")
  @Min(1000)
  public int connectionTimeout = sec(5);

  @Parameter(names = "--parser-timeout")
  @Min(100)
  public int parserTimeout = sec(1);

  @Parameter(names = "--sync-threads")
  @Min(0)
  public int syncThreads = 1;

  /**
   * @return converted minutes in milliseconds
   */
  private static int min(int minutes) {
    return minutes*60000;
  }

  /**
   * @return converted seconds in milliseconds
   */
  private static int sec(int seconds) {
    return seconds*1000;
  }

  private Properties toProps(boolean withPrefix) {
    final String prefix = withPrefix ? PROPERTY_PREFIX : "";
    Properties props = new Properties();
    props.put(prefix + "dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
    if (withPrefix) {
      props.put(ChecklistBankServiceMyBatisModule.PARSER_TIMEOUT_PROP, String.valueOf(parserTimeout));
      props.put(ChecklistBankServiceMyBatisModule.IMPORT_THREADS_PROP, String.valueOf(syncThreads));
    }
    if (workMem > 0) {
      props.put(prefix + CONNECTION_INIT_SQL_PROP, "SET work_mem='"+workMem+"MB'");
    }
    for (Field field : ClbConfiguration.class.getDeclaredFields()) {
      if (!field.isSynthetic() && Modifier.isPublic(field.getModifiers())) {
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

  public InternalChecklistBankServiceMyBatisModule createMapperModule() {
    LOG.info("Connecting to checklistbank db {} on {} with user {}", databaseName, serverName, user);
    return new InternalChecklistBankServiceMyBatisModule(toProps(false), parserTimeout, syncThreads);
  }

  public ChecklistBankServiceMyBatisModule createServiceModule() {
    LOG.info("Connecting to checklistbank db {} on {} with user {}", databaseName, serverName, user);
    return new ChecklistBankServiceMyBatisModule(toProps(true));
  }

  /**
   * @return a new simple postgres jdbc connection
   */
  public BaseConnection connect() throws SQLException {
    String url = "jdbc:postgresql://" + serverName + "/" + databaseName;
    return (BaseConnection) DriverManager.getConnection(url, user, password);
  }

}
