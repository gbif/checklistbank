package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.model.Citation;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.gbif.utils.text.StringUtils;

import java.util.Properties;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.Rule;

public class MapperITBase<T> {
  private static final String PREFIX = "checklistbank.db.";

  protected T mapper;
  private Injector injector;
  private final Class<T> mapperClass;
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

  @Rule
  public ClbDbTestRule sbSetup;

  public MapperITBase(Class<T> mapperClass, ClbDbTestRule rule) {
    this.mapperClass = mapperClass;
    sbSetup = rule;
    this.initData = false;
  }

  public MapperITBase(Class<T> mapperClass, boolean initData) {
    this.mapperClass = mapperClass;
    this.initData = initData;
    this.sbSetup = ClbDbTestRule.empty();
  }

  @Before
  public void init() throws Exception {
    Module module = new InternalChecklistBankServiceMyBatisModule(strippedProperties(sbSetup.getProperties()), 500);
    injector = Guice.createInjector(module);
    mapper = injector.getInstance(mapperClass);
    if (initData) {
      initData();
    }
  }

  private void initData() throws Exception {
    parsedNameMapper = getInstance(ParsedNameMapper.class);
    nameUsageMapper = getInstance(NameUsageMapper.class);
    nubRelMapper = getInstance(NubRelMapper.class);
    datasetMapper = getInstance(DatasetMapper.class);
    citationMapper = getInstance(CitationMapper.class);

    ParsedName pn = new ParsedName();
    pn.setType(NameType.SCIENTIFIC);
    pn.setScientificName(StringUtils.randomSpecies());
    parsedNameMapper.create(pn, pn.getScientificName());
    final int nameKey = pn.getKey();

    datasetKey = UUID.randomUUID();
    datasetMapper.insert(datasetKey, datasetTitle);

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

  /**
   * Filtered properties also removing the given property prefix string
   */
  public static Properties strippedProperties(Properties properties) {
    Properties filtered = new Properties();

    for (String key : properties.stringPropertyNames()) {
      if (key.startsWith(PREFIX)) {
        filtered.setProperty(key.substring(PREFIX.length()), properties.getProperty(key));
      }
    }
    return filtered;
  }

  public <K> K getInstance(Class<K> clazz) {
    return injector.getInstance(clazz);
  }
}