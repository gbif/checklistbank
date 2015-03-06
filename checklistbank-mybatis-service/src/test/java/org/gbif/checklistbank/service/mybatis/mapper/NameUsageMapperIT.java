package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.service.mybatis.postgres.MybatisMapperTestRule;

import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class NameUsageMapperIT {

  private static final UUID DATASET_KEY = UUID.randomUUID();

  private ParsedNameMapper nameMapper;
  private CitationMapper citationMapper;

  @Rule
  public MybatisMapperTestRule<NameUsageMapper> ddt = new MybatisMapperTestRule(NameUsageMapper.class);

  @Before
  public void setup() {
    nameMapper = ddt.getInjector().getInstance(ParsedNameMapper.class);
    citationMapper = ddt.getInjector().getInstance(CitationMapper.class);
  }

  private int createName(String name){
    ParsedName pn = new ParsedName();
    pn.setType(NameType.WELLFORMED);
    pn.setScientificName(name);
    return nameMapper.create(pn, name);

  }

  /**
   * Check all enum values have a matching postgres type value.
   */
  @Test
  public void testEnums() {
    int nameKey = createName("Abies alba Mill.");

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
}