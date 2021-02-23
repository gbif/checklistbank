package org.gbif.checklistbank.nub.source;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;

import java.util.*;


public class RandomSourceList extends NubSourceList {

  /**
   * Creates a random source list based on given dataset keys.
   */
  public static RandomSourceList source(NeoConfiguration neo, int datasetSize, int numDatasets) {
    List<RandomSource> sources = new ArrayList<>(numDatasets);
    Random rnd = new Random();
    while (numDatasets>1) {
      Kingdom k = Kingdom.byNubUsageKey(rnd.nextInt(9));
      sources.add(new RandomSource(datasetSize, k, neo));
      numDatasets--;
    }
    return new RandomSourceList(neo, sources);
  }

  private RandomSourceList(NeoConfiguration neo, Iterable<RandomSource> sources) {
    super(new NubConfiguration(neo));
    submitSources(sources);
  }
  
}
