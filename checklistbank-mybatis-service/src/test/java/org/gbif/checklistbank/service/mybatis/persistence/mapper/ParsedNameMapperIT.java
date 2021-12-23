package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.utils.text.StringUtils;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ParsedNameMapperIT extends MapperITBase {

  private final ParsedNameMapper mapper;

  @Autowired
  public ParsedNameMapperIT(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      DataSource dataSource) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        dataSource,
        false);
    this.mapper = parsedNameMapper;
  }

  /** Check all enum values have a matching postgres type value. */
  @Test
  public void testEnums() {
    ParsedName pn = new ParsedName();
    for (NameType p : NameType.values()) {
      pn.setKey(null);
      pn.setScientificName(StringUtils.randomSpecies());
      pn.setType(p);
      mapper.create(pn);
    }
    for (Rank r : Rank.values()) {
      pn.setKey(null);
      pn.setScientificName(StringUtils.randomSpecies());
      pn.setRank(r);
      mapper.create(pn);
    }
    for (NamePart p : NamePart.values()) {
      pn.setKey(null);
      pn.setScientificName(StringUtils.randomSpecies());
      pn.setNotho(p);
      mapper.create(pn);
    }
  }

  @Test
  public void testGetMax() {
    assertNull(mapper.maxKey());
    ParsedName pn = new ParsedName();
    pn.setType(NameType.CULTIVAR);
    for (int x = 0; x < 10; x++) {
      pn.setKey(null);
      pn.setScientificName(StringUtils.randomSpecies());
      mapper.create(pn);
    }
    assertEquals((Integer) 10, mapper.maxKey());
  }

  @Test
  public void roundtrip() throws Exception {
    ParsedName pn = new ParsedName();
    pn.setType(NameType.SCIENTIFIC);
    pn.setParsedPartially(true);
    pn.setGenusOrAbove("Abies");
    pn.setSpecificEpithet("alba");
    pn.setInfraSpecificEpithet("alpina");
    pn.setRank(Rank.INFRASUBSPECIFIC_NAME);
    pn.setBracketYear("123");
    pn.setYear("1987");
    pn.setAuthorship("Markus");
    pn.setBracketAuthorship("Peter");
    pn.setNotho(NamePart.SPECIFIC);
    pn.setRemarks("no remarks");
    pn.setNomStatus("nom.illeg.");
    pn.setInfraGeneric("Pineta");
    pn.setCultivarEpithet("Green Hell");
    pn.setSensu("Berendsohn 1999");
    pn.setStrain("train");

    pn.setScientificName(pn.fullName());

    mapper.create(pn);

    ParsedName pn2 = mapper.getByName(pn.getScientificName(), pn.getRank());
    assertEquals(pn, pn2);
  }
}
