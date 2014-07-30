/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.index.model;


import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.common.search.model.Key;
import org.gbif.common.search.model.SuggestMapping;

import org.apache.solr.client.solrj.beans.Field;

/**
 * This class contains the annotations required by {@link org.gbif.api.service.common.SuggestService} and the Solr
 * result/object mapping.
 * This class holds the Solr response mapping and the annotation required by the suggest service.
 */
@SuggestMapping(field = "canonical_name_as", phraseQueryField = "canonical_name_as_phrase")
public class NameUsageSolrSuggestResult extends NameUsageSuggestResult {

  @Override
  @Field("canonical_name")
  public void setCanonicalName(String canonicalName) {
    super.setCanonicalName(canonicalName);
  }

  @Override
  @Field("class_key")
  public void setClassKey(Integer classKey) {
    super.setClassKey(classKey);
  }

  @Override
  @Field("class")
  public void setClazz(String clazz) {
    super.setClazz(clazz);
  }

  @Override
  @Field("family")
  public void setFamily(String family) {
    super.setFamily(family);
  }

  @Override
  @Field("family_key")
  public void setFamilyKey(Integer familyKey) {
    super.setFamilyKey(familyKey);
  }

  @Override
  @Field("genus")
  public void setGenus(String genus) {
    super.setGenus(genus);
  }

  @Override
  @Field("genus_key")
  public void setGenusKey(Integer genusKey) {
    super.setGenusKey(genusKey);
  }

  @Override
  @Field("key")
  @Key
  public void setKey(Integer key) {
    super.setKey(key);
  }

  @Override
  @Field("kingdom")
  public void setKingdom(String kingdom) {
    super.setKingdom(kingdom);
  }

  @Override
  @Field("kingdom_key")
  public void setKingdomKey(Integer kingdomKey) {
    super.setKingdomKey(kingdomKey);
  }

  @Override
  @Field("order")
  public void setOrder(String order) {
    super.setOrder(order);
  }

  @Override
  @Field("order_key")
  public void setOrderKey(Integer orderKey) {
    super.setOrderKey(orderKey);
  }

  @Override
  @Field("phylum")
  public void setPhylum(String phylum) {
    super.setPhylum(phylum);
  }

  @Override
  @Field("phylum_key")
  public void setPhylumKey(Integer phylumKey) {
    super.setPhylumKey(phylumKey);
  }

  @Override
  @Field("species")
  public void setSpecies(String species) {
    super.setSpecies(species);
  }

  @Override
  @Field("species_key")
  public void setSpeciesKey(Integer speciesKey) {
    super.setSpeciesKey(speciesKey);
  }

  @Override
  @Field("subgenus")
  public void setSubgenus(String subgenus) {
    super.setSubgenus(subgenus);
  }

  @Override
  @Field("subgenus_key")
  public void setSubgenusKey(Integer subgenusKey) {
    super.setSubgenusKey(subgenusKey);
  }

  @Override
  @Field("nub_key")
  public void setNubKey(Integer nubKey) {
    super.setNubKey(nubKey);
  }

  @Override
  @Field("parent")
  public void setParent(String parent) {
    super.setParent(parent);
  }

  @Override
  @Field("parent_key")
  public void setParentKey(Integer parentKey) {
    super.setParentKey(parentKey);
  }

  @Field("rank_key")
  public void setRank(Integer rank) {
    if (rank != null) {
      super.setRank(Rank.values()[rank]);
    } else {
      super.setRank(null);
    }
  }

  @Override
  public String getHigherRank(Rank rank) {
    return ClassificationUtils.getHigherRank(this, rank);
  }

  @Override
  public Integer getHigherRankKey(Rank rank) {
    return ClassificationUtils.getHigherRankKey(this, rank);
  }

}
