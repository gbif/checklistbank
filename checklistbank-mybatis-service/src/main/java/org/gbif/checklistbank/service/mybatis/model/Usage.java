package org.gbif.checklistbank.service.mybatis.model;

import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

/**
 * A simple usage class for nub building only with minimal footprint to save memory.
 */
public class Usage {
  public int key;
  public int nameKey;
  public int parentKey;
  public int namePublishedInKey;
  public TaxonomicStatus status;
  public Rank rank;
  public Origin origin;
  public int sourceKey;
  public int accordingToKey;

  public boolean hasParent() {
    return parentKey > 0;
  }
}
