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
package org.gbif;

import org.gbif.checklistbank.BaseDBTest;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.ClbDbLoadTestDataBeforeAll;
import org.gbif.checklistbank.service.mybatis.service.SpringServiceConfig;
import org.gbif.ws.security.AppKeySigningService;
import org.gbif.ws.security.FileSystemKeyStore;
import org.gbif.ws.security.GbifAuthServiceImpl;
import org.gbif.ws.security.GbifAuthenticationManagerImpl;
import org.gbif.ws.server.filter.AppIdentityFilter;
import org.gbif.ws.server.filter.IdentityFilter;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ChecklistbankMyBatisServiceITBase.ChecklistBankServiceTestConfiguration.class)
@ActiveProfiles("test")
@ExtendWith(ClbDbLoadTestDataBeforeAll.class)
public class ChecklistbankMyBatisServiceITBase extends BaseDBTest {

  protected DataSource dataSource;

  public ChecklistbankMyBatisServiceITBase(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @TestConfiguration
  @PropertySource("classpath:application-test.yml")
  @Import(SpringServiceConfig.class) // actually not needed, it gets scanned by default
  @SpringBootApplication(exclude = {RabbitAutoConfiguration.class})
  @ComponentScan(
    excludeFilters = {
      @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
          AppKeySigningService.class,
          FileSystemKeyStore.class,
          IdentityFilter.class,
          AppIdentityFilter.class,
          GbifAuthenticationManagerImpl.class,
          GbifAuthServiceImpl.class
        })
    })
  public static class ChecklistBankServiceTestConfiguration {
    public static void main(String[] args) {
      SpringApplication.run(ChecklistBankServiceTestConfiguration.class, args);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("checklistbank.datasource.url", PG_CONTAINER::getJdbcUrl);
    registry.add("checklistbank.datasource.username", PG_CONTAINER::getUsername);
    registry.add("checklistbank.datasource.password", PG_CONTAINER::getPassword);
    registry.add("checklistbank.datasource.hikari.maximumPoolSize", () -> "2");
    registry.add("checklistbank.datasource.hikari.minimumIdle", () -> "1");
    registry.add("checklistbank.datasource.hikari.idleTimeout", () -> "60000");
    registry.add("checklistbank.datasource.hikari.connectionTimeout", () -> "2000");
    registry.add("checklistbank.datasource.hikari.leakDetectionThreshold", () -> "10000");
    registry.add("checklistbank.datasource.hikari.connectionInitSql", () -> "SET work_mem='64MB'");
  }
}
