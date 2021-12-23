package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.model.Citation;
import org.gbif.checklistbank.model.DatasetCore;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule2;
import org.gbif.utils.text.StringUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.ConnectionInfo;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = MapperITBase.ChecklistBankMappersTestConfiguration.class)
@ContextConfiguration(initializers = {MapperITBase.ContextInitializer.class})
@ActiveProfiles("test")
public class MapperITBase {

  @TestConfiguration
  @PropertySource("classpath:application-test.yml")
  @MapperScan("org.gbif.checklistbank.service.mybatis.persistence.mapper")
  @ComponentScan(basePackages = "org.gbif.checklistbank.service.mybatis.persistence")
  @SpringBootApplication(exclude = {RabbitAutoConfiguration.class})
  public static class ChecklistBankMappersTestConfiguration {
    public static void main(String[] args) {
      SpringApplication.run(ChecklistBankMappersTestConfiguration.class, args);
    }
  }

  /** Custom ContextInitializer to expose the registry DB data source and search flags. */
  public static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private final List<Consumer<EmbeddedPostgres.Builder>> builderCustomizers =
        new CopyOnWriteArrayList<>();

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      try {
        LiquibasePreparer liquibasePreparer =
            LiquibasePreparer.forClasspathLocation("liquibase/master.xml");
        PreparedDbProvider provider =
            PreparedDbProvider.forPreparer(liquibasePreparer, builderCustomizers);
        ConnectionInfo connectionInfo = provider.createNewDatabase();

        TestPropertyValues.of(Stream.of(dbTestPropertyPairs(connectionInfo)).toArray(String[]::new))
            .applyTo(configurableApplicationContext.getEnvironment());
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    /** Creates the registry datasource settings from the embedded database. */
    String[] dbTestPropertyPairs(ConnectionInfo connectionInfo) {
      return new String[] {
        "checklistbank.datasource.url=jdbc:postgresql://localhost:"
            + connectionInfo.getPort()
            + "/"
            + connectionInfo.getDbName(),
        "checklistbank.datasource.username=" + connectionInfo.getUser(),
        "checklistbank.datasource.password="
      };
    }
  }

  private final boolean initData;

  protected final String datasetTitle = "My Favorite Checklist";
  protected final String citation1 = "citeme one";
  protected int citationKey1;
  protected final String citation2 = "citeme two";
  protected final String citation2doi = "doi:10.10003/citeme two";
  protected final String citation2link = "http://purl.org/citeme/two";
  protected int citationKey2;
  protected int usageKey;
  protected int nubKey;
  protected UUID datasetKey;

  protected ParsedNameMapper parsedNameMapper;
  protected NameUsageMapper nameUsageMapper;
  protected NubRelMapper nubRelMapper;
  protected DatasetMapper datasetMapper;
  protected CitationMapper citationMapper;
  private DataSource dataSource;

  @RegisterExtension public ClbDbTestRule2 sbSetup;

  @Autowired
  public MapperITBase(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      DataSource dataSource,
      boolean initData) {
    this(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        dataSource,
        initData,
        ClbDbTestRule2.empty(dataSource));
  }

  @Autowired
  public MapperITBase(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      DataSource dataSource,
      boolean initData,
      ClbDbTestRule2 rule) {
    this.parsedNameMapper = parsedNameMapper;
    this.nameUsageMapper = nameUsageMapper;
    this.nubRelMapper = nubRelMapper;
    this.datasetMapper = datasetMapper;
    this.citationMapper = citationMapper;
    this.dataSource = dataSource;
    this.initData = initData;
    this.sbSetup = rule;
  }

  public MapperITBase(boolean initData) {
    this.initData = initData;
  }

  @BeforeEach
  public void init() throws Exception {
    if (initData) {
      initData();
    }
  }

  private void initData() throws Exception {
    ParsedName pn = new ParsedName();
    pn.setType(NameType.SCIENTIFIC);
    pn.setScientificName(StringUtils.randomSpecies());
    parsedNameMapper.create(pn);
    final int nameKey = pn.getKey();

    datasetKey = UUID.randomUUID();
    DatasetCore d = new DatasetCore();
    d.setKey(datasetKey);
    d.setTitle(datasetTitle);
    datasetMapper.insert(d);

    NameUsageWritable uw = new NameUsageWritable();
    uw.setNameKey(nameKey);
    uw.setDatasetKey(datasetKey);
    uw.setOrigin(Origin.SOURCE);
    uw.setRank(Rank.SPECIES);
    uw.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
    nameUsageMapper.insert(uw);
    usageKey = uw.getKey();

    NameUsageWritable nub = new NameUsageWritable();
    nub.setNameKey(nameKey);
    nub.setDatasetKey(Constants.NUB_DATASET_KEY);
    nub.setOrigin(Origin.SOURCE);
    nub.setRank(Rank.SPECIES);
    nub.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
    nameUsageMapper.insert(nub);
    nubKey = nub.getKey();

    nubRelMapper.insert(datasetKey, usageKey, nubKey);

    Citation c = new Citation(citation1);
    citationMapper.insert(c);
    citationKey1 = c.getKey();

    c = new Citation(citation2);
    c.setDoi(citation2doi);
    c.setLink(citation2link);
    citationMapper.insert(c);
    citationKey2 = c.getKey();
  }
}
