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
package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.checklistbank.utils.NameParsers;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan(
    basePackages = {
      "org.gbif.checklistbank.config",
      "org.gbif.checklistbank.service.mybatis.service",
      "org.gbif.checklistbank.service.mybatis.persistence"
    })
@MapperScan("org.gbif.checklistbank.service.mybatis.persistence.mapper")
public class SpringServiceConfig {
  private static final Logger LOG = LoggerFactory.getLogger(SpringServiceConfig.class);
  public static final String PARSER_TIMEOUT_PROP = "checklistbank.parser.timeout";

  // we dont really need this bean, but we want to configure the singleton instance timeout
  @Bean("nameParserTimeoutConfig")
  public Long nameParserConfig(@Value("${checklistbank.parser.timeout:20000}") long timeout) {
    LOG.info("Name parser timeout in spring context: {}", timeout);
    NameParsers.INSTANCE.setTimeout(timeout);
    return timeout;
  }
}
