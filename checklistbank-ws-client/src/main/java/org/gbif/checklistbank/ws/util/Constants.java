package org.gbif.checklistbank.ws.util;

/**
 * Common variables shared by several classes.
 */
public class Constants {

  // used in URL paths
  public static final String DATASET_METRICS_PATH = "dataset_metrics";
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
  public static final String VERNACULAR_NAME_PATH = "vernacular_name";
  public static final String VERNACULAR_NAMES_PATH = "vernacular_names";
  public static final String DESCRIPTION_PATH = "description";
  public static final String DESCRIPTIONS_PATH = "descriptions";
  public static final String DISTRIBUTION_PATH = "distribution";
  public static final String DISTRIBUTIONS_PATH = "distributions";
  public static final String IDENTIFIER_PATH = "identifier";
  public static final String IDENTIFIERS_PATH = "identifiers";
  public static final String IMAGE_PATH = "image";
  public static final String IMAGES_PATH = "images";
  public static final String REFERENCE_PATH = "reference";
  public static final String REFERENCES_PATH = "references";
  public static final String SPECIES_PROFILE_PATH = "species_profile";
  public static final String SPECIES_PROFILES_PATH = "species_profiles";
  public static final String TYPE_SPECIMEN_PATH = "type_specimen";
  public static final String TYPE_SPECIMENS_PATH = "type_specimens";
  // Query parameters
  public static final String NUB_KEY = "nubKey";
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
