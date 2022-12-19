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

import org.gbif.checklistbank.BaseDBTest;
import org.gbif.checklistbank.service.mybatis.service.SpringServiceConfig;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PostgresITBase.ChecklistBankServiceTestConfiguration.class)
@ActiveProfiles("test")
public class PostgresITBase extends BaseDBTest {

  @RegisterExtension public ClbLoadTestDb sbSetup;

  public PostgresITBase() {
    this.sbSetup = ClbLoadTestDb.empty(createConnectionSupplier());
  }

  public PostgresITBase(ClbLoadTestDb sbSetup) {
    this.sbSetup = sbSetup;
  }

  @TestConfiguration
  @PropertySource("classpath:application-test.yml")
  @Import(SpringServiceConfig.class) // actually not needed, it gets scanned by default
  @SpringBootApplication(exclude = {RabbitAutoConfiguration.class})
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
  }
}
