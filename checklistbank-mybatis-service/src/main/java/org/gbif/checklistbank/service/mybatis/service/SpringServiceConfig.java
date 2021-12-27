package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.nameparser.NameParserGbifV1;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(
    basePackages = {
      "org.gbif.checklistbank.service.mybatis.service",
      "org.gbif.checklistbank.service.mybatis.persistence"
    })
@MapperScan("org.gbif.checklistbank.service.mybatis.persistence.mapper")
public class SpringServiceConfig {

  @Bean
  public NameParser nameParser(@Value("${parserTimeout:500}") long parserTimeout) {
    return new NameParserGbifV1(parserTimeout);
  }
}
