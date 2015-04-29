package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

public class SrcUsage {
  public int key;
  public Integer parentKey;
  public Integer originalNameKey;
  public String canonical;
  public NameType nameType;
  public String genus;
  public String epithet;
  public String author;
  public Integer year;
  public Rank rank;
  public TaxonomicStatus status;
  public NomenclaturalStatus[] nomStatus;
}
