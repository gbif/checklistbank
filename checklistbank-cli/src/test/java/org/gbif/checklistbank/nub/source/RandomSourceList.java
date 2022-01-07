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

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;

import java.util.*;


public class RandomSourceList extends NubSourceList {

  /**
   * Creates a random source list based on given dataset keys.
   */
  public static RandomSourceList source(int loaderThreads, NeoConfiguration neo, int datasetSize, int numDatasets) {
    List<RandomSource> sources = new ArrayList<>(numDatasets);
    Random rnd = new Random();
    while (numDatasets>1) {
      Kingdom k = Kingdom.byNubUsageKey(rnd.nextInt(9));
      sources.add(new RandomSource(datasetSize, k, neo));
      numDatasets--;
    }
    NubConfiguration cfg = new NubConfiguration(neo);
    cfg.sourceLoaderThreads=loaderThreads;
    return new RandomSourceList(cfg, sources);
  }

  private RandomSourceList(NubConfiguration cfg, Iterable<RandomSource> sources) {
    super(cfg);
    submitSources(sources);
  }
  
}
