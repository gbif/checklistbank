package org.gbif.nub.lookup.straight;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;

import java.util.Objects;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.gbif.api.vocabulary.TaxonomicStatus;

/**
 * Simple usage representing the minimal nub usage info needed to match names.
 */
public class LookupUsage implements Comparable<LookupUsage> {
  private int key;
  @JsonIgnore
  /**
   * A map of parent usage key -> pro parte usage key
   * We want to keep the deleted flag information and integrate it into the pro parte key
   * by using the convention that negative keys are deleted keys!
   */
  private Int2IntMap proParteKeys; // parentKey -> pro parte usageKey
  private String canonical;
  private String authorship;
  private String year;
  private Rank rank;
  private TaxonomicStatus status;
  private Kingdom kingdom;
  private boolean deleted;

  public LookupUsage() {
  }

  public LookupUsage(int key, String canonical, String authorship, String year, Rank rank, TaxonomicStatus status, Kingdom kingdom, boolean deleted) {
    this(key, null, canonical, authorship, year, rank, status, kingdom, deleted);
  }

  public LookupUsage(int key, Int2IntMap proParteKeys, String canonical, String authorship, String year, Rank rank, TaxonomicStatus status, Kingdom kingdom, boolean deleted) {
    this.year = year;
    this.authorship = authorship;
    this.canonical = canonical;
    this.key = key;
    this.proParteKeys = proParteKeys;
    this.rank = rank;
    this.status = status;
    this.kingdom = kingdom;
    this.deleted = deleted;
  }

  public String getAuthorship() {
    return authorship;
  }

  public void setAuthorship(String authorship) {
    this.authorship = authorship;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public boolean hasAuthorship() {
    return authorship != null || year != null;
  }

  public String getCanonical() {
    return canonical;
  }

  public void setCanonical(String canonical) {
    this.canonical = canonical;
  }

  public int getKey() {
    return key;
  }

  public void setKey(int key) {
    this.key = key;
  }

  public Int2IntMap getProParteKeys() {
    return proParteKeys;
  }
  
  public TaxonomicStatus getStatus() {
    return status;
  }
  
  public void setStatus(TaxonomicStatus status) {
    this.status = status;
  }
  
  public Kingdom getKingdom() {
    return kingdom;
  }

  public void setKingdom(Kingdom kingdom) {
    this.kingdom = kingdom;
  }

  public Rank getRank() {
    return rank;
  }

  public void setRank(Rank rank) {
    this.rank = rank;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  /**
   * @return the key or the greatest pro parte key if it exceeds the key
   */
  public int getMaxKey() {
    if (proParteKeys == null) return key;
    else return Math.max(key, proParteKeys.values().stream().max(Integer::compare).orElse(-1));
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, proParteKeys, rank, status, kingdom, canonical, authorship, year, deleted);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final LookupUsage other = (LookupUsage) obj;
    return Objects.equals(this.key, other.key)
        && Objects.equals(this.proParteKeys, other.proParteKeys)
        && Objects.equals(this.rank, other.rank)
        && Objects.equals(this.year, other.year)
        && Objects.equals(this.status, other.status)
        && Objects.equals(this.kingdom, other.kingdom)
        && Objects.equals(this.canonical, other.canonical)
        && Objects.equals(this.authorship, other.authorship)
        && Objects.equals(this.deleted, other.deleted);
  }

  @Override
  public int compareTo(LookupUsage that) {
    return ComparisonChain.start()
        .compare(this.rank, that.rank, Ordering.natural().nullsLast())
        .compare(this.canonical, that.canonical, Ordering.natural().nullsLast())
        .compare(this.authorship, that.authorship, Ordering.natural().nullsLast())
        .compare(this.key, that.key, Ordering.natural().nullsLast())
        .result();
  }

  @Override
  public String toString() {
    return String.format("%s %s [%s%s]", canonical, authorship, key, proParteKeys!=null && !proParteKeys.isEmpty() ? "/pp"+proParteKeys.size():"");
  }
}
