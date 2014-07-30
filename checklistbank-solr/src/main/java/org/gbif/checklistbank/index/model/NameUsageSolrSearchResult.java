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

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.common.search.model.FacetField;
import org.gbif.common.search.model.FacetField.Method;
import org.gbif.common.search.model.FullTextSearchField;
import org.gbif.common.search.model.HighlightableList;
import org.gbif.common.search.model.Key;
import org.gbif.common.search.model.SearchMapping;
import org.gbif.common.search.model.WildcardPadding;

import java.util.List;
import java.util.UUID;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.apache.solr.client.solrj.beans.Field;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * This class contains the annotations required by {@link org.gbif.api.service.common.SearchService} and the Solr
 * result/object mapping.
 * The intention of having a separate class containing mostly annotations is to avoid the usage of specific Solr
 * annotations in the model objects that are part of the API.
 * Scoring used in this class:
 * canonical_name:Abies^100
 * scientific_name:Abies^10
 * scientific_name:*Abies*^0.2
 * vernacular_name:Abies^25
 * vernacular_name:Abies^6
 * vernacular_name:*Abies*^0.2
 * kingdom:Abies
 * phylum:Abies
 * class:Abies
 * order:Abies
 * family:Abies^2
 * genus:Abies^2
 * subgenus:Abies^3
 * species:Abies^5
 * description:Abies^0.3
 */
@SearchMapping(
  facets = {
    @FacetField(name = "DATASET_KEY", field = "dataset_key", method = Method.ENUM),
    @FacetField(name = "HIGHERTAXON_KEY", field = "higher_taxon_nub_key"),
    @FacetField(name = "STATUS", field = "taxonomic_status_key", method = Method.ENUM),
    @FacetField(name = "RANK", field = "rank_key", method = Method.ENUM),
    @FacetField(name = "THREAT", field = "threat_status_key", method = Method.ENUM),
    @FacetField(name = "IS_EXTINCT", field = "extinct", method = Method.ENUM),
    @FacetField(name = "NOMENCLATURAL_STATUS", field = "nomenclatural_status_key", method = Method.ENUM),
    @FacetField(name = "NAME_TYPE", field = "name_type", method = Method.ENUM),
    @FacetField(name = "HABITAT", field = "marine", method = Method.ENUM)
  },
  fulltextFields = {
    @FullTextSearchField(field = "canonical_name", exactMatchScore = 100.0d,
      partialMatchScore = 1d, partialMatching = WildcardPadding.RIGHT),
    @FullTextSearchField(field = "vernacular_name", highlightField = "vernacular_name_lang", exactMatchScore = 10d,
      partialMatchScore = 0.75d, partialMatching = WildcardPadding.RIGHT),
    @FullTextSearchField(field = "scientific_name", exactMatchScore = 5d,
      partialMatchScore = 0.7d, partialMatching = WildcardPadding.RIGHT),
    @FullTextSearchField(field = "description", exactMatchScore = 0.2d,
      partialMatching = WildcardPadding.NONE),
    @FullTextSearchField(field = "species", exactMatchScore = 5d, partialMatching = WildcardPadding.NONE),
    @FullTextSearchField(field = "subgenus", exactMatchScore = 3d, partialMatching = WildcardPadding.NONE),
    @FullTextSearchField(field = "genus", exactMatchScore = 2d, partialMatching = WildcardPadding.NONE),
    @FullTextSearchField(field = "family", exactMatchScore = 2d, partialMatching = WildcardPadding.NONE),
    @FullTextSearchField(field = "order", partialMatching = WildcardPadding.NONE),
    @FullTextSearchField(field = "class", partialMatching = WildcardPadding.NONE),
    @FullTextSearchField(field = "phylum", partialMatching = WildcardPadding.NONE),
    @FullTextSearchField(field = "kingdom", partialMatching = WildcardPadding.NONE),
  })
public class NameUsageSolrSearchResult extends NameUsageSearchResult {

  private static Function<VernacularName, String> toNameLang = new Function<VernacularName, String>() {

    @Override
    public String apply(VernacularName obj) {
      String lang = obj.getLanguage() == null ? "" : obj.getLanguage().getIso2LetterCode();
      return Strings.nullToEmpty(obj.getVernacularName()) + lang;
    }
  };

  private static Ordering<VernacularName> byNameAndLanguage = Ordering.natural().onResultOf(toNameLang);

  @JsonIgnore
  public HighlightableList getDescriptionsSerialized() {
    return new HighlightableDescription(getDescriptions());
  }

  @JsonIgnore
  public HighlightableList getVernacularNamesSerialized() {
    return new HighlightableVernacularName(getVernacularNames());
  }

  @Override
  @Field("accepted")
  public void setAccepted(String accepted) {
    super.setAccepted(accepted);
  }

  @Override
  @Field("accepted_key")
  public void setAcceptedKey(Integer acceptedKey) {
    super.setAcceptedKey(acceptedKey);
  }

  @Override
  @Field("according_to")
  public void setAccordingTo(String accordingTo) {
    super.setAccordingTo(accordingTo);
  }

  @Override
  @Field("authorship")
  public void setAuthorship(String authorship) {
    super.setAuthorship(authorship);
  }

  @Override
  @Field("basionym")
  public void setBasionym(String basionym) {
    super.setBasionym(basionym);
  }

  @Override
  @Field("basionym_key")
  public void setBasionymKey(Integer basionymKey) {
    super.setBasionymKey(basionymKey);
  }

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

  @Field("dataset_key")
  public void setDatasetKey(String datasetKey) {
    super.setDatasetKey(UUID.fromString(datasetKey));
  }

  @Field("description")
  public void setDescriptionsSerialized(List<String> descriptions) {
    List<Description> descriptionList = Lists.newArrayList();
    for (String d : descriptions) {
      descriptionList.add(NameUsageDocConverter.deserializeDescription(d));
    }
    super.setDescriptions(descriptionList);
  }

  @Override
  @Field("extinct")
  public void setExtinct(Boolean extinct) {
    super.setExtinct(extinct);
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
  @Field("marine")
  public void setMarine(Boolean marine) {
    super.setMarine(marine);
  }

  @Field("name_type")
  public void setNameType(Integer nameType) {
    if (nameType != null) {
      super.setNameType(NameType.values()[nameType]);
    } else {
      super.setNameType(null);
    }
  }

  @Field("nomenclatural_status_key")
  public void setNomenclaturalStatusAsInts(List<Integer> nomenclaturalStatus) {
    List<NomenclaturalStatus> stati = Lists.newArrayList();
    if (nomenclaturalStatus != null) {
      for (Integer statusOrdinalIdx : nomenclaturalStatus) {
        stati.add(NomenclaturalStatus.values()[statusOrdinalIdx]);
      }
    }
    super.setNomenclaturalStatus(stati);
  }

  @Override
  @Field("nub_key")
  public void setNubKey(Integer nubKey) {
    super.setNubKey(nubKey);
  }

  @Override
  @Field("num_descendants")
  public void setNumDescendants(int numDescendants) {
    super.setNumDescendants(numDescendants);
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
  @Field("parent")
  public void setParent(String parent) {
    super.setParent(parent);
  }

  @Override
  @Field("parent_key")
  public void setParentKey(Integer parentKey) {
    super.setParentKey(parentKey);
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
  @Field("published_in")
  public void setPublishedIn(String publishedIn) {
    super.setPublishedIn(publishedIn);
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
  @Field("scientific_name")
  public void setScientificName(String scientificName) {
    super.setScientificName(scientificName);
  }


  @Override
  @Field("source_id")
  public void setTaxonID(String sourceId) {
    super.setTaxonID(sourceId);
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
  @Field("is_synonym")
  public void setSynonym(boolean isSynonym) {
    super.setSynonym(isSynonym);
  }

  @Field("taxonomic_status_key")
  public void setTaxonomicStatus(Integer taxonomicStatus) {
    if (taxonomicStatus != null) {
      super.setTaxonomicStatus(TaxonomicStatus.values()[taxonomicStatus]);
    } else {
      super.setTaxonomicStatus(null);
    }
  }

  @Field("vernacular_name_lang")
  /**
   * Populates the vernacular names from a solr response.
   * @param vernacularNamesLanguages the vernacularNamesLanguages to set
   */
  public void setVernacularNamesSerialized(List<String> vernacularNamesLanguages) {
    // deserialize them into vernacular names
    List<VernacularName> vnames = Lists.newArrayListWithCapacity(vernacularNamesLanguages.size());
    for (String v : vernacularNamesLanguages) {
      vnames.add(NameUsageDocConverter.deserializeVernacularName(v));
    }
    super.setVernacularNames(vnames);
  }

}
