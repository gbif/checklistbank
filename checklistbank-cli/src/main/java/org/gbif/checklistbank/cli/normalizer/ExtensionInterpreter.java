package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.MediaObject;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.vocabulary.Extension;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.OccurrenceIssue;
import org.gbif.common.parsers.BooleanParser;
import org.gbif.common.parsers.CountryParser;
import org.gbif.common.parsers.EstablishmentMeansParser;
import org.gbif.common.parsers.LanguageParser;
import org.gbif.common.parsers.LifeStageParser;
import org.gbif.common.parsers.SexParser;
import org.gbif.common.parsers.core.EnumParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.common.parsers.UrlParser;
import org.gbif.common.parsers.NumberParser;
import org.gbif.common.parsers.MediaParser;
import org.gbif.common.parsers.date.DateParseUtils;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwc.terms.UnknownTerm;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;

public class ExtensionInterpreter {
  private final CountryParser countryParser = CountryParser.getInstance();
  private final LanguageParser languageParser = LanguageParser.getInstance();
  private final LifeStageParser lifeStageParser = LifeStageParser.getInstance();
  private final SexParser sexParser = SexParser.getInstance();
  private final EstablishmentMeansParser establishmentMeansParser = EstablishmentMeansParser.getInstance();
  private final BooleanParser booleanParser = BooleanParser.getInstance();
  private final MediaParser mediaParser = MediaParser.getInstance();

  private final TermFactory tf = TermFactory.instance();
  // EOL & AUDUBON MEDIA TERMS
  private final Term accessURI = tf.findTerm("http://rs.tdwg.org/ac/terms/accessURI");
  private final Term furtherInformationURL = tf.findTerm("http://rs.tdwg.org/ac/terms/furtherInformationURL");
  private final Term createDate = tf.findTerm("http://ns.adobe.com/xap/1.0/CreateDate");
  private final Term usageTerms = tf.findTerm("http://ns.adobe.com/xap/1.0/rights/UsageTerms");
  private final Term owner = tf.findTerm("http://ns.adobe.com/xap/1.0/rights/Owner");
  private final Term derivedFrom = tf.findTerm("http://rs.tdwg.org/ac/terms/derivedFrom");
  private final Term caption = tf.findTerm("http://rs.tdwg.org/ac/terms/caption");
  private final Term attributionLinkURL = tf.findTerm("http://rs.tdwg.org/ac/terms/attributionLinkURL");

  /**
   * Tries various terms in given order until it finds a non empty value.
   * @param rec
   * @param terms
   * @return non empty, cleaned value or null
   */
  private String value(Map<Term, String> rec, Term ... terms) {
    for (Term t : terms) {
      if (rec.containsKey(t)) {
        String val = NeoInserter.clean(rec.get(t));
        if (val != null) {
          return val;
        }
      }
    }
    return null;
  }

  /**
   * Tries to parse the verbatim value for the given term into a boolean.
   * If a value exists but cannot be parsed tan issue is added to the usage container.
   * @param rec
   * @param issue kind of issue to add if parsing fails
   * @param u
   * @param term
   * @return
   */
  private Boolean bool(Map<Term, String> rec, NameUsageIssue issue, NameUsageContainer u, Term term) {
    Boolean result = null;
    String val = value(rec, term);
    if (val != null) {
      result = booleanParser.parse(val).getPayload();
      if (result == null) {
        u.addIssue(issue);
      }
    }
    return result;
  }

  private Integer integer(Map<Term, String> rec, NameUsageIssue issue, NameUsageContainer u, Term term) {
    Integer i = null;
    String val = value(rec, term);
    if (val != null) {
      i = NumberParser.parseInteger(val);
      if (i == null) {
        u.addIssue(issue);
      }
    }
    return i;
  }

  private <T extends Enum<T>> T enumify(Map<Term, String> rec, NameUsageIssue issue, EnumParser<T> parser,
    NameUsageContainer u, Term ... terms) {
    boolean valuesFound = false;
    for (Term t : terms) {
      if (rec.containsKey(t)) {
        String val = NeoInserter.clean(rec.get(t));
        if (val != null) {
          valuesFound = true;
          T result = parser.parse(val).getPayload();
          if (result != null) {
            return result;
          }
        }
      }
    }
    // nothing found, raise issue?
    if (valuesFound) {
      u.addIssue(issue);
    }
    return null;
  }

  private void interpretVernacularNames(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.VERNACULAR_NAME)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.VERNACULAR_NAME)) {
        VernacularName vn = new VernacularName();
        vn.setVernacularName(value(rec, DwcTerm.vernacularName));
        if (vn.getVernacularName() == null) {
          u.addIssue(NameUsageIssue.VERNACULAR_NAME_INVALID);
          // flag, but ignore this non vernacular name
          continue;
        }
        // locationID > locality
        vn.setArea(value(rec, DwcTerm.locationID, DwcTerm.locality));
        vn.setCountry(enumify(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, countryParser, u,
                              DwcTerm.countryCode, DwcTerm.country, DwcTerm.locationID, DwcTerm.locality));
        vn.setLanguage(enumify(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, languageParser, u, DcTerm.language));
        vn.setLifeStage(enumify(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, lifeStageParser, u, DwcTerm.lifeStage));
        vn.setPlural(bool(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, u, GbifTerm.isPlural));
        vn.setPreferred(bool(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, u, GbifTerm.isPreferredName));
        vn.setSex(enumify(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, sexParser, u, GbifTerm.isPreferredName));
        vn.setSource(value(rec, DcTerm.source));
        // interpret rec
        u.getVernacularNames().add(vn);
      }
    }
  }

  //TODO: decide whether we want to index specimens ???
  private void interpretTypes(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.TYPES_AND_SPECIMEN)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.TYPES_AND_SPECIMEN)) {
//        TypeSpecimen t = new TypeSpecimen();
        // interpret
//        u.getTypeSpecimens().add(t);
      }
    }
  }

  private void interpretSpeciesProfiles(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.SPECIES_PROFILE)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.SPECIES_PROFILE)) {
        SpeciesProfile s = new SpeciesProfile();
        // interpret rec
        s.setSource(value(rec, DcTerm.source));
        s.setAgeInDays(integer(rec, NameUsageIssue.SPECIES_PROFILE_INVALID, u, GbifTerm.ageInDays));
        s.setMassInGram(integer(rec, NameUsageIssue.SPECIES_PROFILE_INVALID, u, GbifTerm.massInGram));
        s.setSizeInMillimeter(integer(rec, NameUsageIssue.SPECIES_PROFILE_INVALID, u, GbifTerm.sizeInMillimeter));
        s.setHybrid(bool(rec, NameUsageIssue.SPECIES_PROFILE_INVALID, u, GbifTerm.isHybrid));
        s.setMarine(bool(rec, NameUsageIssue.SPECIES_PROFILE_INVALID, u, GbifTerm.isMarine));
        s.setFreshwater(bool(rec, NameUsageIssue.SPECIES_PROFILE_INVALID, u, GbifTerm.isFreshwater));
        s.setTerrestrial(bool(rec, NameUsageIssue.SPECIES_PROFILE_INVALID, u, GbifTerm.isTerrestrial));
        s.setExtinct(bool(rec, NameUsageIssue.SPECIES_PROFILE_INVALID, u, GbifTerm.isExtinct));
        s.setLivingPeriod(value(rec, GbifTerm.livingPeriod));
        s.setLifeForm(value(rec, GbifTerm.lifeForm));
        s.setHabitat(value(rec, DwcTerm.habitat));
        u.getSpeciesProfiles().add(s);
      }
    }
  }

  private void interpretReference(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.REFERENCE)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.REFERENCE)) {
        Reference r = new Reference();
        // interpret rec
        r.setType(value(rec, DcTerm.type));
        r.setCitation(value(rec, DcTerm.bibliographicCitation));
        r.setTitle(value(rec, DcTerm.title));
        r.setAuthor(value(rec, DcTerm.creator));
        r.setDate(value(rec, DcTerm.date, DcTerm.created));
        r.setSource(value(rec, DcTerm.source));
        r.setRemarks(value(rec, DwcTerm.taxonRemarks));
        // TODO: need to check this mapping!
        r.setDoi(value(rec, DcTerm.identifier));
        r.setLink(value(rec, DcTerm.references));
        u.getReferenceList().add(r);
      }
    }
  }

  private void extractMedia(NameUsageContainer u, VerbatimNameUsage v, Extension ext) {
    if (v.hasExtension(ext)) {
      for (Map<Term, String> rec : v.getExtensions().get(ext)) {
        URI uri = UrlParser.parse(value(rec, accessURI, DcTerm.identifier));
        URI link = UrlParser.parse(value(rec, furtherInformationURL, DcTerm.references, attributionLinkURL));
        // link or media uri must exist
        if (uri == null && link == null) {
          u.addIssue(NameUsageIssue.MULTIMEDIA_INVALID);
        } else {
          NameUsageMediaObject m = new NameUsageMediaObject();
          m.setIdentifier(uri);
          m.setReferences(link);
          m.setTitle(value(rec, DcTerm.title, caption));
          m.setDescription(value(rec, DcTerm.description));
          m.setLicense(value(rec, DcTerm.license, usageTerms, DcTerm.rights));
          m.setPublisher(value(rec, DcTerm.publisher));
          m.setContributor(value(rec, DcTerm.contributor));
          m.setSource(value(rec, derivedFrom, DcTerm.source));
          m.setAudience(value(rec, DcTerm.audience));
          m.setRightsHolder(value(rec, owner, DcTerm.rightsHolder));
          m.setCreator(value(rec, DcTerm.creator));
          m.setFormat(mediaParser.parseMimeType(value(rec, DcTerm.format)));
          String created = value(rec, createDate, DcTerm.created, DcTerm.date);
          if (created != null) {
            m.setCreated(DateParseUtils.parse(created).getPayload());
          }
          mediaParser.detectType(m);
          u.getMedia().add(m);
        }
      }
    }
  }

  private void interpretMultimedia(NameUsageContainer u, VerbatimNameUsage v) {
    extractMedia(u, v, Extension.IMAGE);
    extractMedia(u, v, Extension.MULTIMEDIA);
    extractMedia(u, v, Extension.EOL_MEDIA);
    extractMedia(u, v, Extension.AUDUBON);
    /**
     * merges media records if the same image URL or link is given several times.
     * Remove any media that has not either a file or webpage uri.
     */
    Map<String, NameUsageMediaObject> media = Maps.newLinkedHashMap();
    for (NameUsageMediaObject m : u.getMedia()) {
      // we can get file uris or weblinks. Prefer file URIs as they clearly identify a single image
      URI uri = m.getIdentifier() != null ? m.getIdentifier() : m.getReferences();
      if (uri != null) {
        String url = uri.toString();
        if (media.containsKey(url)) {
          // merge infos about the same image?
        } else {
          media.put(url, m);
        }
      }
    }
    u.setMedia(Lists.newArrayList(media.values()));
  }

  private void interpretIdentifier(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.IDENTIFIER)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.IDENTIFIER)) {
        Identifier i = new Identifier();
        // interpret rec
//        u.getIdentifiers().add(i);
      }
    }
  }

  private void interpretDistribution(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.DISTRIBUTION)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.DISTRIBUTION)) {
        Distribution d = new Distribution();
        // interpret rec
//        u.getDistributions().add(d);
      }
    }
  }

  private void interpretDescription(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.DESCRIPTION)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.DESCRIPTION)) {
        Description d = new Description();
        // interpret rec
//        u.getDescriptions().add(d);
      }
    }
  }

  public void interpret(NameUsageContainer u, VerbatimNameUsage v) {
    interpretDescription(u,v);
    interpretDistribution(u, v);
    interpretIdentifier(u, v);
    interpretMultimedia(u, v);
    interpretReference(u, v);
    interpretSpeciesProfiles(u, v);
    interpretTypes(u, v);
    interpretVernacularNames(u, v);
  }
}
