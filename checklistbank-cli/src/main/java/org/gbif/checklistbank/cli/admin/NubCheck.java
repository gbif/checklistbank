package org.gbif.checklistbank.cli.admin;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.vocabulary.Rank;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class NubCheck {
  final NameUsageMatchingService client;

  public NubCheck(NameUsageMatchingService service) {
    this.client = service;
  }

  public void testFile(File test) throws IOException {
    CSVReader reader = CSVReaderFactory.build(test, "UTF-8", ",", '"', 0);

    while (reader.hasNext()) {
      String[] row = reader.next();
      if (row.length >= 9) {
        String r = Strings.emptyToNull(getCol(row, 9));
        Rank rank = r==null? null : Rank.valueOf(r);
        LinneanClassification cl = new NameUsageMatch();
        cl.setKingdom(getCol(row, 1));
        cl.setPhylum(getCol(row, 2));
        cl.setClazz(getCol(row, 3));
        cl.setOrder(getCol(row, 4));
        cl.setFamily(getCol(row, 5));
        cl.setGenus(getCol(row, 6));
        testQuery(getColAsInt(row, 0), cl, getCol(row, 7), getCol(row, 8), rank);
      } else {
        // probably a comment
        System.out.println(StringUtils.join(row));
      }
    }
  }

  private String getCol(String[] row, int column) {
    String val = StringUtils.trimToNull(row[column]);
    if (val == null || val.equalsIgnoreCase("null")) {
      return null;
    }
    return val;
  }

  private Integer getColAsInt(String[] row, int column) {
    String val = getCol(row, column);
    if (val != null) {
      try {
        return Integer.valueOf(val);
      } catch (NumberFormatException e) {
        // nothing to do
      }
    }
    return null;
  }

  private void testQuery(Integer targetNubId, LinneanClassification cl, String scientificName, String authorship, Rank rank) {

    List<NameUsageMatch> results = Lists.newArrayList();
    NameUsageMatch lookup = null;
    try {
      lookup = client.match(scientificName, rank, cl, false, true);
      results.add(lookup);

      boolean gotMatch;
      if (targetNubId == null) {
        gotMatch = lookup.getUsageKey() == null;
      } else {
        gotMatch = lookup.getUsageKey() != null && lookup.getUsageKey().equals(targetNubId);
      }
      if (rank != null && rank.isSuprageneric()){
        if (lookup.getSpeciesKey()!=null || lookup.getSpecies()!=null){
          System.err.println(
              "Lookup for "+rank.name()+" [" + scientificName +"] contains non null species "+lookup.getSpecies()+" [" + lookup.getSpeciesKey() + "]");
        }
      }
      if (gotMatch) {
        System.out.println(
            "Lookup good for sciName [" + scientificName + "] nubID [" + targetNubId + "] - " + client.toString());
      } else {
        System.err.println(
            "Lookup mismatch for sciName [" + scientificName +"] - wanted [" + targetNubId + "], got [" + (lookup == null ? null : lookup.getUsageKey()) + "] - [" + client.toString() + "]");
      }
    } catch (Exception e) {
      System.err.println("Webservice ERROR: " + e.getMessage());
    }

    for (NameUsageMatch outer : results) {
      if (outer==null) continue;
      for (NameUsageMatch inner : results) {
        if (inner == null) continue;
        if (System.identityHashCode(outer) != System.identityHashCode(inner)) {
          if (!equalLookups(outer, inner)) {
            System.err.println("Results are different from different servers for sciName [" + scientificName + "]");
          }
        }
      }
    }
  }

  private boolean equalLookups(NameUsageMatch a, NameUsageMatch b) {
    boolean match = true;
    if (a.getConfidence() == null || b.getConfidence() == null) {
      match = match && a.getConfidence() == null && b.getConfidence() == null;
    } else {
      match = match && a.getConfidence().equals(b.getConfidence());
    }

    if (a.getUsageKey() == null || b.getUsageKey() == null) {
      match = match && a.getUsageKey() == null && b.getUsageKey() == null;
    } else {
      match = match && a.getUsageKey().equals(b.getUsageKey());
    }

    if (a.getClassKey() == null || b.getClassKey() == null) {
      match = match && a.getClassKey() == null && b.getClassKey() == null;
    } else {
      match = match && a.getClassKey().equals(b.getClassKey());
    }

    if (a.getFamilyKey() == null || b.getFamilyKey() == null) {
      match = match && a.getFamilyKey() == null && b.getFamilyKey() == null;
    } else {
      match = match && a.getFamilyKey().equals(b.getFamilyKey());
    }

    if (a.getGenusKey() == null || b.getGenusKey() == null) {
      match = match && a.getGenusKey() == null && b.getGenusKey() == null;
    } else {
      match = match && a.getGenusKey().equals(b.getGenusKey());
    }

    if (a.getKingdomKey() == null || b.getKingdomKey() == null) {
      match = match && a.getKingdomKey() == null && b.getKingdomKey() == null;
    } else {
      match = match && a.getKingdomKey().equals(b.getKingdomKey());
    }

    if (a.getOrderKey() == null || b.getOrderKey() == null) {
      match = match && a.getOrderKey() == null && b.getOrderKey() == null;
    } else {
      match = match && a.getOrderKey().equals(b.getOrderKey());
    }

    if (a.getPhylumKey() == null || b.getPhylumKey() == null) {
      match = match && a.getPhylumKey() == null && b.getPhylumKey() == null;
    } else {
      match = match && a.getPhylumKey().equals(b.getPhylumKey());
    }

    if (a.getSpeciesKey() == null || b.getSpeciesKey() == null) {
      match = match && a.getSpeciesKey() == null && b.getSpeciesKey() == null;
    } else {
      match = match && a.getSpeciesKey().equals(b.getSpeciesKey());
    }

    return match;
  }

}
