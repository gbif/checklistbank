package org.gbif.checklistbank.utils;

import org.gbif.api.vocabulary.Rank;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RankUtilsTest {

  @Test
  public void testNextLowerLinneanRank() throws Exception {
    assertEquals(Rank.SPECIES, RankUtils.nextLowerLinneanRank(Rank.GENUS));
    assertEquals(Rank.GENUS, RankUtils.nextLowerLinneanRank(Rank.SUBFAMILY));
    assertEquals(Rank.SPECIES, RankUtils.nextLowerLinneanRank(Rank.SUBGENUS));
    assertEquals(Rank.PHYLUM, RankUtils.nextLowerLinneanRank(Rank.KINGDOM));
    assertEquals(Rank.KINGDOM, RankUtils.nextLowerLinneanRank(Rank.DOMAIN));
    assertEquals(Rank.SPECIES, RankUtils.nextLowerLinneanRank(Rank.INFRAGENERIC_NAME));
    assertEquals(null, RankUtils.nextLowerLinneanRank(Rank.INFRASUBSPECIFIC_NAME));
    assertEquals(null, RankUtils.nextLowerLinneanRank(Rank.VARIETY));
  }

  @Test
  public void testNextHigherLinneanRank() throws Exception {
    assertEquals(Rank.FAMILY, RankUtils.nextHigherLinneanRank(Rank.GENUS));
    assertEquals(Rank.FAMILY, RankUtils.nextHigherLinneanRank(Rank.SUBFAMILY));
    assertEquals(Rank.GENUS, RankUtils.nextHigherLinneanRank(Rank.SUBGENUS));
    assertEquals(null, RankUtils.nextHigherLinneanRank(Rank.KINGDOM));
    assertEquals(null, RankUtils.nextHigherLinneanRank(Rank.DOMAIN));
    assertEquals(Rank.SPECIES, RankUtils.nextHigherLinneanRank(Rank.VARIETY));
  }
}