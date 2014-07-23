package org.gbif.checklistbank.cli.importer;

import com.beust.jcommander.Parameter;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;

/**
 * A configuration for the checklist bank database connection pool
 * as used by the mybatis layer. Knows how to create a service guice module.
 */
public class ClbConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(ClbConfiguration.class);
    private static final String PROPERTY_PREFIX = "checklistbank.db.";

    @NotNull
    @Parameter(names = "--clb-url")
    public String url;

    @NotNull
    @Parameter(names = "--clb-username")
    public String username;

    @NotNull
    @Parameter(names = "--clb-password")
    public String password;

    @Parameter(names = "--clb-partitions")
    public int partitions = 1;

    @Parameter(names = "--clb-connections")
    public int connections = 10;

    public ChecklistBankServiceMyBatisModule createServiceModule() {
        Properties props = new Properties();
        for (Field field : ClbConfiguration.class.getDeclaredFields()) {
            if (!field.isSynthetic() && Modifier.isPublic(field.getModifiers())) {
                try {
                    props.put(PROPERTY_PREFIX + field.getName(), String.valueOf(field.get(this)));
                } catch (IllegalAccessException e) {
                    // cant happen, we check for public access
                    throw new RuntimeException(e);
                }
            }
        }
        return new ChecklistBankServiceMyBatisModule(props);
    }

}
