package org.gbif.checklistbank.nub.model;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.util.Arrays;
import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SrcUsage)) return false;
    SrcUsage srcUsage = (SrcUsage) o;
    return Objects.equals(key, srcUsage.key)
           && Objects.equals(parentKey, srcUsage.parentKey)
           && Objects.equals(originalNameKey, srcUsage.originalNameKey)
           && Objects.equals(scientificName, srcUsage.scientificName)
           && Objects.equals(publishedIn, srcUsage.publishedIn)
           && Objects.equals(parsedName, srcUsage.parsedName)
           && rank == srcUsage.rank
           && status == srcUsage.status
           && Arrays.equals(nomStatus, srcUsage.nomStatus);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(key, parentKey, originalNameKey, scientificName, publishedIn, parsedName, rank, status);
    result = 31 * result + Arrays.hashCode(nomStatus);
    return result;
  }

  @Override
  public String toString() {
    return rank + " " + scientificName + " [" + key + "]";
  }
}
