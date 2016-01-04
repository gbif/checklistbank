package org.gbif.checklistbank.authorship;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;


/**
 * As we often see missing brackets from author names we must code defensively and allow several original names
 * in the data for a single epithet.
 */
public class BasionymGroup<T> {
  private static final Joiner joiner = Joiner.on("; ").skipNulls();
  private String epithet;
  private String authorship;
  private String year;
  private List<T> basionyms = Lists.newArrayList();
  private List<T> recombinations = Lists.newArrayList();

  public BasionymGroup() {
  }

  public T getBasionym() {
    return basionyms.isEmpty() ? null : basionyms.get(0);
  }

  public List<T> getBasionyms() {
    return basionyms;
  }

  public void setBasionyms(List<T> basionyms) {
    this.basionyms = basionyms;
  }

  public void addBasionym(T basionym) {
    this.basionyms.add(basionym);
  }

  public List<T> getRecombinations() {
    return recombinations;
  }

  public boolean hasBasionym() {
    return !basionyms.isEmpty();
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
    return Objects.hash(basionyms, recombinations);
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
    return Objects.equals(this.basionyms, other.basionyms)
        && Objects.equals(this.recombinations, other.recombinations);
  }

  @Override
  public String toString() {
    return "BasionymGroup{" + epithet + ' ' + authorship + ' ' + year + " | " +
        basionyms + ": " + joiner.join(recombinations) + '}';
  }
}
