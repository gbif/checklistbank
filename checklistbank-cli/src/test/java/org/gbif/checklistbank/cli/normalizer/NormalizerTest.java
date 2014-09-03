package org.gbif.checklistbank.cli.normalizer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NormalizerTest {

  @Test
  public void testSplitByCommonDelimiters() throws Exception {
    assertThat(Normalizer.splitByCommonDelimiters("gx:1234")).containsExactly("gx:1234");
    assertThat(Normalizer.splitByCommonDelimiters("1234|135286|678231612")).containsExactly("1234","135286","678231612");
    assertThat(Normalizer.splitByCommonDelimiters("1234  135286 678231612")).containsExactly("1234","135286","678231612");
    assertThat(Normalizer.splitByCommonDelimiters("1234; 135286; 678231612")).containsExactly("1234","135286","678231612");
    assertThat(Normalizer.splitByCommonDelimiters("1234,135286 | 67.8231612")).containsExactly("1234,135286","67.8231612");
  }
}