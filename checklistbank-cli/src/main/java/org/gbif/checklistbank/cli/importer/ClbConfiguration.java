package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Properties;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A configuration for the checklist bank database connection pool
 * as used by the mybatis layer. Knows how to insert a service guice module.
 */
@SuppressWarnings("PublicField")
public class ClbConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(ClbConfiguration.class);
  private static final String PROPERTY_PREFIX = "checklistbank.db.JDBC.";
  private static final Map<String, String> PROPERTY_MAP = ImmutableMap.<String, String>builder()
    .put("partitions", "checklistbank.db.bonecp.partitionCount")
    .put("connections", "checklistbank.db.bonecp.maxConnectionsPerPartition")
    .put("maxAge", "checklistbank.db.bonecp.maxConnectionAgeInSeconds")
    .build();

  @NotNull
  @Parameter(names = "--clb-url")
  public String url;

  @NotNull
  @Parameter(names = "--clb-username")
  public String username;

  @NotNull
  @Parameter(names = "--clb-password", password = true)
  public String password;

  @Parameter(names = "--clb-partitions")
  public int partitions = 1;

  @Parameter(names = "--clb-connections")
  public int connections = 10;

  @Parameter(names = "--clb-maxAge")
  public int maxAge = 120;

  public ChecklistBankServiceMyBatisModule createServiceModule() {
    Properties props = new Properties();
    props.put(PROPERTY_PREFIX + "driver", "org.postgresql.Driver");
    for (Field field : ClbConfiguration.class.getDeclaredFields()) {
      if (!field.isSynthetic() && Modifier.isPublic(field.getModifiers())) {
        try {
          if (PROPERTY_MAP.containsKey(field.getName())) {
            props.put(PROPERTY_MAP.get(field.getName()), String.valueOf(field.get(this)));
          } else {
            props.put(PROPERTY_PREFIX + field.getName(), String.valueOf(field.get(this)));
          }
        } catch (IllegalAccessException e) {
          // cant happen, we check for public access
          throw new RuntimeException(e);
        }
      }
    }
    return new ChecklistBankServiceMyBatisModule(props);
  }

}
