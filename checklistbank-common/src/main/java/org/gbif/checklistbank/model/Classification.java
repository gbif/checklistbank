package org.gbif.checklistbank.model;

import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class Classification implements LinneanClassificationKeys, LinneanClassification {
    // for LinneanClassificationKeys
    private Integer kingdomKey;
    private Integer phylumKey;
    private Integer classKey;
    private Integer orderKey;
    private Integer familyKey;
    private Integer genusKey;
    private Integer subgenusKey;
    private Integer speciesKey;
    // for LinneanClassification
    private String kingdom;
    private String phylum;
    private String clazz;
    private String order;
    private String family;
    private String genus;
    private String subgenus;
    private String species;

    @Override
    public Integer getKingdomKey() {
        return kingdomKey;
    }

    @Override
    public void setKingdomKey(Integer kingdomKey) {
        this.kingdomKey = kingdomKey;
    }

    @Override
    public Integer getPhylumKey() {
        return phylumKey;
    }

    @Override
    public void setPhylumKey(Integer phylumKey) {
        this.phylumKey = phylumKey;
    }

    @Override
    public Integer getClassKey() {
        return classKey;
    }

    @Override
    public void setClassKey(Integer classKey) {
        this.classKey = classKey;
    }

    @Override
    public Integer getOrderKey() {
        return orderKey;
    }

    @Override
    public void setOrderKey(Integer orderKey) {
        this.orderKey = orderKey;
    }

    @Override
    public Integer getFamilyKey() {
        return familyKey;
    }

    @Override
    public void setFamilyKey(Integer familyKey) {
        this.familyKey = familyKey;
    }

    @Override
    public Integer getGenusKey() {
        return genusKey;
    }

    @Override
    public void setGenusKey(Integer genusKey) {
        this.genusKey = genusKey;
    }

    @Override
    public Integer getSubgenusKey() {
        return subgenusKey;
    }

    @Override
    public void setSubgenusKey(Integer subgenusKey) {
        this.subgenusKey = subgenusKey;
    }

    @Override
    public Integer getSpeciesKey() {
        return speciesKey;
    }

    @Override
    public void setSpeciesKey(Integer speciesKey) {
        this.speciesKey = speciesKey;
    }

    @Nullable
    @Override
    public String getClazz() {
        return clazz;
    }

    @Override
    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    @Nullable
    @Override
    public String getFamily() {
        return family;
    }

    @Override
    public void setFamily(String family) {
        this.family = family;
    }

    @Nullable
    @Override
    public String getGenus() {
        return genus;
    }

    @Override
    public void setGenus(String genus) {
        this.genus = genus;
    }

    @Nullable
    @Override
    public String getKingdom() {
        return kingdom;
    }

    @Override
    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }

    @Nullable
    @Override
    public String getOrder() {
        return order;
    }

    @Override
    public void setOrder(String order) {
        this.order = order;
    }

    @Nullable
    @Override
    public String getPhylum() {
        return phylum;
    }

    @Override
    public void setPhylum(String phylum) {
        this.phylum = phylum;
    }

    @Nullable
    @Override
    public String getSpecies() {
        return species;
    }

    @Override
    public void setSpecies(String species) {
        this.species = species;
    }

    @Nullable
    @Override
    public String getSubgenus() {
        return subgenus;
    }

    @Override
    public void setSubgenus(String subgenus) {
        this.subgenus = subgenus;
    }

    @Override
    public Integer getHigherRankKey(Rank rank) {
        return ClassificationUtils.getHigherRankKey(this, rank);
    }

    @Nullable
    @Override
    public String getHigherRank(Rank rank) {
        return ClassificationUtils.getHigherRank(this, rank);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                        kingdomKey,
                        phylumKey,
                        classKey,
                        orderKey,
                        familyKey,
                        genusKey,
                        subgenusKey,
                        speciesKey,
                        kingdom,
                        phylum,
                        clazz,
                        order,
                        family,
                        genus,
                        subgenus,
                        species);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Classification other = (Classification) obj;
        return Objects.equal(this.kingdomKey, other.kingdomKey)
                && Objects.equal(this.phylumKey, other.phylumKey)
                && Objects.equal(this.classKey, other.classKey)
                && Objects.equal(this.orderKey, other.orderKey)
                && Objects.equal(this.familyKey, other.familyKey)
                && Objects.equal(this.genusKey, other.genusKey)
                && Objects.equal(this.subgenusKey, other.subgenusKey)
                && Objects.equal(this.speciesKey, other.speciesKey)
                && Objects.equal(this.kingdom, other.kingdom)
                && Objects.equal(this.phylum, other.phylum)
                && Objects.equal(this.clazz, other.clazz)
                && Objects.equal(this.order, other.order)
                && Objects.equal(this.family, other.family)
                && Objects.equal(this.genus, other.genus)
                && Objects.equal(this.subgenus, other.subgenus)
                && Objects.equal(this.species, other.species);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("kingdomKey", kingdomKey)
                .add("phylumKey", phylumKey)
                .add("classKey", classKey)
                .add("orderKey", orderKey)
                .add("familyKey", familyKey)
                .add("genusKey", genusKey)
                .add("subgenusKey", subgenusKey)
                .add("speciesKey", speciesKey)
                .add("kingdom", kingdom)
                .add("phylum", phylum)
                .add("class", clazz)
                .add("order", order)
                .add("family", family)
                .add("genus", genus)
                .add("subgenus", subgenus)
                .add("species", species)
                .toString();
    }
}
