package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;

import java.util.Objects;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * Simple usage representing the minimal nub usage info needed to match names.
 */
public class LookupUsage implements Comparable<LookupUsage> {
    private int key;
    private String canonical;
    private String authorship;
    private String year;
    private Rank rank;
    private Kingdom kingdom;
    private boolean deleted;

    public LookupUsage() {
    }

    public LookupUsage(int key, String canonical) {
    }

    public LookupUsage(int key, String canonical, String authorship, String year, Rank rank, Kingdom kingdom, boolean deleted) {
        this.year = year;
        this.authorship = authorship;
        this.canonical = canonical;
        this.key = key;
        this.kingdom = kingdom;
        this.rank = rank;
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

    @Override
    public int hashCode() {
        return Objects.hash(key, rank, kingdom, canonical, authorship, year, deleted);
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
                && Objects.equals(this.rank, other.rank)
                && Objects.equals(this.year, other.year)
                && Objects.equals(this.kingdom, other.kingdom)
                && Objects.equals(this.canonical, other.canonical)
                && Objects.equals(this.authorship, other.authorship)
                && Objects.equals(this.deleted, other.deleted);
    }

  @Override
  public int compareTo(LookupUsage that) {
    return ComparisonChain.start()
        .compare(this.rank, that.rank, Ordering.natural().nullsLast())
        .compare(this.kingdom, that.kingdom, Ordering.natural().nullsLast())
        .compare(this.canonical, that.canonical, Ordering.natural().nullsLast())
        .result();
  }
}
