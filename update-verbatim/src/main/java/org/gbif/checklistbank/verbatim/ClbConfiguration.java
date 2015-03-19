package org.gbif.checklistbank.verbatim;

import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;
import java.util.Set;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
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
  private static final String PROPERTY_PREFIX = "";
  private static final Set<String> DATASOURCE_SET = Sets.newHashSet("serverName","databaseName","user","password");

  @NotNull
  @Parameter(names = "--clb-host")
  public String serverName;

  @NotNull
  @Parameter(names = "--clb-db")
  public String databaseName;

  @NotNull
  @Parameter(names = "--clb-user")
  public String user;

  @NotNull
  @Parameter(names = "--clb-password", password = true)
  public String password;

  @Parameter(names = "--clb-maximumPoolSize")
  public int maximumPoolSize = 4;

  @Parameter(names = "--clb-connectionTimeout")
  public int connectionTimeout = 5000;

  public InternalChecklistBankServiceMyBatisModule createServiceModule() {
    Properties props = new Properties();
    props.put(PROPERTY_PREFIX + "dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
    for (Field field : ClbConfiguration.class.getDeclaredFields()) {
      if (!field.isSynthetic() && Modifier.isPublic(field.getModifiers())) {
        try {
          if (DATASOURCE_SET.contains(field.getName())) {
            props.put(PROPERTY_PREFIX + "dataSource." + field.getName(), String.valueOf(field.get(this)));
          } else {
            props.put(PROPERTY_PREFIX + field.getName(), String.valueOf(field.get(this)));
          }
        } catch (IllegalAccessException e) {
          // cant happen, we check for public access
          throw new RuntimeException(e);
        }
      }
    }
    LOG.info("Connecting to checklistbank db {} on {} with user {}", databaseName, serverName, user);
    return new InternalChecklistBankServiceMyBatisModule(props);
  }

}
