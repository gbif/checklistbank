package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.service.mybatis.postgres.MybatisMapperTestRule;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class NameUsageMapperIT {

  private static final UUID DATASET_KEY = UUID.randomUUID();

  private ParsedNameMapper nameMapper;

  @Rule
  public MybatisMapperTestRule<NameUsageMapper> ddt = new MybatisMapperTestRule(NameUsageMapper.class);

  @Before
  public void setup() {
    nameMapper = ddt.getInjector().getInstance(ParsedNameMapper.class);
  }

  private int createName(String name){
    ParsedName pn = new ParsedName();
    pn.setType(NameType.WELLFORMED);
    pn.setScientificName(name);
    nameMapper.create(pn, name);
    return pn.getKey();
  }

  private void deleteName(String name){
    ParsedName pn = nameMapper.getByName(name);
    if (pn != null) {
      nameMapper.delete(pn.getKey());
    }
  }

  /**
   * Check all enum values have a matching postgres type value.
   */
  @Test
  public void testEnums() {
    String name = "Abies alba Mill.";
    deleteName(name);
    int nameKey = createName(name);

    NameUsageWritable u = new NameUsageWritable();
    u.setDatasetKey(DATASET_KEY);
    u.setNameKey(nameKey);
    for (Rank r : Rank.values()) {
      u.setRank(r);
      ddt.getService().insert(u);
    }
    for (Origin o : Origin.values()) {
      u.setOrigin(o);
      ddt.getService().insert(u);
    }
    for (TaxonomicStatus s : TaxonomicStatus.values()) {
      u.setTaxonomicStatus(s);
      ddt.getService().insert(u);
    }
    for (NomenclaturalStatus s : NomenclaturalStatus.values()) {
      u.getNomenclaturalStatus().add(s);
      ddt.getService().insert(u);
    }
    for (NameUsageIssue s : NameUsageIssue.values()) {
      u.getIssues().add(s);
      ddt.getService().insert(u);
    }
  }

  /**
   * Check all enum values have a matching postgres type value.
   */
  @Test
  public void testListUsageRange() {
    List<NameUsage> list = ddt.getService().listRange(0, 1000);
  }
}