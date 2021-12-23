package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.nameparser.NameParserGbifV1;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.gbif.vocabulary.service")
@MapperScan("org.gbif.vocabulary.persistence.mappers")
public class SpringServiceConfig {

  @Bean
  public NameParser nameParser() {
    // TODO: parserTimeout should come from properties
    long parserTimeout = 100;
    return new NameParserGbifV1(parserTimeout);
  }

}
