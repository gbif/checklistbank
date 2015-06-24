package org.gbif.checklistbank.nub.authorship;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LongestCommonSubstringTest {

  @Test
  public void testLcs() throws Exception {
    assertEquals("", LongestCommonSubstring.lcs("a", "node"));
    assertEquals("ma", LongestCommonSubstring.lcs("markus", "mama"));
    assertEquals("mar", LongestCommonSubstring.lcs("markus döring", "carla maria möglich"));
  }
}