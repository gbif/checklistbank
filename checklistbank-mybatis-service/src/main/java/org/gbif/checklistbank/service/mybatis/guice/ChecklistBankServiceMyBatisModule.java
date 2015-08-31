package org.gbif.checklistbank.service.mybatis.guice;

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
import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.ColAnnotationService;
import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.nameparser.NameParser;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

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

public class ChecklistBankServiceMyBatisModule extends PrivateServiceModule {
    private static final String PREFIX = "checklistbank.db.";
    public static final String PARSER_TIMEOUT_PROP = "checklistbank.parser.timeout";
    private final int parserTimeout;

    /**
     * Uses the given properties to configure the service.
     *
     * @param properties to use
     */
    public ChecklistBankServiceMyBatisModule(Properties properties) {
        super(PREFIX, properties);
        parserTimeout = Integer.parseInt(properties.getProperty(PARSER_TIMEOUT_PROP, "500"));
    }

    @Override
    protected void configureService() {
        // there is no validation happening currently
        //install(new ValidationModule());

        // default parser timeout is 500ms
        // install mybatis module
        MyBatisModule mybatModule = new InternalChecklistBankServiceMyBatisModule(getProperties(), parserTimeout);
        install(mybatModule);
        // expose a named datasource binding
        expose(mybatModule.getDatasourceKey());

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
        expose(DatasetImportService.class);
        expose(CitationService.class);
        expose(ColAnnotationService.class);
        expose(DatasetAnalysisService.class);
    }

}
