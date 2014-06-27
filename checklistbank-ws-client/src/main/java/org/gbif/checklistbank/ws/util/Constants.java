package org.gbif.checklistbank.ws.util;

/**
 * Common variables shared by several classes.
 */
public class Constants {

  // used in URL paths
  public static final String DATASET_PATH = "dataset";
  public static final String SPECIES_PATH = "species";
  public static final String CHILDREN_PATH = "children";
  public static final String PARENTS_PATH = "parents";
  public static final String RELATED_PATH = "related";
  public static final String SYNONYMS_PATH = "synonyms";
  public static final String ROOT_USAGES_PATH = "root";
  public static final String METRICS_PATH = "metrics";
  public static final String PARSED_NAME_PATH = "name";
  public static final String VERBATIM_PATH = "verbatim";
  // used in sub-resource URL paths
  public static final String VERNACULAR_NAMES_PATH = "vernacularNames";
  public static final String DESCRIPTION_PATH = "description";
  public static final String DESCRIPTIONS_PATH = "descriptions";
  public static final String DISTRIBUTIONS_PATH = "distributions";
  public static final String MEDIA_PATH = "media";
  public static final String REFERENCES_PATH = "references";
  public static final String SPECIES_PROFILES_PATH = "speciesProfiles";
  public static final String TYPE_SPECIMENS_PATH = "typeSpecimens";
  // Query parameters
  public static final String TAXON_KEY = "taxonKey";
  public static final String DATASET_KEY = "datasetKey";
  public static final String RANK = "rank";
  public static final String SOURCE_ID = "sourceId";
  public static final String CANONICAL_NAME = "name";

  /**
   * Private constructor.
   */
  private Constants() {
    throw new UnsupportedOperationException("Can't initialize class");
  }
}
