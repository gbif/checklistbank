package org.gbif.checklistbank.nub.model;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

public class SrcUsage {
  public Integer key;
  public Integer parentKey;
  public Integer originalNameKey;
  public String scientificName;
  public String publishedIn;
  public ParsedName parsedName;
  public Rank rank;
  public TaxonomicStatus status;
  public NomenclaturalStatus[] nomStatus;
}
