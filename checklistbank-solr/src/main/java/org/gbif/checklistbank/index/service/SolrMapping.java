package org.gbif.checklistbank.index.service;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import com.google.common.collect.ImmutableMap;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;

import java.util.Map;
import java.util.UUID;

/**
 *
 */
public class SolrMapping {

  public static final BiMap<NameUsageSearchParameter, String> FACET_MAPPING = EnumHashBiMap.create(NameUsageSearchParameter.class);
  static {
    FACET_MAPPING.put(NameUsageSearchParameter.DATASET_KEY, "dataset_key");
    FACET_MAPPING.put(NameUsageSearchParameter.CONSTITUENT_KEY, "constituent_key");
    FACET_MAPPING.put(NameUsageSearchParameter.HIGHERTAXON_KEY, "higher_taxon_key");
    FACET_MAPPING.put(NameUsageSearchParameter.ORIGIN, "origin_key");
    FACET_MAPPING.put(NameUsageSearchParameter.STATUS, "taxonomic_status_key");
    FACET_MAPPING.put(NameUsageSearchParameter.RANK, "rank_key");
    FACET_MAPPING.put(NameUsageSearchParameter.THREAT, "threat_status_key");
    FACET_MAPPING.put(NameUsageSearchParameter.IS_EXTINCT, "extinct");
    FACET_MAPPING.put(NameUsageSearchParameter.NOMENCLATURAL_STATUS, "nomenclatural_status_key");
    FACET_MAPPING.put(NameUsageSearchParameter.NAME_TYPE, "name_type");
    FACET_MAPPING.put(NameUsageSearchParameter.HABITAT, "habitat_key");
    FACET_MAPPING.put(NameUsageSearchParameter.ISSUE, "issues");
  }

  public static final Map<NameUsageSearchRequest.QueryField, String> HIGHLIGHT_FIELDS = ImmutableMap.of(
      NameUsageSearchRequest.QueryField.DESCRIPTION, "description",
      NameUsageSearchRequest.QueryField.VERNACULAR, "vernacular_name"
  );

  /**
   * Converts a solr string value into the proper java instance.
   */
  public static String interpretSolrValue(NameUsageSearchParameter param, String value) {
    if (Strings.isNullOrEmpty(value)) return null;

    if (Enum.class.isAssignableFrom(param.type())) {
      Class<Enum<?>> vocab = (Class<Enum<?>>) param.type();
      return vocab.getEnumConstants()[Integer.valueOf(value)].name();

    } else if (UUID.class.isAssignableFrom(param.type())) {
      return UUID.fromString(value).toString();

    } else if (Double.class.isAssignableFrom(param.type())) {
      return String.valueOf(Double.parseDouble(value));

    } else if (Integer.class.isAssignableFrom(param.type())) {
      return String.valueOf(Integer.parseInt(value));

    } else if (Boolean.class.isAssignableFrom(param.type())) {
      return String.valueOf(Boolean.parseBoolean(value));

    }
    return value;
  }

}
