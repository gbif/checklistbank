package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.util.Objects;

/**
 * Simple usage representing the minimal nub usage info needed to match names.
 */
public class LookupUsage {
    private int key;
    private Integer parentKey;
    private String canonical;
    private String authorship;
    private Rank rank;
    private TaxonomicStatus status;
    private Kingdom kingdom;
    private boolean deleted;

    public LookupUsage() {
    }

    public LookupUsage(int key, Integer parentKey, String canonical, String authorship, Rank rank, TaxonomicStatus status, Kingdom kingdom, boolean deleted) {
        this.parentKey = parentKey;
        this.status = status;
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

    public TaxonomicStatus getStatus() {
        return status;
    }

    public void setStatus(TaxonomicStatus status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, parentKey, rank, kingdom, canonical, authorship, deleted, status);
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
                && Objects.equals(this.parentKey, other.parentKey)
                && Objects.equals(this.rank, other.rank)
                && Objects.equals(this.status, other.status)
                && Objects.equals(this.kingdom, other.kingdom)
                && Objects.equals(this.canonical, other.canonical)
                && Objects.equals(this.authorship, other.authorship)
                && Objects.equals(this.deleted, other.deleted);
    }
}
