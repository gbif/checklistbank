package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Habitat;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.api.vocabulary.ThreatStatus;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.common.parsers.HabitatParser;
import org.gbif.common.parsers.core.ParseResult;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Threadsafe class that transforms a {@link NameUsage} object into {@link SolrInputDocument} and vice versa.
 */
public class NameUsageDocConverter {

  private static final Logger LOG = LoggerFactory.getLogger(NameUsageDocConverter.class);
  private static final String CONCAT = " # ";
  private static final Pattern LANG_SPLIT = Pattern.compile("^([a-zA-Z]*)" + CONCAT + "(.*)$");

  /**
   * Takes a name usage instance and its associated information and transform it into a {@link SolrInputDocument}.
   *
   * @param usage container to be transformed into a {@link SolrInputDocument}.
   *              vernacular names, descriptions, species profiles and distributions are used, so populate them!
   *
   * @return a {@link SolrInputDocument} using the Object parameter.
   */
  public SolrInputDocument toDoc(NameUsage usage, List<Integer> parents, @Nullable UsageExtensions extensions) {
    try {
      SolrInputDocument doc = new SolrInputDocument();

      doc.addField("key", usage.getKey());
      doc.addField("name_key", usage.getNameKey());
      doc.addField("nub_key", usage.getNubKey());
      doc.addField("dataset_key", str(usage.getDatasetKey()));
      doc.addField("constituent_key", str(usage.getConstituentKey()));
      doc.addField("parent_key", usage.getParentKey());
      doc.addField("parent", usage.getParent());
      doc.addField("accepted_key", usage.getAcceptedKey());
      doc.addField("accepted", usage.getAccepted());
      doc.addField("basionym_key", usage.getBasionymKey());
      doc.addField("basionym", usage.getBasionym());
      doc.addField("scientific_name", usage.getScientificName());
      doc.addField("canonical_name", usage.getCanonicalName());
      doc.addField("name_type", ordinal(usage.getNameType()));
      doc.addField("authorship", usage.getAuthorship());
      doc.addField("taxonomic_status_key", ordinal(usage.getTaxonomicStatus()));
      if (usage.getNomenclaturalStatus() != null) {
        for (NomenclaturalStatus ns : usage.getNomenclaturalStatus()) {
          doc.addField("nomenclatural_status_key", ns.ordinal());
        }
      }
      doc.addField("rank_key", ordinal(usage.getRank()));
      doc.addField("published_in", usage.getPublishedIn());
      doc.addField("according_to", usage.getAccordingTo());
      doc.addField("num_descendants", usage.getNumDescendants());
      doc.addField("source_id", usage.getTaxonID());
      // classification fields
      addClassification(doc, usage);
      // higher_taxon_key
      addHigherTaxonKeys(parents, doc);
      // issues
      addIssues(usage, doc);

      // extract extension infos
      if (extensions != null) {
        // vernacular_name, vernacular_name_lang
        addVernacularNames(doc, extensions);
        // description
        addDescriptions(doc, extensions);
        // threat_status_key
        addDistributionsAndThreatStatus(doc, extensions);
        // habitat_key, extinct
        addSpeciesProfiles(doc, extensions);
      }
      return doc;

    } catch (Exception e) {
      LOG.error("Error converting usage {} to solr document: {}", usage.getKey(), e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public NameUsageSearchResult toSearchUsage(SolrDocument doc, boolean addExtensionData) {
    NameUsageSearchResult u = new NameUsageSearchResult();

    u.setKey((Integer)doc.getFieldValue("key"));
    u.setNameKey((Integer)doc.getFieldValue("name_key"));
    u.setNubKey((Integer)doc.getFieldValue("nub_key"));
    u.setDatasetKey(toUUID(doc.getFieldValue("dataset_key")));
    u.setConstituentKey(toUUID(doc.getFieldValue("constituent_key")));
    u.setParentKey((Integer)doc.getFieldValue("parent_key"));
    u.setParent((String)doc.getFieldValue("parent"));
    u.setAcceptedKey((Integer)doc.getFieldValue("accepted_key"));
    u.setAccepted((String)doc.getFieldValue("accepted"));
    u.setBasionymKey((Integer)doc.getFieldValue("basionym_key"));
    u.setBasionym((String)doc.getFieldValue("basionym"));
    u.setScientificName((String)doc.getFieldValue("scientific_name"));
    u.setCanonicalName((String)doc.getFieldValue("canonical_name"));
    u.setNameType(toEnum(doc, NameType.class, "name_type"));
    u.setAuthorship((String)doc.getFieldValue("authorship"));
    u.setTaxonomicStatus(toEnum(doc, TaxonomicStatus.class, "taxonomic_status_key"));
    addEnumList(NomenclaturalStatus.class, u.getNomenclaturalStatus(), doc, "nomenclatural_status_key");
    u.setRank(toEnum(doc, Rank.class, "rank_key"));
    u.setPublishedIn((String)doc.getFieldValue("published_in"));
    u.setAccordingTo((String)doc.getFieldValue("according_to"));

    addClassification(doc, u, u);

    u.setNumDescendants((Integer)doc.getFieldValue("num_descendants"));
    u.setTaxonID((String)doc.getFieldValue("source_id"));

    if (addExtensionData) {
      // habitat_key,extinct
      addEnumList(Habitat.class, u.getHabitats(), doc, "habitat_key");
      u.setExtinct((Boolean) doc.getFieldValue("extinct"));
      // threat_status_key
      addEnumList(ThreatStatus.class, u.getThreatStatuses(), doc, "threat_status_key");
      // vernacular_name, vernacular_name_lang
      addObjList(u.getVernacularNames(), doc, "vernacular_name_lang", new Function<Object, VernacularName>() {
        @Nullable
        @Override
        public VernacularName apply(@Nullable Object input) {
          return deserializeVernacularName((String)input);
        }
      });
      // description
      addObjList(u.getDescriptions(), doc, "description", new Function<Object, Description>() {
        @Nullable
        @Override
        public Description apply(@Nullable Object input) {
          return deserializeDescription((String)input);
        }
      });
    }

    return u;
  }

  private static <T extends Enum<?>> void addEnumList(Class<T> vocab, List<T> data, SolrDocument doc, String field) {
    if (doc.getFieldValues(field) != null) {
      for (Object val : doc.getFieldValues(field)) {
        data.add(vocab.getEnumConstants()[(Integer) val]);
      }
    }
  }

  private static <T> void addObjList(List<T> data, SolrDocument doc, String field, Function<Object, T> func) {
    if (doc.getFieldValues(field) != null) {
      for (Object val : doc.getFieldValues(field)) {
        data.add(func.apply(val));
      }
    }
  }

  public NameUsageSuggestResult toSuggestUsage(SolrDocument doc) {
    NameUsageSuggestResult u = new NameUsageSuggestResult();

    u.setKey((Integer)doc.getFieldValue("key"));
    u.setNameKey((Integer)doc.getFieldValue("name_key"));
    u.setNubKey((Integer)doc.getFieldValue("nub_key"));
    u.setParentKey((Integer)doc.getFieldValue("parent_key"));
    u.setParent((String)doc.getFieldValue("parent"));
    u.setScientificName((String)doc.getFieldValue("scientific_name"));
    u.setCanonicalName((String)doc.getFieldValue("canonical_name"));
    u.setStatus(toEnum(doc, TaxonomicStatus.class, "taxonomic_status_key"));
    u.setRank(toEnum(doc, Rank.class, "rank_key"));

    addClassification(doc, u, u);

    return u;
  }

  private void addClassification(SolrInputDocument doc, NameUsage usage) {
    for (Rank r : Rank.DWC_RANKS) {
      doc.addField(r.name().toLowerCase(), usage.getHigherRank(r));
      doc.addField(r.name().toLowerCase()+"_key", usage.getHigherRankKey(r));
    }
  }

  private void addClassification(SolrDocument doc, LinneanClassification lc, LinneanClassificationKeys lck) {
    for (Rank r : Rank.DWC_RANKS) {
      ClassificationUtils.setHigherRank(lc, r, (String) doc.getFieldValue(r.name().toLowerCase()));
      ClassificationUtils.setHigherRankKey(lck, r, (Integer) doc.getFieldValue(r.name().toLowerCase()+"_key"));
    }
  }

  private static Description deserializeDescription(String description) {
    Description d = new Description();
    d.setDescription(description);
    return d;
  }

  private static VernacularName deserializeVernacularName(String vernacularName) {
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

  private static String serializeDescription(Description description) {
    return stripHtml(description.getDescription());
  }

  private static String stripHtml(String html) {
    if (!Strings.isNullOrEmpty(html)) {
      try {
        return Jsoup.parse(html).text();
      } catch (RuntimeException e) {
        LOG.error("Failed to read description input");
      }
    }
    return null;
  }

  private static String serializeVernacularName(VernacularName vn) {
    return vn.getLanguage() == null ? vn.getVernacularName() : vn.getLanguage().getIso2LetterCode() + CONCAT + vn.getVernacularName();
  }

  private static Integer ordinal(Enum val) {
    if (val != null) {
      return val.ordinal();
    }
    return null;
  }

  private static String str(@Nullable Object obj) {
    return obj == null ? null : obj.toString();
  }

  private static <T extends Enum<?>> T toEnum(SolrDocument doc, Class<T> vocab, String field) {
    return toEnum(vocab, (Integer) doc.getFieldValue(field));
  }

  private static <T extends Enum<?>> T toEnum(Class<T> vocab, Integer ordinal) {
    if (ordinal != null) {
      T[] values = vocab.getEnumConstants();
      return values[ordinal];
    }
    return null;
  }

  private static UUID toUUID(Object value) {
    if (value != null) {
      return UUID.fromString(value.toString());
    }
    return null;
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
      LOG.error("Error converting issues for usage {}", nameUsage.getKey(), e);
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
      doc.addField("vernacular_name_lang", serializeVernacularName(vernacularName));
    }
  }

}
