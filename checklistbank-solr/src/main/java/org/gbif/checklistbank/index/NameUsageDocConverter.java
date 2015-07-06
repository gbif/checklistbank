package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.vocabulary.Habitat;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.checklistbank.index.model.NameUsageSolrSearchResult;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.common.parsers.HabitatParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.common.search.util.AnnotationUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Threadsafe class that transforms a {@link NameUsage} object into {@link SolrInputDocument}.
 */
public class NameUsageDocConverter {

  private static final String CONCAT = " # ";
  private static final Pattern LANG_SPLIT = Pattern.compile("^([a-zA-Z]*)" + CONCAT + "(.*)$");

  /**
   * Holds a map of {@link NameUsage} properties (Java fields) to Solr fields name.
   */
  private final BiMap<String, String> fieldPropertyMap;

  /**
   * Logger for the {@link NameUsageDocConverter} class
   */
  protected static Logger log = LoggerFactory.getLogger(NameUsageDocConverter.class);

  public NameUsageDocConverter() {
    // initializes the map of Solr fields to Java fields
    fieldPropertyMap = AnnotationUtils.initFieldsPropertiesMap(NameUsageSolrSearchResult.class);
  }


  public static Description deserializeDescription(String description) {
    Description d = new Description();
    d.setDescription(description);
    return d;
  }

  public static VernacularName deserializeVernacularName(String vernacularName) {
    Matcher m = LANG_SPLIT.matcher(vernacularName);
    VernacularName vn = new VernacularName();
    if (m.find()) {
      vn.setLanguage(Language.fromIsoCode(m.group(1)));
      vn.setVernacularName(m.group(2));
    } else {
      vn.setVernacularName(vernacularName);
    }
    return vn;
  }

  public static String serializeDescription(Description description) {
    return stripHtml(description.getDescription());
  }

  private static String stripHtml(String html) {
    if (!Strings.isNullOrEmpty(html)) {
      try {
        return Jsoup.parse(html).text();
      } catch (RuntimeException e) {
        log.error("Failed to read description input");
      }
    }
    return null;
  }

  public static String serializeVernacularName(VernacularName vernacularName) {
    return vernacularName.getLanguage().getIso2LetterCode() + CONCAT + vernacularName.getVernacularName();
  }

  /**
   * Takes a Generic object and transform it into a {@link SolrInputDocument}.
   *
   * @param usage container to be transformed into a {@link SolrInputDocument}.
   *              vernacular names, descriptions, species profiles and distributions are used, so populate them!
   *
   * @return a {@link SolrInputDocument} using the Object parameter.
   */
  public SolrInputDocument toObject(NameUsage usage, List<Integer> parents, @Nullable UsageExtensions extensions) {
    try {
      SolrInputDocument doc = new SolrInputDocument();
      // Uses the pre-initialized field-property map to find the corresponding Solr field of a Java field.
      for (String field : fieldPropertyMap.keySet()) {
        String property = fieldPropertyMap.get(field);
        // Complex properties and Enum types properties are handled by utility methods.
        if (!property.endsWith("Serialized")
            && !property.equals("extinct")
            && !property.equals("habitatAsInts")
            && !property.equals("nomenclaturalStatusAsInts")
            && !property.equals("taxonomicStatus") && !property.equals("rank")
            && !property.equals("nameType")) {
          doc.addField(field, BeanUtils.getProperty(usage, property));
        }
      }
      // higher taxa
      addHigherTaxonKeys(parents, doc);
      // enums
      addIssues(usage, doc);
      addNomenclaturalStatus(usage, doc);
      addTaxonomicStatus(usage, doc);
      addRank(usage, doc);
      addNameType(usage, doc);
      // extract extension infos
      if (extensions != null) {
        addVernacularNames(doc, extensions);
        addDescriptions(doc, extensions);
        addDistributionsAndThreatStatus(doc, extensions);
        addSpeciesProfiles(doc, extensions);
      }
      return doc;

    } catch (Exception e) {
      log.error("Error converting usage {} to solr document: {}", usage.getKey(), e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private void addIssues(NameUsage nameUsage, SolrInputDocument doc) {
    try {
      if (!nameUsage.getIssues().isEmpty()) {
        for (NameUsageIssue issue : nameUsage.getIssues()) {
          // Uses the converter to get the key and name values
          doc.addField("issues", issue.ordinal());
        }
      }
    } catch (Exception e) {
      log.error("Error converting issues for usage {}", nameUsage.getKey(), e);
    }
  }

  /**
   * Utility method that iterates over all the {@link Description} objects of a {@link NameUsage}.
   *
   * @param doc to be modified by adding the description fields
   */
  private void addDescriptions(SolrInputDocument doc, UsageExtensions ext) {
    if (ext.descriptions == null) {
      return;
    }
    for (Description description : ext.descriptions) {
      doc.addField("description", serializeDescription(description));
    }
  }

  /**
   * Utility method that iterates over all the {@link Distribution} objects of a {@link NameUsage}.
   *
   * @param doc to be modified by adding the distributions fields
   */
  private void addDistributionsAndThreatStatus(SolrInputDocument doc, UsageExtensions ext) {
    if (ext.distributions == null) {
      return;
    }
    for (Distribution distribution : ext.distributions) {
      if (distribution.getThreatStatus() != null) {
        doc.addField("threat_status_key", distribution.getThreatStatus().ordinal());
      }
    }
  }

  /**
   * Adds the multivalued field higher_taxon_key field.
   * @param parents
   * @param doc to be modified by adding the higher taxon fields.
   */
  private void addHigherTaxonKeys(List<Integer> parents, SolrInputDocument doc) {
    if (parents != null) {
      for (Integer key : parents) {
        doc.addField("higher_taxon_key", key);
      }
    }
  }

  /**
   * Adds the name type field to the Solr document.
   *
   * @param nameUsage         a existing {@link NameUsage}.
   * @param doc to be modified by adding the name type fields.
   */
  private void addNameType(NameUsage nameUsage, SolrInputDocument doc) {
    if (nameUsage.getNameType() != null) {
      doc.addField("name_type", nameUsage.getNameType().ordinal());
    }
  }

  /**
   * Adds the nomenclatural status fields to the Solr document.
   */
  private void addNomenclaturalStatus(NameUsage nameUsage, SolrInputDocument doc) {
    if (nameUsage.getNomenclaturalStatus() != null) {
      for (NomenclaturalStatus ns : nameUsage.getNomenclaturalStatus()) {
        doc.addField("nomenclatural_status_key", ns.ordinal());
      }
    }
  }

  /**
   * Adds the rank fields to the Solr document.
   */
  private void addRank(NameUsage nameUsage, SolrInputDocument doc) {
    if (nameUsage.getRank() != null) {
      doc.addField("rank_key", nameUsage.getRank().ordinal());
    }
  }

  /**
   * Utility method that iterates over all the {@link SpeciesProfile} objects of a {@link NameUsage}.
   *
   * @param doc to be modified by adding the species profiles(extinct & marine) fields
   */
  private void addSpeciesProfiles(SolrInputDocument doc, UsageExtensions ext) {
    if (ext.speciesProfiles == null) {
      return;
    }

    // use container logic to build a single value
    NameUsageContainer usage = new NameUsageContainer();
    usage.setSpeciesProfiles(ext.speciesProfiles);

    doc.addField("extinct", usage.isExtinct());
    // derive habitat values from boolean flags
    addHabitat(doc, usage.isFreshwater(), Habitat.FRESHWATER);
    addHabitat(doc, usage.isMarine(), Habitat.MARINE);
    addHabitat(doc, usage.isTerrestrial(), Habitat.TERRESTRIAL);
    // see if we can make use of uncontrolled habitat values with the parser, CoL uses it a lot!
    HabitatParser hp = HabitatParser.getInstance();
    for (String habitat : usage.getHabitats()) {
      ParseResult<Habitat> result = hp.parse(habitat);
      if (result.isSuccessful()) {
        addHabitat(doc, result.getPayload());
      }
    }
  }

  private void addHabitat(SolrInputDocument doc, Boolean add, Habitat habitat) {
    if (add != null && add) {
      addHabitat(doc, habitat);
    }
  }

  private void addHabitat(SolrInputDocument doc, Habitat habitat) {
    if (habitat != null) {
      doc.addField("habitat_key", habitat.ordinal());
    }
  }

  /**
   * Adds the taxonomic status fields to the Solr document.
   */
  private void addTaxonomicStatus(NameUsage nameUsage, SolrInputDocument doc) {
    if (nameUsage.getTaxonomicStatus() != null) {
      doc.addField("taxonomic_status_key", nameUsage.getTaxonomicStatus().ordinal());
    }
  }

  /**
   * Utility method that iterates over all the {@link VernacularName} objects of a {@link NameUsage}.
   *
   * @param doc to be modified by adding the vernacular name fields
   */
  private void addVernacularNames(SolrInputDocument doc, UsageExtensions ext) {
    if (ext.vernacularNames == null) {
      return;
    }
    for (VernacularName vernacularName : ext.vernacularNames) {
      doc.addField("vernacular_name", vernacularName.getVernacularName());
      if (vernacularName.getLanguage() != null) {
        doc.addField("vernacular_lang", vernacularName.getLanguage().getIso2LetterCode());
        doc.addField("vernacular_name_lang", serializeVernacularName(vernacularName));
      }
    }
  }

}
