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
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;

import java.util.Map;

import com.google.common.base.Strings;

public class ExtensionInterpreter {


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


  private void interpretVernacularNames(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.VERNACULAR_NAME)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.VERNACULAR_NAME)) {
        VernacularName vn = new VernacularName();
        vn.setVernacularName(value(rec, DwcTerm.vernacularName));
        if (vn.getVernacularName() == null) {
//          u.addIssue(NameUsageIssue.VERNACULAR_NAME_INVALID);
          continue;
        }
        // locationID > locality
        vn.setArea(value(rec, DwcTerm.locationID, DwcTerm.locality));

//        vn.setCountry();
//        vn.setLanguage();
//        vn.setLifeStage();
//        vn.setPlural();
//        vn.setPreferred();
//        vn.setSex();
//        vn.setSource(clean(rec.get(DcTerm.source)));
        // interpret rec
        u.getVernacularNames().add(vn);
      }
    }
  }

  private void interpretTypes(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.TYPES_AND_SPECIMEN)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.TYPES_AND_SPECIMEN)) {
        TypeSpecimen t = new TypeSpecimen();
        // interpret rec
        u.getTypeSpecimens().add(t);
      }
    }
  }

  private void interpretSpeciesProfiles(NameUsageContainer u, VerbatimNameUsage v) {
    if (v.hasExtension(Extension.SPECIES_PROFILE)) {
      for (Map<Term, String> rec : v.getExtensions().get(Extension.SPECIES_PROFILE)) {
        SpeciesProfile s = new SpeciesProfile();
        // interpret rec
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
