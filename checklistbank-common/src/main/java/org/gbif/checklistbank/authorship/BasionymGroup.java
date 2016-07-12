package org.gbif.checklistbank.authorship;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;


/**
 * A group of recombined names and their original basionym with the epithet and original publication author.
 */
public class BasionymGroup<T> {
  private static final Joiner joiner = Joiner.on("; ").skipNulls();
  private String epithet;
  private String authorship;
  private String year;
  private T basionym;
  private List<T> recombinations = Lists.newArrayList();

  public BasionymGroup() {
  }

  public T getBasionym() {
    return basionym;
  }

  public void setBasionym(T basionym) {
    this.basionym = basionym;
  }

  public List<T> getRecombinations() {
    return recombinations;
  }

  public boolean hasBasionym() {
    return basionym != null;
  }

  public boolean hasRecombinations() {
    return !recombinations.isEmpty();
  }

  public String getAuthorship() {
    return authorship;
  }

  public String getEpithet() {
    return epithet;
  }

  public String getYear() {
    return year;
  }

  public void setName(String epithet, String authorship, String year) {
    this.epithet = epithet;
    this.authorship = authorship;
    this.year = year;
  }

  @Override
  public int hashCode() {
    return Objects.hash(basionym, recombinations, epithet, authorship, year);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final BasionymGroup other = (BasionymGroup) obj;
    return Objects.equals(this.basionym, other.basionym)
        && Objects.equals(this.recombinations, other.recombinations)
        && Objects.equals(this.epithet, other.epithet)
        && Objects.equals(this.authorship, other.authorship)
        && Objects.equals(this.year, other.year);
  }

  @Override
  public String toString() {
    return "BasionymGroup{" + epithet + ' ' + authorship + ' ' + year + " | " +
        basionym + ": " + joiner.join(recombinations) + '}';
  }
}
