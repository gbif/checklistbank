package org.gbif.checklistbank.config;

import org.gbif.checklistbank.utils.PropertiesUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
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

  public static final String PARSER_TIMEOUT_PROP = "checklistbank.parser.timeout";
  public static final String IMPORT_THREADS_PROP = "checklistbank.import.threads";
  private static final String WORK_MEM_PROP = "checklistbank.pg.workMem";

  @NotNull
  @Parameter(names = "--clb-url")
  public String databaseUrl;

  @NotNull
  @Parameter(names = "--clb-user")
  public String user;

  @NotNull
  @Parameter(names = "--clb-password", password = true)
  public String password;

  @Parameter(names = "--clb-maximum-pool-size")
  @Min(1)
  public int maximumPoolSize = 8;

  /**
   * TThe minimum number of idle connections that the pool tries to maintain.
   * If the idle connections dip below this value, the pool will make a best effort to add additional connections quickly and efficiently.
   * However, for maximum performance and responsiveness to spike demands, it is recommended to set this value not too low.
   * Beware that postgres statically allocates the work_mem for each session which can eat up memory a lot.
   */
  @Parameter(names = "--clb-minimum-idle")
  @Min(0)
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

  public Properties toProps(boolean withPrefix) {
    final String prefix = withPrefix ? PROPERTY_PREFIX : "";
    Properties props = new Properties();
    props.put(prefix + "dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
    if (withPrefix) {
      props.put(PARSER_TIMEOUT_PROP, String.valueOf(parserTimeout));
      props.put(IMPORT_THREADS_PROP, String.valueOf(syncThreads));
      props.put(WORK_MEM_PROP, String.valueOf(workMem));
    }
    if (workMem > 0) {
      props.put(prefix + CONNECTION_INIT_SQL_PROP, "SET work_mem='"+workMem+"MB'");
    }
    for (Field field : ClbConfiguration.class.getDeclaredFields()) {
      if (!field.isSynthetic() && Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
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

  public static ClbConfiguration fromProperties(Properties props) {
    ClbConfiguration cfg = new ClbConfiguration();
    cfg.parserTimeout = PropertiesUtils.getIntProp(props, PARSER_TIMEOUT_PROP, cfg.parserTimeout);
    cfg.syncThreads = PropertiesUtils.getIntProp(props, IMPORT_THREADS_PROP, cfg.syncThreads);
    cfg.workMem = PropertiesUtils.getIntProp(props, WORK_MEM_PROP, cfg.workMem);

    for (Field field : ClbConfiguration.class.getDeclaredFields()) {
      if (!field.isSynthetic() && Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
        try {
          if (!IGNORE.contains(field.getName())) {
            String prefix;
            if (DATASOURCE_SET.contains(field.getName())) {
              prefix = PROPERTY_PREFIX+"dataSource.";
            } else {
              prefix = PROPERTY_PREFIX;
            }
            Class<?> clazz = field.getType();
            if (int.class == clazz) {
              field.setInt(cfg, Integer.parseInt(props.getProperty(prefix + field.getName(), String.valueOf(field.get(cfg)))));
            } else {
              field.set(cfg, props.getProperty(prefix + field.getName(), String.valueOf(field.get(cfg))));
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

  /**
   * @return a new simple postgres jdbc connection
   */
  public Connection connect() throws SQLException {
    return DriverManager.getConnection(databaseUrl, user, password);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("databaseUrl", databaseUrl)
        .add("user", user)
        .add("password", password)
        .add("connectionTimeout", connectionTimeout)
        .add("maximumPoolSize", maximumPoolSize)
        .add("minimumIdle", minimumIdle)
        .add("idleTimeout", idleTimeout)
        .add("maxLifetime", maxLifetime)
        .add("workMem", workMem)
        .add("parserTimeout", parserTimeout)
        .add("syncThreads", syncThreads)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(databaseUrl, user, password, maximumPoolSize, minimumIdle, idleTimeout, maxLifetime, workMem, connectionTimeout, parserTimeout, syncThreads);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ClbConfiguration other = (ClbConfiguration) obj;
    return Objects.equals(this.databaseUrl, other.databaseUrl)
        && Objects.equals(this.user, other.user)
        && Objects.equals(this.password, other.password)
        && Objects.equals(this.maximumPoolSize, other.maximumPoolSize)
        && Objects.equals(this.minimumIdle, other.minimumIdle)
        && Objects.equals(this.idleTimeout, other.idleTimeout)
        && Objects.equals(this.maxLifetime, other.maxLifetime)
        && Objects.equals(this.workMem, other.workMem)
        && Objects.equals(this.connectionTimeout, other.connectionTimeout)
        && Objects.equals(this.parserTimeout, other.parserTimeout)
        && Objects.equals(this.syncThreads, other.syncThreads);
  }
}
