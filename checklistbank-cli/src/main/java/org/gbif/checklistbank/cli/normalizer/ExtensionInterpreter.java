package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.*;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.vocabulary.Extension;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.MediaType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.neo.NeoInserter;
import org.gbif.common.parsers.*;
import org.gbif.common.parsers.core.EnumParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.common.parsers.date.DateParsers;
import org.gbif.common.parsers.date.TemporalAccessorUtils;
import org.gbif.dwc.terms.*;

import java.net.URI;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionInterpreter {
  private static final Logger LOG = LoggerFactory.getLogger(ExtensionInterpreter.class);

  private final CountryParser countryParser = CountryParser.getInstance();
  private final LanguageParser languageParser = LanguageParser.getInstance();
  private final LifeStageParser lifeStageParser = LifeStageParser.getInstance();
  private final SexParser sexParser = SexParser.getInstance();
  private final EstablishmentMeansParser establishmentMeansParser = EstablishmentMeansParser.getInstance();
  private final BooleanParser booleanParser = BooleanParser.getInstance();
  private final MediaParser mediaParser = MediaParser.getInstance();
  private final OccurrenceStatusParser occurrenceStatusParser = OccurrenceStatusParser.getInstance();
  private final ThreatStatusParser threatStatusParser = ThreatStatusParser.getInstance();
  private final CitesAppendixParser citesAppendixParser= CitesAppendixParser.getInstance();
  private final TypeStatusParser typeStatusParser = TypeStatusParser.getInstance();
  private final MediaTypeParser mediaTypeParser = MediaTypeParser.getInstance();

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
  private Boolean bool(Map<Term, String> rec, NameUsageIssue issue, NameUsage u, Term term) {
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

  private Integer integer(Map<Term, String> rec, NameUsageIssue issue, NameUsage u, Term term) {
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

  private <T extends Enum<T>> T enumify(Map<Term, String> rec, @Nullable NameUsageIssue issue, EnumParser<T> parser, NameUsage u, Term ... terms) {
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
    if (valuesFound && issue != null) {
      u.addIssue(issue);
    }
    return null;
  }

  private void interpretVernacularNames(NameUsage u, UsageExtensions e, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.VERNACULAR_NAME)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.VERNACULAR_NAME)) {
        VernacularName vn = new VernacularName();
        vn.setVernacularName(value(rec, DwcTerm.vernacularName));
        if (vn.getVernacularName() == null) {
          u.addIssue(NameUsageIssue.VERNACULAR_NAME_INVALID);
          continue;
        }
        // locationID > locality
        vn.setArea(value(rec, DwcTerm.locationID, DwcTerm.locality));
        vn.setCountry(enumify(rec, null, countryParser, u,
                              DwcTerm.countryCode, DwcTerm.country, DwcTerm.locationID, DwcTerm.locality));
        vn.setLanguage(enumify(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, languageParser, u, DcTerm.language));
        vn.setLifeStage(enumify(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, lifeStageParser, u, DwcTerm.lifeStage));
        vn.setPlural(bool(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, u, GbifTerm.isPlural));
        vn.setPreferred(bool(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, u, GbifTerm.isPreferredName));
        vn.setSex(enumify(rec, NameUsageIssue.VERNACULAR_NAME_INVALID, sexParser, u, DwcTerm.sex));
        vn.setSource(value(rec, DcTerm.source));
        // interpret rec
        e.vernacularNames.add(vn);
      }
    }
  }

  /**
   * We only keep type names and ignore specimens...
   */
  private void interpretTypes(NameUsage u, UsageExtensions e, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.TYPES_AND_SPECIMEN)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.TYPES_AND_SPECIMEN)) {
        TypeSpecimen t = new TypeSpecimen();
        // interpret
        t.setScientificName(expandGenus(value(rec, DwcTerm.scientificName), u.getScientificName()));

        if (t.getScientificName() == null || t.getScientificName().equalsIgnoreCase(u.getScientificName())) {
          LOG.debug("Ignore type name for {} as the name is the same as the taxon", u.getScientificName());
          continue;
        }
        t.setTypeDesignatedBy(value(rec, GbifTerm.typeDesignatedBy));
        t.setTypeStatus(enumify(rec, null, typeStatusParser, u, DwcTerm.typeStatus));
        t.setSource(value(rec, DcTerm.source));
        //t.setCitation(value(rec, DcTerm.bibliographicCitation));
        //t.setTypeDesignationType(value(rec, GbifTerm.typeDesignatedType));
        e.typeSpecimens.add(t);
      }
    }
  }

  /**
   * Expands abbreviated genus names with the full genus
   * @param abbreviatedName the potentially abbreviated scientific name, e.g. "A. alba"
   * @param scientificName the full scientific name for the main taxon
   */
  @VisibleForTesting
  protected static String expandGenus(String abbreviatedName, String scientificName) {
    // test if name has an abbreviated genus
    if (abbreviatedName != null && abbreviatedName.length() > 1 && scientificName != null && scientificName.length() > 2) {
      String[] parts = abbreviatedName.split("\\s+", 2);
      String genus = scientificName.split("\\s+", 2)[0];
      String abbrev = parts[0].replaceAll("\\.$", "");

      if (parts.length == 2 && abbrev.length() < 4 && genus.startsWith(abbrev)) {
        return genus + " " + parts[1];
      }
    }
    return abbreviatedName;
  }

  private void interpretSpeciesProfiles(NameUsage u, UsageExtensions e, VerbatimNameUsage v) {
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
        e.speciesProfiles.add(s);
      }
    }
  }

  private void interpretReference(NameUsage u, UsageExtensions e, VerbatimNameUsage v) {
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
        e.referenceList.add(r);
      }
    }
  }

  private void extractMedia(NameUsage u, UsageExtensions e, VerbatimNameUsage v, Extension ext, boolean requireType) {
    if (v.hasExtension(ext)) {
      for (Map<Term, String> rec : v.getExtensions().get(ext)) {
        URI uri = UrlParser.parse(value(rec, AcTerm.accessURI, DcTerm.identifier, DcElement.identifier));
        URI link = UrlParser.parse(value(rec, AcTerm.furtherInformationURL, DcTerm.references, AcTerm.attributionLinkURL));
        // EOL media extension is also used to publish text descriptions - avoid those
        MediaType type = enumify(rec, null, mediaTypeParser, u, DcTerm.type, DcElement.type);
        if (requireType && type == null) {
          continue;
        }
        // link or media uri must exist
        if (uri == null && link == null) {
          u.addIssue(NameUsageIssue.MULTIMEDIA_INVALID);
        } else {
          NameUsageMediaObject m = new NameUsageMediaObject();
          m.setType(type);
          m.setIdentifier(uri);
          m.setReferences(link);
          m.setTitle(value(rec, DcTerm.title, AcTerm.caption));
          m.setDescription(value(rec, DcTerm.description));
          m.setLicense(value(rec, DcTerm.license, XmpRightsTerm.UsageTerms, DcTerm.rights));
          m.setPublisher(value(rec, DcTerm.publisher));
          m.setContributor(value(rec, DcTerm.contributor));
          m.setSource(value(rec, AcTerm.derivedFrom, DcTerm.source));
          m.setAudience(value(rec, DcTerm.audience));
          m.setRightsHolder(value(rec, XmpRightsTerm.Owner, DcTerm.rightsHolder));
          m.setCreator(value(rec, DcTerm.creator));
          m.setFormat(mediaParser.parseMimeType(value(rec, DcTerm.format)));
          String created = value(rec, XmpTerm.CreateDate, DcTerm.created, DcTerm.date);
          if (created != null) {
            ParseResult<TemporalAccessor> pr = DateParsers.defaultTemporalParser().parse(created);
            if (pr.isSuccessful()) {
              m.setCreated(TemporalAccessorUtils.toDate(pr.getPayload()));
            }
          }
          mediaParser.detectType(m);
          e.media.add(m);
        }
      }
    }
  }

  private void interpretMultimedia(NameUsage u, UsageExtensions e, VerbatimNameUsage v) {
    extractMedia(u, e, v, Extension.IMAGE, false);
    extractMedia(u, e, v, Extension.MULTIMEDIA, false);
    extractMedia(u, e, v, Extension.EOL_MEDIA, true);
    extractMedia(u, e, v, Extension.AUDUBON, false);
    extractMediaCore(u, e, v);
    /**
     * merges media records if the same image URL or link is given several times.
     * Remove any media that has not either a file or webpage uri.
     */
    Map<String, NameUsageMediaObject> media = Maps.newLinkedHashMap();
    for (NameUsageMediaObject m : e.media) {
      // we can get file uris or weblinks. Prefer file URIs as they clearly identify a single image
      URI uri = m.getIdentifier() != null ? m.getIdentifier() : m.getReferences();
      if (uri != null) {
        String url = uri.toString();
        if (media.containsKey(url)) {
          // TODO: merge infos about the same image?
        } else {
          media.put(url, m);
        }
      }
    }
    e.media = Lists.newArrayList(media.values());
  }

  private void extractMediaCore(NameUsage u, UsageExtensions e, VerbatimNameUsage v) {
    if (v.hasCoreField(DwcTerm.associatedMedia)) {
      for (URI uri : UrlParser.parseUriList(v.getCoreField(DwcTerm.associatedMedia))) {
        if (uri != null) {
          NameUsageMediaObject m = new NameUsageMediaObject();
          m.setIdentifier(uri);
          mediaParser.detectType(m);
          e.media.add(m);
        }
      }
    }
  }

  private void interpretIdentifier(NameUsage u, UsageExtensions e, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.IDENTIFIER)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.IDENTIFIER)) {
        Identifier i = new Identifier();
        // interpret rec
        i.setIdentifier(value(rec, DcTerm.identifier));
        if (i.getIdentifier() == null) {
          u.addIssue(NameUsageIssue.ALT_IDENTIFIER_INVALID);
          continue;
        }
        i.setTitle(value(rec, DcTerm.title));
        i.setType(IdentifierType.inferFrom(i.getIdentifier()));
        e.identifiers.add(i);
      }
    }
  }

  private void interpretDistribution(NameUsage u, UsageExtensions e, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.DISTRIBUTION)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.DISTRIBUTION)) {
        Distribution d = new Distribution();
        // interpret rec
        d.setLocality(value(rec, DwcTerm.locality));
        d.setLocationId(value(rec, DwcTerm.locationID));
        d.setCountry(enumify(rec, NameUsageIssue.DISTRIBUTION_INVALID, countryParser, u, DwcTerm.country, DwcTerm.countryCode));
        // some location is required, otherwise its pointless
        if (d.getLocality() == null && d.getLocationId() == null && d.getCountry() == null) {
          u.addIssue(NameUsageIssue.DISTRIBUTION_INVALID);
          continue;
        }
//        d.setStatus(enumify(rec, NameUsageIssue.DISTRIBUTION_INVALID, occurrenceStatusParser, u, DwcTerm.occurrenceStatus));
        d.setEstablishmentMeans(enumify(rec, NameUsageIssue.DISTRIBUTION_INVALID, establishmentMeansParser, u, DwcTerm.establishmentMeans));
        d.setAppendixCites(enumify(rec, NameUsageIssue.DISTRIBUTION_INVALID, citesAppendixParser, u, GbifTerm.appendixCITES));
        d.setThreatStatus(enumify(rec, NameUsageIssue.DISTRIBUTION_INVALID, threatStatusParser, u, IucnTerm.threatStatus));
        d.setLifeStage(enumify(rec, NameUsageIssue.DISTRIBUTION_INVALID, lifeStageParser, u, DwcTerm.lifeStage));
        d.setTemporal(value(rec, DwcTerm.eventDate, DcTerm.temporal));
        d.setEndDayOfYear(integer(rec, NameUsageIssue.DISTRIBUTION_INVALID, u, DwcTerm.endDayOfYear));
        d.setStartDayOfYear(integer(rec, NameUsageIssue.DISTRIBUTION_INVALID, u, DwcTerm.startDayOfYear));
        d.setRemarks(value(rec, DwcTerm.occurrenceRemarks, DwcTerm.taxonRemarks));
        d.setSource(value(rec, DcTerm.source));
        e.distributions.add(d);
      }
    }
  }

  private void interpretDescription(NameUsage u, UsageExtensions e, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.DESCRIPTION)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.DESCRIPTION)) {
        Description d = new Description();
        // interpret rec
        d.setDescription(value(rec, DcTerm.description, DcTerm.abstract_));
        d.setType(value(rec, DcTerm.type));
        d.setSource(value(rec, DcTerm.source));
        d.setContributor(value(rec, DcTerm.contributor));
        d.setCreator(value(rec, DcTerm.creator, DcTerm.rightsHolder));
        d.setLanguage(enumify(rec, NameUsageIssue.DESCRIPTION_INVALID, languageParser, u, DcTerm.language));
        d.setLicense(value(rec, DcTerm.license, DcTerm.rights));
        e.descriptions.add(d);
      }
    }
    // EOL MULTIMEDIA
    else if (v.hasExtension(Extension.EOL_MEDIA)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.EOL_MEDIA)) {
        // ignore non text type records
        if (!isTextType(value(rec, DcTerm.type))) {
          continue;
        }
        Description d = new Description();
        // interpret rec
        d.setType(value(rec, DcTerm.title));
        d.setDescription(value(rec, DcTerm.description, DcTerm.abstract_));
        // make sure we have some description
        if (d.getDescription() == null) {
          u.addIssue(NameUsageIssue.DESCRIPTION_INVALID);
          continue;
        }
        d.setSource(value(rec, DcTerm.source, DcTerm.bibliographicCitation));
        d.setContributor(value(rec, DcTerm.contributor));
        d.setCreator(value(rec, DcTerm.creator, DcTerm.rightsHolder, XmpRightsTerm.Owner, DcTerm.publisher));
        d.setLanguage(enumify(rec, NameUsageIssue.DESCRIPTION_INVALID, languageParser, u, DcTerm.language));
        d.setLicense(value(rec, DcTerm.license, DcTerm.rights, XmpRightsTerm.UsageTerms));
        e.descriptions.add(d);
      }
    }
    // verify descriptions
    // make sure we have some description
    Iterator<Description> iter = e.descriptions.iterator();
    while(iter.hasNext()) {
      Description d = iter.next();
      if (StringUtils.isBlank(d.getDescription())) {
        u.addIssue(NameUsageIssue.DESCRIPTION_INVALID);
        iter.remove();
      }
    }
  }

  private boolean isTextType(String type) {
    return type.equalsIgnoreCase("http://purl.org/dc/dcmitype/Text")
           || type.equalsIgnoreCase("purl.org/dc/dcmitype/Text")
           || type.equalsIgnoreCase("dcmitype:Text")
           || type.equalsIgnoreCase("dctype:Text")
           || type.equalsIgnoreCase("dc:Text")
           || type.equalsIgnoreCase("Text");
  }

  public UsageExtensions interpret(NameUsage u, VerbatimNameUsage v) {
    UsageExtensions ext = new UsageExtensions();
    interpretDescription(u, ext,v);
    interpretDistribution(u ,ext, v);
    interpretIdentifier(u, ext, v);
    interpretMultimedia(u, ext, v);
    interpretReference(u, ext, v);
    interpretSpeciesProfiles(u, ext, v);
    interpretTypes(u, ext, v);
    interpretVernacularNames(u, ext, v);
    return ext;
  }

}
