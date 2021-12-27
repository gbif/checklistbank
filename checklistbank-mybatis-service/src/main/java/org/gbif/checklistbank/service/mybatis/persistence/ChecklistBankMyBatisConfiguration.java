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
package org.gbif.checklistbank.service.mybatis.persistence;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.v2.RankedName;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.checklistbank.model.DatasetCore;
import org.gbif.checklistbank.model.IucnRedListCategory;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.model.ParsedNameUsage;
import org.gbif.checklistbank.model.RawUsage;
import org.gbif.checklistbank.model.TocEntry;
import org.gbif.checklistbank.model.Usage;
import org.gbif.checklistbank.model.UsageCount;
import org.gbif.checklistbank.model.UsageRelated;
import org.gbif.checklistbank.service.mybatis.service.DatasetMetricsServiceMyBatis;
import org.gbif.mybatis.type.CountryTypeHandler;
import org.gbif.mybatis.type.LanguageTypeHandler;
import org.gbif.mybatis.type.UriTypeHandler;
import org.gbif.mybatis.type.UuidTypeHandler;

import java.net.URI;
import java.util.UUID;

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Checklistbank MyBatis Spring configuration.
 */
@Configuration
public class ChecklistBankMyBatisConfiguration {

  @Bean
  ConfigurationCustomizer mybatisConfigCustomizer() {
    return configuration -> {
      configuration.setMapUnderscoreToCamelCase(true);

      //Aliases
      configuration.getTypeAliasRegistry().registerAlias("Count", DatasetMetricsServiceMyBatis.Count.class);
      configuration.getTypeAliasRegistry().registerAlias("DatasetCore", DatasetCore.class);
      configuration.getTypeAliasRegistry().registerAlias("DatasetMetrics", DatasetMetrics.class);
      configuration.getTypeAliasRegistry().registerAlias("Description", Description.class);
      configuration.getTypeAliasRegistry().registerAlias("Distribution", Distribution.class);
      configuration.getTypeAliasRegistry().registerAlias("Identifier", Identifier.class);
      configuration.getTypeAliasRegistry().registerAlias("NameUsage", NameUsage.class);
      configuration.getTypeAliasRegistry().registerAlias("ParsedNameUsage", ParsedNameUsage.class);
      configuration.getTypeAliasRegistry().registerAlias("RankedName", RankedName.class);
      configuration.getTypeAliasRegistry().registerAlias("NameUsageContainer", NameUsageContainer.class);
      configuration.getTypeAliasRegistry().registerAlias("NameUsageMediaObject", NameUsageMediaObject.class);
      configuration.getTypeAliasRegistry().registerAlias("NameUsageMetrics", NameUsageMetrics.class);
      configuration.getTypeAliasRegistry().registerAlias("NameUsageWritable", NameUsageWritable.class);
      configuration.getTypeAliasRegistry().registerAlias("ParsedName", ParsedName.class);
      configuration.getTypeAliasRegistry().registerAlias("RawUsage", RawUsage.class);
      configuration.getTypeAliasRegistry().registerAlias("Reference", Reference.class);
      configuration.getTypeAliasRegistry().registerAlias("SpeciesProfile", SpeciesProfile.class);
      configuration.getTypeAliasRegistry().registerAlias("TocEntry", TocEntry.class);
      configuration.getTypeAliasRegistry().registerAlias("TypeSpecimen", TypeSpecimen.class);
      configuration.getTypeAliasRegistry().registerAlias("Usage", Usage.class);
      configuration.getTypeAliasRegistry().registerAlias("UsageCount", UsageCount.class);
      configuration.getTypeAliasRegistry().registerAlias("UsageRelated", UsageRelated.class);
      configuration.getTypeAliasRegistry().registerAlias("VerbatimNameUsage", VerbatimNameUsage.class);
      configuration.getTypeAliasRegistry().registerAlias("VernacularName", VernacularName.class);
      configuration.getTypeAliasRegistry().registerAlias("IucnRedListCategory", IucnRedListCategory.class);

      //TypeHandlers
      // TODO: remove the registry package??
      configuration.getTypeHandlerRegistry().register("org.gbif.registry.persistence.handler");
      configuration.getTypeHandlerRegistry().register(Country.class, CountryTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(Language.class, LanguageTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(URI.class, UriTypeHandler.class);
    };
  }

  @Bean
  @Primary
  @ConfigurationProperties("checklistbank.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  @ConfigurationProperties("checklistbank.datasource.hikari")
  public HikariDataSource dataSource() {
    return dataSourceProperties()
      .initializeDataSourceBuilder()
      .type(HikariDataSource.class)
      .build();
  }
}
