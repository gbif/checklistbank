package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.nub.model.SrcUsage;

import java.util.List;

public interface UsageSource {

  List<NubSource> listSources();

  /**
   * Returns a neo db backed iterable that can be used to iterate over all usages in the source multiple times.
   * The iteration is in taxonomic order, starting with the highest root taxa and walks
   * the taxonomic tree in depth order first, including synonyms.
   */
  Iterable<SrcUsage> iterateSource(NubSource source);

}
