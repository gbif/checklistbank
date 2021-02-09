package org.gbif.checklistbank.service.mybatis.guice;

import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.IdentifierService;
import org.gbif.api.service.checklistbank.MultimediaService;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.service.checklistbank.ReferenceService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.UsageSyncService;
import org.gbif.checklistbank.service.mybatis.mapper.DistributionMapper;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.service.mybatis.mapper.UsageCountMapper;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.service.guice.PrivateServiceModule;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice Module to use the database backed ChecklistBank MyBatis layer.
 * To use this instantiate this module using one of the available constructors and provide the required properties. You
 * need to provide at least the following entries for this to work. All properties have to be
 * prefixed with {@code checklistbank.db}:
 * <ul>
 * <li>{@code checklistbank.db.dataSource.serverName</li>
 * <li>{@code checklistbank.db.dataSource.databaseName</li>
 * <li>{@code checklistbank.db.dataSource.user</li>
 * <li>{@code checklistbank.db.dataSource.password</li>
 * </ul>
 * You can also use any other properties that MyBatis or HikariCP understands as long as they
 * have the proper prefix.
 * See https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby
 */

public class ChecklistBankServiceMyBatisModule extends PrivateServiceModule implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(ChecklistBankServiceMyBatisModule.class);
  private static final String PREFIX = "checklistbank.db.";

  private final int parserTimeout;
  private final int importThreads;

  /**
   * Uses the given properties to configure the service.
   *
   * @param properties to use
   */
  public ChecklistBankServiceMyBatisModule(Properties properties) {
    super(PREFIX, properties);
    parserTimeout = Integer.parseInt(properties.getProperty(ClbConfiguration.PARSER_TIMEOUT_PROP, "500"));
    importThreads = Integer.parseInt(properties.getProperty(ClbConfiguration.IMPORT_THREADS_PROP, "2"));
  }

  public static ChecklistBankServiceMyBatisModule create(ClbConfiguration cfg) {
    LOG.info("Connecting to checklistbank db {} on {} with user {}", cfg.databaseName, cfg.serverName, cfg.user);
    return new ChecklistBankServiceMyBatisModule(cfg.toProps(true));
  }

  @Provides
  @Singleton
  public ClbConfiguration provideCfg() throws Exception {
    ClbConfiguration cfg = new ClbConfiguration();
    cfg.serverName = getProperties().getProperty("dataSource.serverName");
    cfg.databaseName = getProperties().getProperty("dataSource.databaseName");
    cfg.user = getProperties().getProperty("dataSource.user");
    cfg.password = getProperties().getProperty("dataSource.password");
    cfg.parserTimeout = parserTimeout;
    cfg.syncThreads = importThreads;
    cfg.maximumPoolSize = getIntProp("maximumPoolSize", cfg.maximumPoolSize);
    cfg.minimumIdle = getIntProp("minimumIdle", cfg.minimumIdle);
    cfg.idleTimeout = getIntProp("idleTimeout", cfg.idleTimeout);
    cfg.maxLifetime = getIntProp("maxLifetime", cfg.maxLifetime);
    cfg.connectionTimeout = getIntProp("connectionTimeout", cfg.connectionTimeout);

    LOG.info("Provide {}", cfg.toString());
    return cfg;
  }

  private int getIntProp(String name, int defaultVal) {
    try {
      return Integer.valueOf(getProperties().getProperty(name, "x"));
    } catch (NumberFormatException e) {
      return defaultVal;
    }
  }

  @Override
  protected void configureService() {

    // default parser timeout is 500ms
    // install mybatis module
    MyBatisModule mybatModule = new InternalChecklistBankServiceMyBatisModule(getProperties(), parserTimeout, importThreads);
    install(mybatModule);
    // expose a named datasource binding and session manager for transactions
    expose(mybatModule.getDatasourceKey());
    expose(mybatModule.getSessionManagerKey());

    expose(NameUsageService.class);
    expose(VernacularNameService.class);
    expose(ReferenceService.class);
    expose(DescriptionService.class);
    expose(DistributionService.class);
    expose(IdentifierService.class);
    expose(MultimediaService.class);
    expose(SpeciesProfileService.class);
    expose(TypeSpecimenService.class);
    expose(DatasetMetricsService.class);
    // not available in API:
    expose(UsageService.class);
    expose(ParsedNameService.class);
    expose(NameParser.class);
    expose(CitationService.class);
    expose(DatasetAnalysisService.class);
    expose(UsageCountMapper.class);
    expose(DistributionMapper.class);

    expose(DatasetImportService.class).annotatedWith(Mybatis.class);
    expose(UsageSyncService.class);

    // mappers being exposed
    expose(NameUsageMapper.class);

    expose(ClbConfiguration.class);
  }

  @Override
  public void close() throws IOException {

  }
}
