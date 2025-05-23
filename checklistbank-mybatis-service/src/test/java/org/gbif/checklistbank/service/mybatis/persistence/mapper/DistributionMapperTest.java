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
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.model.IucnRedListCategory;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.ClbDbLoadTestDataBeforeEach;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ClbDbLoadTestDataBeforeEach.class)
@TestData
public class DistributionMapperTest extends MapperITBase {

  private final DistributionMapper mapper;

  @Autowired
  public DistributionMapperTest(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      DistributionMapper distributionMapper) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        true);
    this.mapper = distributionMapper;
  }

  @Test
  public void testMapper() throws Exception {
    assertTrue(mapper.listByChecklistUsage(usageKey, new PagingRequest()).isEmpty());
    assertTrue(mapper.listByNubUsage(usageKey, new PagingRequest()).isEmpty());

    Distribution obj = new Distribution();
    obj.setAppendixCites(CitesAppendix.II);
    obj.setCountry(null); // global
    obj.setEndDayOfYear(1890);
    obj.setEstablishmentMeans(EstablishmentMeans.NATIVE);
    obj.setLifeStage(LifeStage.EMRYO);
    obj.setLocality("location location location");
    obj.setLocationId("locID");
    obj.setRemarks("remarks");
    obj.setStartDayOfYear(1889);
    obj.setStatus(DistributionStatus.COMMON);
    obj.setTemporal("aha");
    obj.setThreatStatus(ThreatStatus.CRITICALLY_ENDANGERED);
    // these should get ignored
    obj.setSource("sourcy s");
    obj.setSourceTaxonKey(123);
    mapper.insert(usageKey, obj, citationKey1);

    obj = new Distribution();
    obj.setAppendixCites(CitesAppendix.II);
    obj.setCountry(Country.ALGERIA);
    obj.setEndDayOfYear(1990);
    obj.setEstablishmentMeans(EstablishmentMeans.NATIVE);
    obj.setLifeStage(LifeStage.EMRYO);
    obj.setLocality("location location location");
    obj.setLocationId("locID");
    obj.setRemarks("remarks");
    obj.setStartDayOfYear(1989);
    obj.setStatus(DistributionStatus.COMMON);
    obj.setTemporal("aha");
    obj.setThreatStatus(ThreatStatus.CRITICALLY_ENDANGERED);
    // these should get ignored
    obj.setSource("sourcy s");
    obj.setSourceTaxonKey(123);
    mapper.insert(usageKey, obj, citationKey1);

    List<Distribution> list = mapper.listByChecklistUsage(usageKey, new PagingRequest());
    assertEquals(2, list.size());
    assertNull(list.get(0).getCountry());
    assertObject(obj, list.get(1), citation1, null);

    list = mapper.listByNubUsage(nubKey, new PagingRequest());
    assertEquals(2, list.size());
    // these are now nub source usage values
    assertNull(list.get(0).getCountry());
    assertObject(obj, list.get(1), datasetTitle, usageKey);
  }

  private Integer setupSpecies(UUID datasetKey, Integer parentKey, TaxonomicStatus status, String taxonID) {
    ParsedName pn = new ParsedName();
    pn.setType(NameType.SCIENTIFIC);
    pn.setGenusOrAbove("Abies");
    pn.setSpecificEpithet("alba-" + UUID.randomUUID().toString().replaceAll("-", ""));
    pn.setRank(Rank.SPECIES);
    pn.setScientificName(pn.getGenusOrAbove() + " " + pn.getSpecificEpithet());
    parsedNameMapper.create(pn);

    NameUsageWritable uw = new NameUsageWritable();
    uw.setDatasetKey(datasetKey);
    uw.setParentKey(parentKey);
    uw.setOrigin(Origin.SOURCE);
    uw.setTaxonID(taxonID);
    uw.setTaxonomicStatus(status);
    uw.setSynonym(status.isSynonym());
    uw.setRank(pn.getRank());
    uw.setNameKey(pn.getKey());
    nameUsageMapper.insert(uw);

    return uw.getKey();
  }

  private void setupIucnNubRel(int usageKey, int nubKey) {
    nubRelMapper.insert(DistributionMapper.iucnDatasetKey, usageKey, nubKey);
  }

  @Test
  public void testIUCN() throws Exception {
    // setup NUB usages
    int nub1 = setupSpecies(Constants.NUB_DATASET_KEY, null, TaxonomicStatus.ACCEPTED, null);
    int nub2 = setupSpecies(Constants.NUB_DATASET_KEY, null, TaxonomicStatus.ACCEPTED, null);
    int nub3 = setupSpecies(Constants.NUB_DATASET_KEY, null, TaxonomicStatus.ACCEPTED, null);

    // setup IUCN record and matching nub relation
    int acc1 = setupSpecies(DistributionMapper.iucnDatasetKey, null, TaxonomicStatus.ACCEPTED, "iucn:001");
    int acc2 = setupSpecies(DistributionMapper.iucnDatasetKey, null, TaxonomicStatus.ACCEPTED, "iucn:002");
    int syn = setupSpecies(DistributionMapper.iucnDatasetKey, acc2, TaxonomicStatus.HETEROTYPIC_SYNONYM, "iucn:003");

    Distribution d = new Distribution();
    d.setLocality("global");
    d.setThreatStatus(ThreatStatus.CRITICALLY_ENDANGERED);
    mapper.insert(acc1, d, null);
    d.setThreatStatus(ThreatStatus.VULNERABLE);
    mapper.insert(acc2, d, null);

    // map acc1 to an accepted nub usage, but
    setupIucnNubRel(acc1, nub1);
    setupIucnNubRel(acc2, nub2);
    setupIucnNubRel(syn, nub3);


    IucnRedListCategory iucn = mapper.getIucnRedListCategory(nub1);
    assertEquals(ThreatStatus.CRITICALLY_ENDANGERED, iucn.getCategory());
    assertEquals("iucn:001", iucn.getIucnTaxonID());

    iucn = mapper.getIucnRedListCategory(nub2);
    assertEquals(ThreatStatus.VULNERABLE, iucn.getCategory());
    assertEquals("iucn:002", iucn.getIucnTaxonID());

    iucn = mapper.getIucnRedListCategory(nub3);
    assertEquals(ThreatStatus.VULNERABLE, iucn.getCategory());
    assertEquals("iucn:003", iucn.getIucnTaxonID());

    // no usage
    assertNull(mapper.getIucnRedListCategory(99999999));
    // no mapped IUCN species
    assertNull(mapper.getIucnRedListCategory(100000000));
  }

  private void assertObject(
      Distribution obj, Distribution obj2, String source, Integer sourceTaxonKey) {
    assertEquals(obj.getAppendixCites(), obj2.getAppendixCites());
    assertEquals(obj.getCountry(), obj2.getCountry());
    assertEquals(obj.getEndDayOfYear(), obj2.getEndDayOfYear());
    assertEquals(obj.getEstablishmentMeans(), obj2.getEstablishmentMeans());
    assertEquals(obj.getLifeStage(), obj2.getLifeStage());
    assertEquals(obj.getLocality(), obj2.getLocality());
    assertEquals(obj.getLocationId(), obj2.getLocationId());
    assertEquals(obj.getRemarks(), obj2.getRemarks());
    assertEquals(obj.getStartDayOfYear(), obj2.getStartDayOfYear());
    assertEquals(obj.getStatus(), obj2.getStatus());
    assertEquals(obj.getTemporal(), obj2.getTemporal());
    assertEquals(obj.getThreatStatus(), obj2.getThreatStatus());

    assertEquals(source, obj2.getSource());
    assertEquals(sourceTaxonKey, obj2.getSourceTaxonKey());
  }
}
