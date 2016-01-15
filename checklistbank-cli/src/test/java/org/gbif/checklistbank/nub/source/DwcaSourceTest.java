package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.nub.model.SrcUsage;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class DwcaSourceTest {
  public static final URL BACKBONE_PATCH_DWCA;
  static {
    try {
      BACKBONE_PATCH_DWCA = new URL("https://github.com/gbif/backbone-patch/archive/master.zip");
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testPatch() throws Exception {
    DwcaSource src = new DwcaSource("patch checklist", BACKBONE_PATCH_DWCA);
    assertNotNull(src);

    src.init(true, false);
    int counter = 0;
    for (SrcUsage u : src) {
      assertNotNull(u.key);
      assertNotNull(u.scientificName);
      counter++;
    }
    assertTrue(counter > 30);
    src.close();
  }

}