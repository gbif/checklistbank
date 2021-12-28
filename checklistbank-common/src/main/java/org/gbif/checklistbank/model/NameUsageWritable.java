/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.model;

import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.net.URI;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

/**
 * Internal persistence class that holds writable values for a name_usage record.
 */
public class NameUsageWritable implements LinneanClassificationKeys {

    private Integer key;
    private UUID datasetKey;
    private UUID constituentKey;
    private Integer nameKey;
    private Rank rank;
    // watch out, this is the parent_key as in the db and is the acceptedKey for synonyms, but parentKey for accepted taxa!!!
    private Integer parentKey;
    private boolean isSynonym;
    private Integer proParteKey;
    private TaxonomicStatus taxonomicStatus;
    private Set<NomenclaturalStatus> nomenclaturalStatus = Sets.newHashSet();
    private Integer basionymKey;
    // for LinneanClassificationKeys
    private Integer kingdomKey;
    private Integer phylumKey;
    private Integer classKey;
    private Integer orderKey;
    private Integer familyKey;
    private Integer genusKey;
    private Integer subgenusKey;
    private Integer speciesKey;
    private Integer publishedInKey;
    private Integer accordingToKey;
    private Origin origin;
    private String remarks;
    private URI references;
    private String taxonID;
    private Integer sourceTaxonKey;
    private Date modified;
    private int numDescendants;
    private Set<NameUsageIssue> issues = Sets.newHashSet();

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public UUID getDatasetKey() {
        return datasetKey;
    }

    public void setDatasetKey(UUID datasetKey) {
        this.datasetKey = datasetKey;
    }

    public UUID getConstituentKey() {
        return constituentKey;
    }

    public void setConstituentKey(UUID constituentKey) {
        this.constituentKey = constituentKey;
    }

    public Integer getNameKey() {
        return nameKey;
    }

    public void setNameKey(Integer nameKey) {
        this.nameKey = nameKey;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public Integer getParentKey() {
        return parentKey;
    }

    public void setParentKey(Integer parentKey) {
        this.parentKey = parentKey;
    }

    public boolean isSynonym() {
        return isSynonym;
    }

    public void setSynonym(boolean isSynonym) {
        this.isSynonym = isSynonym;
    }

    public Integer getProParteKey() {
        return proParteKey;
    }

    public void setProParteKey(Integer proParteKey) {
        this.proParteKey = proParteKey;
    }

    public TaxonomicStatus getTaxonomicStatus() {
        return taxonomicStatus;
    }

    public void setTaxonomicStatus(TaxonomicStatus taxonomicStatus) {
        this.taxonomicStatus = taxonomicStatus;
    }

    public Set<NomenclaturalStatus> getNomenclaturalStatus() {
        return nomenclaturalStatus;
    }

    public void setNomenclaturalStatus(Set<NomenclaturalStatus> nomenclaturalStatus) {
        this.nomenclaturalStatus = nomenclaturalStatus;
    }

    public Integer getBasionymKey() {
        return basionymKey;
    }

    public void setBasionymKey(Integer basionymKey) {
        this.basionymKey = basionymKey;
    }

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

    @Nullable
    @Override
    public Integer getHigherRankKey(Rank rank) {
        return ClassificationUtils.getHigherRankKey(this, rank);
    }

    @Override
    public Integer getSpeciesKey() {
        return speciesKey;
    }

    @Override
    public void setSpeciesKey(Integer speciesKey) {
        this.speciesKey = speciesKey;
    }

    public Integer getPublishedInKey() {
        return publishedInKey;
    }

    public void setPublishedInKey(Integer publishedInKey) {
        this.publishedInKey = publishedInKey;
    }

    public Integer getAccordingToKey() {
        return accordingToKey;
    }

    public void setAccordingToKey(Integer accordingToKey) {
        this.accordingToKey = accordingToKey;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public URI getReferences() {
        return references;
    }

    public void setReferences(URI references) {
        this.references = references;
    }

    public String getTaxonID() {
        return taxonID;
    }

    public void setTaxonID(String taxonID) {
        this.taxonID = taxonID;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public int getNumDescendants() {
        return numDescendants;
    }

    public void setNumDescendants(int numDescendants) {
        this.numDescendants = numDescendants;
    }

    public Set<NameUsageIssue> getIssues() {
        return issues;
    }

    public void setIssues(Set<NameUsageIssue> issues) {
        this.issues = issues;
    }

    public Integer getSourceTaxonKey() {
        return sourceTaxonKey;
    }

    public void setSourceTaxonKey(Integer sourceTaxonKey) {
        this.sourceTaxonKey = sourceTaxonKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NameUsageWritable) {
            NameUsageWritable that = (NameUsageWritable) obj;
            return Objects.equal(this.key, that.key) && Objects.equal(this.datasetKey, that.datasetKey) && Objects
                    .equal(this.constituentKey, that.constituentKey) && Objects.equal(this.nameKey, that.nameKey) && Objects
                    .equal(this.rank, that.rank) && Objects.equal(this.parentKey, that.parentKey) && Objects
                    .equal(this.isSynonym, that.isSynonym) && Objects.equal(this.proParteKey, that.proParteKey) && Objects
                    .equal(this.taxonomicStatus, that.taxonomicStatus) && Objects
                    .equal(this.nomenclaturalStatus, that.nomenclaturalStatus) && Objects
                    .equal(this.basionymKey, that.basionymKey) && Objects.equal(this.kingdomKey, that.kingdomKey) && Objects
                    .equal(this.phylumKey, that.phylumKey) && Objects.equal(this.classKey, that.classKey) && Objects
                    .equal(this.orderKey, that.orderKey) && Objects.equal(this.familyKey, that.familyKey) && Objects
                    .equal(this.genusKey, that.genusKey) && Objects.equal(this.subgenusKey, that.subgenusKey) && Objects
                    .equal(this.speciesKey, that.speciesKey) && Objects.equal(this.publishedInKey, that.publishedInKey)
                    && Objects.equal(this.accordingToKey, that.accordingToKey) && Objects.equal(this.origin, that.origin)
                    && Objects.equal(this.remarks, that.remarks) && Objects.equal(this.references, that.references)
                    && Objects.equal(this.taxonID, that.taxonID)
                    && Objects.equal(this.sourceTaxonKey, that.sourceTaxonKey)
                    && Objects.equal(this.modified, that.modified) && Objects
                    .equal(this.numDescendants, that.numDescendants) && Objects.equal(this.issues, that.issues);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects
                .hashCode(key, datasetKey, constituentKey, nameKey, rank, parentKey, isSynonym, proParteKey, taxonomicStatus,
                        nomenclaturalStatus, basionymKey, kingdomKey, phylumKey, classKey, orderKey, familyKey, genusKey, subgenusKey,
                        speciesKey, publishedInKey, accordingToKey, origin, remarks, references, taxonID, sourceTaxonKey, modified, numDescendants,
                        issues);
    }

}
