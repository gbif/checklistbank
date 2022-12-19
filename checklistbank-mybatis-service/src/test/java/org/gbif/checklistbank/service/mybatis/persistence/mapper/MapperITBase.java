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
package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.BaseDBTest;
import org.gbif.checklistbank.model.Citation;
import org.gbif.checklistbank.model.DatasetCore;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.ClbDbLoadTestDataBeforeAll;
import org.gbif.utils.text.StringUtils;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = MapperITBase.ChecklistBankMappersTestConfiguration.class)
@ActiveProfiles("test")
@ExtendWith(ClbDbLoadTestDataBeforeAll.class)
public class MapperITBase extends BaseDBTest {

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

  @Autowired
  public MapperITBase(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      boolean initData) {
    this.parsedNameMapper = parsedNameMapper;
    this.nameUsageMapper = nameUsageMapper;
    this.nubRelMapper = nubRelMapper;
    this.datasetMapper = datasetMapper;
    this.citationMapper = citationMapper;
    this.initData = initData;
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

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("checklistbank.datasource.url", PG_CONTAINER::getJdbcUrl);
    registry.add("checklistbank.datasource.username", PG_CONTAINER::getUsername);
    registry.add("checklistbank.datasource.password", PG_CONTAINER::getPassword);
  }
}
