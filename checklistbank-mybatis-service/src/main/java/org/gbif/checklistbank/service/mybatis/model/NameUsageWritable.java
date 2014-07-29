package org.gbif.checklistbank.service.mybatis.model;

import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.net.URI;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;

/**
 *
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
  private Date modified;
  private int numDescendants;

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

  public Integer getKingdomKey() {
    return kingdomKey;
  }

  public void setKingdomKey(Integer kingdomKey) {
    this.kingdomKey = kingdomKey;
  }

  public Integer getPhylumKey() {
    return phylumKey;
  }

  public void setPhylumKey(Integer phylumKey) {
    this.phylumKey = phylumKey;
  }

  public Integer getClassKey() {
    return classKey;
  }

  public void setClassKey(Integer classKey) {
    this.classKey = classKey;
  }

  public Integer getOrderKey() {
    return orderKey;
  }

  public void setOrderKey(Integer orderKey) {
    this.orderKey = orderKey;
  }

  public Integer getFamilyKey() {
    return familyKey;
  }

  public void setFamilyKey(Integer familyKey) {
    this.familyKey = familyKey;
  }

  public Integer getGenusKey() {
    return genusKey;
  }

  public void setGenusKey(Integer genusKey) {
    this.genusKey = genusKey;
  }

  public Integer getSubgenusKey() {
    return subgenusKey;
  }

  public void setSubgenusKey(Integer subgenusKey) {
    this.subgenusKey = subgenusKey;
  }

  @Nullable
  @Override
  public Integer getHigherRankKey(Rank rank) {
    return ClassificationUtils.getHigherRankKey(this, rank);
  }

  public Integer getSpeciesKey() {
    return speciesKey;
  }

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
}
