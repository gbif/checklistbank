package org.gbif.checklistbank.service.mybatis.guice;

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
import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.IdentifierService;
import org.gbif.api.service.checklistbank.MultimediaService;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.service.checklistbank.ReferenceService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.model.RawUsage;
import org.gbif.checklistbank.model.TocEntry;
import org.gbif.checklistbank.model.Usage;
import org.gbif.checklistbank.model.UsageCount;
import org.gbif.checklistbank.model.UsageRelated;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.ColAnnotationService;
import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.UsageSyncService;
import org.gbif.checklistbank.service.mybatis.CitationServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.ColAnnotationServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DatasetAnalysisServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DatasetImportServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DatasetMetricsServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.IdentifierServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.MultimediaServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.NameUsageServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.ParsedNameServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.ReferenceServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.TypeSpecimenServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.UsageServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.UsageSyncServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.VernacularNameServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.mapper.CitationMapper;
import org.gbif.checklistbank.service.mybatis.mapper.ColAnnotationMapper;
import org.gbif.checklistbank.service.mybatis.mapper.DatasetMapper;
import org.gbif.checklistbank.service.mybatis.mapper.DatasetMetricsMapper;
import org.gbif.checklistbank.service.mybatis.mapper.DescriptionMapper;
import org.gbif.checklistbank.service.mybatis.mapper.DistributionMapper;
import org.gbif.checklistbank.service.mybatis.mapper.IdentifierMapper;
import org.gbif.checklistbank.service.mybatis.mapper.MultimediaMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMetricsMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NubRelMapper;
import org.gbif.checklistbank.service.mybatis.mapper.ParsedNameMapper;
import org.gbif.checklistbank.service.mybatis.mapper.RawUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.ReferenceMapper;
import org.gbif.checklistbank.service.mybatis.mapper.SpeciesProfileMapper;
import org.gbif.checklistbank.service.mybatis.mapper.TypeSpecimenMapper;
import org.gbif.checklistbank.service.mybatis.mapper.UsageCountMapper;
import org.gbif.checklistbank.service.mybatis.mapper.UsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.VernacularNameMapper;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.mybatis.type.CountryTypeHandler;
import org.gbif.mybatis.type.LanguageTypeHandler;
import org.gbif.mybatis.type.UriTypeHandler;
import org.gbif.mybatis.type.UuidTypeHandler;
import org.gbif.nameparser.NameParser;

import java.net.URI;
import java.util.Properties;
import java.util.UUID;
import javax.sql.DataSource;

import com.google.common.base.Preconditions;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    addAlias("DatasetMetrics").to(DatasetMetrics.class);
    addAlias("Description").to(Description.class);
    addAlias("Distribution").to(Distribution.class);
    addAlias("Identifier").to(Identifier.class);
    addAlias("NameUsage").to(NameUsage.class);
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

    bind(NameParser.class).toInstance(new NameParser(parserTimeout));
  }
}
