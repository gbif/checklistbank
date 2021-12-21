package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.model.Citation;
import org.gbif.checklistbank.model.DatasetCore;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisConfiguration;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule2;
import org.gbif.utils.text.StringUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

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
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
  classes = MapperITBase2.ChecklistBankMyBatisTestConfiguration.class)
@ContextConfiguration(initializers = {MapperITBase2.ContextInitializer.class})
@ActiveProfiles("test")
public class MapperITBase2<T> {

  @TestConfiguration
  @PropertySource("classpath:application-test.yml")
  @MapperScan("org.gbif.checklistbank.service.mybatis.mapper")
  @SpringBootApplication(exclude = {
    RabbitAutoConfiguration.class
  })
  @ComponentScan(basePackages = "org.gbif.checklistbank.service.mybatis")
  public static class ChecklistBankMyBatisTestConfiguration {
    public static void main(String[] args) {
      SpringApplication.run(ChecklistBankMyBatisTestConfiguration.class, args);
    }
  }

  /** Custom ContextInitializer to expose the registry DB data source and search flags. */
  public static class ContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {


    }
  }

  private final boolean initData;

  final protected String datasetTitle = "My Favorite Checklist";
  final protected String citation1 = "citeme one";
  protected int citationKey1;
  final protected String citation2 = "citeme two";
  final protected String citation2doi = "doi:10.10003/citeme two";
  final protected String citation2link = "http://purl.org/citeme/two";
  protected int citationKey2;
  protected int usageKey;
  protected int nubKey;
  protected UUID datasetKey;

  private ParsedNameMapper parsedNameMapper;
  private NameUsageMapper nameUsageMapper;
  private NubRelMapper nubRelMapper;
  private DatasetMapper datasetMapper;
  private CitationMapper citationMapper;
  private DataSource dataSource;

  @RegisterExtension
  public ClbDbTestRule2 sbSetup;

  @Autowired
  public MapperITBase2(
    ParsedNameMapper parsedNameMapper,
    NameUsageMapper nameUsageMapper,
    NubRelMapper nubRelMapper,
    DatasetMapper datasetMapper,
    CitationMapper citationMapper,
    DataSource dataSource,
    Optional<Boolean> initData
  ) {
    this.parsedNameMapper = parsedNameMapper;
    this.nameUsageMapper = nameUsageMapper;
    this.nubRelMapper = nubRelMapper;
    this.datasetMapper = datasetMapper;
    this.citationMapper = citationMapper;
    this.dataSource = dataSource;
    this.initData = initData.orElse(false);
    this.sbSetup = ClbDbTestRule2.empty(dataSource);
  }

  public MapperITBase2(boolean initData) {
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