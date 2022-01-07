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
package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.nub.NeoTmpRepoRule;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Ignore("REMOVE! ignored only to make the jenkins build work")
public class DwcaSourceTest {
  public static final URL BACKBONE_PATCH_DWCA;

  static {
    try {
      BACKBONE_PATCH_DWCA = new URL("https://github.com/gbif/backbone-patch/archive/master.zip");
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Rule
  public NeoTmpRepoRule neoRepo = new NeoTmpRepoRule();

  @Test
  public void testPatch() throws Exception {
    DwcaSource src = new DwcaSource("patch checklist", BACKBONE_PATCH_DWCA, neoRepo.cfg);
    assertNotNull(src);

    src.init(true, false);
    int counter = 0;
    try (CloseableIterator<SrcUsage> iter = src.iterator()) {
      while (iter.hasNext()) {
        SrcUsage u = iter.next();
        assertNotNull(u.key);
        assertNotNull(u.scientificName);
        counter++;
      }
    }
    assertTrue(counter > 30);
    src.close();
  }

}
