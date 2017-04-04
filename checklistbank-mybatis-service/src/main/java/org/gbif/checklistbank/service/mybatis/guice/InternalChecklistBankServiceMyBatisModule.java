package org.gbif.checklistbank.service.mybatis.guice;

import com.google.common.base.Preconditions;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.gbif.api.model.checklistbank.*;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.service.checklistbank.*;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.*;
import org.gbif.checklistbank.service.*;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.*;
import org.gbif.checklistbank.service.mybatis.mapper.*;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.mybatis.type.CountryTypeHandler;
import org.gbif.mybatis.type.LanguageTypeHandler;
import org.gbif.mybatis.type.UriTypeHandler;
import org.gbif.mybatis.type.UuidTypeHandler;
import org.gbif.nameparser.GBIFNameParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.net.URI;
import java.util.Properties;
import java.util.UUID;

/**
 * This Module should not be used, use the
 * {@link org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule} instead.
 */
public class InternalChecklistBankServiceMyBatisModule extends MyBatisModule {
  private static final Logger LOG = LoggerFactory.getLogger(InternalChecklistBankServiceMyBatisModule.class);

  private static final String DATASOURCE_BINDING_NAME = "checklistbank";
  public static final Key<DataSource> DATASOURCE_KEY = Key.get(DataSource.class,
      Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME));

  private final int parserTimeout;
  private final int importThreads;

  public InternalChecklistBankServiceMyBatisModule(Properties props, int parserTimeout, int importThreads) {
    super(DATASOURCE_BINDING_NAME, props);
    Preconditions.checkArgument(parserTimeout >= 50, "Name parser timeout must be at least 50ms");
    this.parserTimeout = parserTimeout;
    Preconditions.checkArgument(importThreads >= 0, "Number of import threads need to be positive");
    this.importThreads = importThreads;
  }

  public static InternalChecklistBankServiceMyBatisModule create(ClbConfiguration cfg) {
    LOG.info("Connecting to checklistbank db {} on {} with user {}", cfg.databaseName, cfg.serverName, cfg.user);
    return new InternalChecklistBankServiceMyBatisModule(cfg.toProps(false), cfg.parserTimeout, cfg.syncThreads);
  }

  @Override
  protected void bindMappers() {

    // mybatis config
    addAlias("Count").to(DatasetMetricsServiceMyBatis.Count.class);
    addAlias("DatasetCore").to(DatasetCore.class);
    addAlias("DatasetMetrics").to(DatasetMetrics.class);
    addAlias("Description").to(Description.class);
    addAlias("Distribution").to(Distribution.class);
    addAlias("Identifier").to(Identifier.class);
    addAlias("NameUsage").to(NameUsage.class);
    addAlias("ParsedNameUsage").to(ParsedNameUsage.class);
    addAlias("ScientificName").to(ScientificName.class);
    addAlias("NameUsageContainer").to(NameUsageContainer.class);
    addAlias("NameUsageMediaObject").to(NameUsageMediaObject.class);
    addAlias("NameUsageMetrics").to(NameUsageMetrics.class);
    addAlias("NameUsageWritable").to(NameUsageWritable.class);
    addAlias("ParsedName").to(ParsedName.class);
    addAlias("RawUsage").to(RawUsage.class);
    addAlias("Reference").to(Reference.class);
    addAlias("SpeciesProfile").to(SpeciesProfile.class);
    addAlias("TocEntry").to(TocEntry.class);
    addAlias("TypeSpecimen").to(TypeSpecimen.class);
    addAlias("Usage").to(Usage.class);
    addAlias("UsageCount").to(UsageCount.class);
    addAlias("UsageRelated").to(UsageRelated.class);
    addAlias("VerbatimNameUsage").to(VerbatimNameUsage.class);
    addAlias("VernacularName").to(VernacularName.class);


    // mybatis mapper
    addMapperClass(Common.class);
    addMapperClass(CitationMapper.class);
    addMapperClass(ColAnnotationMapper.class);
    addMapperClass(DatasetMapper.class);
    addMapperClass(DatasetMetricsMapper.class);
    addMapperClass(DescriptionMapper.class);
    addMapperClass(DistributionMapper.class);
    addMapperClass(IdentifierMapper.class);
    addMapperClass(MultimediaMapper.class);
    addMapperClass(NameUsageMapper.class);
    addMapperClass(NameUsageMetricsMapper.class);
    addMapperClass(NubRelMapper.class);
    addMapperClass(ParsedNameMapper.class);
    addMapperClass(RawUsageMapper.class);
    addMapperClass(ReferenceMapper.class);
    addMapperClass(SpeciesProfileMapper.class);
    addMapperClass(TypeSpecimenMapper.class);
    addMapperClass(UsageCountMapper.class);
    addMapperClass(UsageMapper.class);
    addMapperClass(VernacularNameMapper.class);
  }

  @Override
  protected void bindTypeHandlers() {
    // mybatis type handler
    handleType(Country.class).with(CountryTypeHandler.class);
    handleType(Language.class).with(LanguageTypeHandler.class);
    handleType(UUID.class).with(UuidTypeHandler.class);
    handleType(URI.class).with(UriTypeHandler.class);
  }

  @Override
  protected void bindManagers() {
    // services. Make sure they are also exposed in the public module!
    bind(NameUsageService.class).to(NameUsageServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(VernacularNameService.class).to(VernacularNameServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(ReferenceService.class).to(ReferenceServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(DescriptionService.class).to(DescriptionServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(DistributionService.class).to(DistributionServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(IdentifierService.class).to(IdentifierServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(MultimediaService.class).to(MultimediaServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(SpeciesProfileService.class).to(SpeciesProfileServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(TypeSpecimenService.class).to(TypeSpecimenServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(DatasetMetricsService.class).to(DatasetMetricsServiceMyBatis.class).in(Scopes.SINGLETON);
    // not exposed in API
    bind(UsageService.class).to(UsageServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(ParsedNameService.class).to(ParsedNameServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(UsageSyncService.class).to(UsageSyncServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(CitationService.class).to(CitationServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(ColAnnotationService.class).to(ColAnnotationServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(DatasetAnalysisService.class).to(DatasetAnalysisServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(Integer.class)
        .annotatedWith(Mybatis.class)
        .toInstance(importThreads);
    bind(DatasetImportService.class)
        .annotatedWith(Mybatis.class)
        .to(DatasetImportServiceMyBatis.class)
        .in(Scopes.SINGLETON);

    bind(NameParser.class).toInstance(new GBIFNameParser(parserTimeout));
  }
}
