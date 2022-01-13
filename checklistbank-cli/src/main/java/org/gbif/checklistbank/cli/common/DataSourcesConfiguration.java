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
package org.gbif.checklistbank.cli.common;

import org.gbif.checklistbank.config.ClbConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@ConditionalOnBean(ClbConfiguration.class)
public class DataSourcesConfiguration {

  @Autowired
  private ClbConfiguration clbConfiguration;

  @Bean
  @Primary
  public DataSourceProperties buildDataSourceProperties() {
    DataSourceProperties dataSourceProperties = new DataSourceProperties();
    dataSourceProperties.setGenerateUniqueName(true);
    dataSourceProperties.setUsername(clbConfiguration.user);
    dataSourceProperties.setPassword(clbConfiguration.password);
    dataSourceProperties.setUrl(clbConfiguration.databaseUrl);
    dataSourceProperties.setInitializationMode(DataSourceInitializationMode.ALWAYS);
    return dataSourceProperties;
  }

  /** Builds a Hikari DataSource using a prefix to get the configuration settings, */
  @Bean
  @Primary
  public HikariDataSource buildDataSource() {
    DataSourceProperties dataSourceProperties = buildDataSourceProperties();

    HikariDataSource dataSource =
        dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    dataSource.setIdleTimeout(clbConfiguration.idleTimeout);
    dataSource.setMaximumPoolSize(clbConfiguration.maximumPoolSize);
    dataSource.setMinimumIdle(clbConfiguration.minimumIdle);
    dataSource.setLeakDetectionThreshold(clbConfiguration.leakDetectionThreshold);
    dataSource.setMaxLifetime(clbConfiguration.maxLifetime);
    dataSource.setConnectionInitSql(clbConfiguration.connectionInitSql);
    dataSource.setConnectionTimeout(clbConfiguration.connectionTimeout);

    return dataSource;
  }
}
