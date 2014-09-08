package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.vocabulary.Extension;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.common.parsers.BooleanParser;
import org.gbif.common.parsers.CountryParser;
import org.gbif.common.parsers.EstablishmentMeansParser;
import org.gbif.common.parsers.LanguageParser;
import org.gbif.common.parsers.LifeStageParser;
import org.gbif.common.parsers.SexParser;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;

import java.util.Map;

import com.google.common.base.Strings;

public class ExtensionInterpreter {
  private final CountryParser countryParser = CountryParser.getInstance();
  private final LanguageParser languageParser = LanguageParser.getInstance();
  private final LifeStageParser lifeStageParser = LifeStageParser.getInstance();
  private final SexParser sexParser = SexParser.getInstance();
  private final EstablishmentMeansParser establishmentMeansParser = EstablishmentMeansParser.getInstance();
  private final BooleanParser booleanParser = BooleanParser.getInstance();

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

  private Boolean bool(Map<Term, String> rec, Term term) {
    return booleanParser.parse(value(rec, term)).getPayload();
  }

  private Integer integer(Map<Term, String> rec, Term term) {
    try {
      return Integer.parseInt(value(rec, term));
    } catch (NumberFormatException e) {

    }
    return null;
  }

  private void interpretVernacularNames(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.VERNACULAR_NAME)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.VERNACULAR_NAME)) {
        VernacularName vn = new VernacularName();
        vn.setVernacularName(value(rec, DwcTerm.vernacularName));
        if (vn.getVernacularName() == null) {
          //u.addIssue(NameUsageIssue.VERNACULAR_NAME_INVALID);
          continue;
        }
        // locationID > locality
        vn.setArea(value(rec, DwcTerm.locationID, DwcTerm.locality));

        vn.setCountry(countryParser.parse(value(rec, DwcTerm.countryCode, DwcTerm.country, DwcTerm.locationID)).getPayload());
        vn.setLanguage(languageParser.parse(value(rec, DcTerm.language)).getPayload());
        vn.setLifeStage(lifeStageParser.parse(value(rec, DwcTerm.lifeStage)).getPayload());
        vn.setPlural(bool(rec, GbifTerm.isPlural));
        vn.setPreferred(bool(rec, GbifTerm.isPreferredName));
        vn.setSex(sexParser.parse(value(rec, DwcTerm.sex)).getPayload());
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
        TypeSpecimen t = new TypeSpecimen();
        // interpret
        u.getTypeSpecimens().add(t);
      }
    }
  }

  private void interpretSpeciesProfiles(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.SPECIES_PROFILE)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.SPECIES_PROFILE)) {
        SpeciesProfile s = new SpeciesProfile();
        // interpret rec
        s.setSource(value(rec, DcTerm.source));
        s.setAgeInDays(integer(rec, GbifTerm.ageInDays));
        s.setMassInGram(integer(rec, GbifTerm.massInGram));
        s.setSizeInMillimeter(integer(rec, GbifTerm.sizeInMillimeter));
        s.setHybrid(bool(rec, GbifTerm.isHybrid));
        s.setMarine(bool(rec, GbifTerm.isMarine));
        s.setFreshwater(bool(rec, GbifTerm.isFreshwater));
        s.setTerrestrial(bool(rec, GbifTerm.isTerrestrial));
        s.setExtinct(bool(rec, GbifTerm.isExtinct));
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
        u.getReferenceList().add(r);
      }
    }
  }

  private void interpretMultimedia(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.MULTIMEDIA)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.MULTIMEDIA)) {
        NameUsageMediaObject m = new NameUsageMediaObject();
        // interpret rec
        u.getMedia().add(m);
      }
    }
    if (v.hasExtension(Extension.IMAGE)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.IMAGE)) {
        NameUsageMediaObject m = new NameUsageMediaObject();
        // interpret rec
        u.getMedia().add(m);
      }
    }
    if (v.hasExtension(Extension.EOL_MEDIA)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.EOL_MEDIA)) {
        NameUsageMediaObject m = new NameUsageMediaObject();
        // interpret rec
        u.getMedia().add(m);
      }
    }
    if (v.hasExtension(Extension.AUDUBON)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.AUDUBON)) {
        NameUsageMediaObject m = new NameUsageMediaObject();
        // interpret rec
        u.getMedia().add(m);
      }
    }
  }

  private void interpretIdentifier(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.IDENTIFIER)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.IDENTIFIER)) {
        Identifier i = new Identifier();
        // interpret rec
        u.getIdentifiers().add(i);
      }
    }
  }

  private void interpretDistribution(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.DISTRIBUTION)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.DISTRIBUTION)) {
        Distribution d = new Distribution();
        // interpret rec
        u.getDistributions().add(d);
      }
    }
  }

  private void interpretDescription(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.DESCRIPTION)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.DESCRIPTION)) {
        Description d = new Description();
        // interpret rec
        u.getDescriptions().add(d);
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
