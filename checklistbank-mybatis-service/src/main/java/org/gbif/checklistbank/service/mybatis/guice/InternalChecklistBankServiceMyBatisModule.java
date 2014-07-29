package org.gbif.checklistbank.service.mybatis.guice;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
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
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.ColAnnotationService;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.CitationMapper;
import org.gbif.checklistbank.service.mybatis.CitationServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.ColAnnotationMapper;
import org.gbif.checklistbank.service.mybatis.ColAnnotationServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DatasetImportServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DatasetMetricsMapper;
import org.gbif.checklistbank.service.mybatis.DatasetMetricsServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DescriptionMapper;
import org.gbif.checklistbank.service.mybatis.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DistributionMapper;
import org.gbif.checklistbank.service.mybatis.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.IdentifierMapper;
import org.gbif.checklistbank.service.mybatis.IdentifierServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.MultimediaMapper;
import org.gbif.checklistbank.service.mybatis.MultimediaServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.NameUsageMetricsMapper;
import org.gbif.checklistbank.service.mybatis.NameUsageServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.NubRelMapper;
import org.gbif.checklistbank.service.mybatis.ParsedNameMapper;
import org.gbif.checklistbank.service.mybatis.ParsedNameServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.RawUsageMapper;
import org.gbif.checklistbank.service.mybatis.ReferenceMapper;
import org.gbif.checklistbank.service.mybatis.ReferenceServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.SpeciesProfileMapper;
import org.gbif.checklistbank.service.mybatis.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.TypeSpecimenMapper;
import org.gbif.checklistbank.service.mybatis.TypeSpecimenServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.UsageMapper;
import org.gbif.checklistbank.service.mybatis.UsageServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.VernacularNameMapper;
import org.gbif.checklistbank.service.mybatis.VernacularNameServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.model.NameUsageWritable;
import org.gbif.checklistbank.service.mybatis.model.RawUsage;
import org.gbif.checklistbank.service.mybatis.model.TocEntry;
import org.gbif.checklistbank.service.mybatis.model.Usage;
import org.gbif.checklistbank.service.mybatis.model.UsageRelated;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.mybatis.type.CountryTypeHandler;
import org.gbif.mybatis.type.LanguageTypeHandler;
import org.gbif.mybatis.type.UriTypeHandler;
import org.gbif.mybatis.type.UuidTypeHandler;

import java.net.URI;
import java.util.UUID;

import com.google.inject.Scopes;

/**
 * This Module should not be used, use the
 * {@link org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule} instead.
 */
public class InternalChecklistBankServiceMyBatisModule extends MyBatisModule {

  public static final String DATASOURCE_BINDING_NAME = "checklistbank";

  public InternalChecklistBankServiceMyBatisModule() {
    super(DATASOURCE_BINDING_NAME);
  }

  @Override
  protected void bindMappers() {
    // mybatis config
    addAlias("Usage").to(Usage.class);
    addAlias("RawUsage").to(RawUsage.class);
    addAlias("NameUsage").to(NameUsage.class);
    addAlias("NameUsageMetrics").to(NameUsageMetrics.class);
    addAlias("NameUsageWritable").to(NameUsageWritable.class);
    addAlias("ParsedName").to(ParsedName.class);
    addAlias("VernacularName").to(VernacularName.class);
    addAlias("Reference").to(Reference.class);
    addAlias("Description").to(Description.class);
    addAlias("Identifier").to(Identifier.class);
    addAlias("Distribution").to(Distribution.class);
    addAlias("NameUsageMediaObject").to(NameUsageMediaObject.class);
    addAlias("SpeciesProfile").to(SpeciesProfile.class);
    addAlias("TypeSpecimen").to(TypeSpecimen.class);
    addAlias("DatasetMetrics").to(DatasetMetrics.class);
    addAlias("VerbatimNameUsage").to(VerbatimNameUsage.class);
    addAlias("Count").to(DatasetMetricsServiceMyBatis.Count.class);
    addAlias("TocEntry").to(TocEntry.class);
    addAlias("UsageRelated").to(UsageRelated.class);

    // mybatis mapper
    addMapperClass(UsageMapper.class);
    addMapperClass(NameUsageMapper.class);
    addMapperClass(VernacularNameMapper.class);
    addMapperClass(ReferenceMapper.class);
    addMapperClass(DescriptionMapper.class);
    addMapperClass(IdentifierMapper.class);
    addMapperClass(DistributionMapper.class);
    addMapperClass(MultimediaMapper.class);
    addMapperClass(SpeciesProfileMapper.class);
    addMapperClass(TypeSpecimenMapper.class);
    addMapperClass(DatasetMetricsMapper.class);
    addMapperClass(RawUsageMapper.class);
    addMapperClass(ParsedNameMapper.class);
    addMapperClass(CitationMapper.class);
    addMapperClass(NameUsageMetricsMapper.class);
    addMapperClass(NubRelMapper.class);
    addMapperClass(ColAnnotationMapper.class);
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
    bind(DatasetImportService.class).to(DatasetImportServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(CitationService.class).to(CitationServiceMyBatis.class).in(Scopes.SINGLETON);
    bind(ColAnnotationService.class).to(ColAnnotationServiceMyBatis.class).in(Scopes.SINGLETON);
  }
}
