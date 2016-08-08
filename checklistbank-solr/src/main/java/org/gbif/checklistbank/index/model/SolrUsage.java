package org.gbif.checklistbank.index.model;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.model.UsageExtensions;

import java.util.List;
import javax.annotation.Nullable;

/**
 * A name usage with all associated data we index in solr
 */
public class SolrUsage {
  public final NameUsage usage;
  public final List<Integer> parents;
  public final @Nullable UsageExtensions extensions;

  public SolrUsage(NameUsage usage, List<Integer> parents, UsageExtensions extensions) {
    this.usage = usage;
    this.parents = parents;
    this.extensions = extensions;
  }
}