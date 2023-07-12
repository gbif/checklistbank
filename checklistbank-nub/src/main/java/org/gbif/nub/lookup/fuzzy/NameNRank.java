package org.gbif.nub.lookup.fuzzy;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.model.Classification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.gbif.checklistbank.utils.ParameterUtils.first;

public class NameNRank {
  private static final Logger LOG = LoggerFactory.getLogger(NameNRank.class);
  private static final List<Rank> REVERSED_DWC_RANKS = new ArrayList<>(Rank.DWC_RANKS);
  static {
    Collections.reverse(REVERSED_DWC_RANKS);
  }
  private static final Pattern BINOMIAL = Pattern.compile("^\\s*([A-Z][a-z]+)\\s+([a-z1-9-]+)\\s*$");

  public final String name;
  public final Rank rank;

  public NameNRank(String name, Rank rank) {
    this.name = name;
    this.rank = rank;
  }

  /**
   * Construct the best possible full name with authorship and rank out of various input parameters
   * @param scientificName
   * @param authorship
   * @param specificEpithet
   * @param infraSpecificEpithet
   * @param rank
   * @param classification
   * @return
   */
  public static NameNRank build(@Nullable String scientificName, @Nullable String authorship,
                                @Nullable String genericName, @Nullable String specificEpithet, @Nullable String infraSpecificEpithet,
                                @Nullable Rank rank, @Nullable LinneanClassification classification) {
    // make sure we have a classification instance
    classification = classification == null ? new Classification() : classification;
    final String genus = clean(first(genericName, classification.getGenus()));
    // If given primarily trust the scientific name, especially since these can be unparsable names like OTUs
    // only exceptions is when the scientific name clearly is just a part of the atoms - then reassemble it
    // authorship can be appended as this is a very common case
    if (exists(scientificName) && useScientificName(scientificName, genericName, specificEpithet, infraSpecificEpithet, classification)) {
      // expand abbreviated or placeholder genus?
      scientificName = expandAbbreviatedGenus(scientificName, genus);
      // missing authorship?
      scientificName = appendAuthorship(scientificName, authorship);
      // ignore atomized name parameters, but warn if not present
      warnIfMissing(scientificName, genus, "genus");
      warnIfMissing(scientificName, specificEpithet, "specificEpithet");
      warnIfMissing(scientificName, infraSpecificEpithet, "infraSpecificEpithet");
      return new NameNRank(scientificName, rank);

    } else {
      // no name given, assemble from pieces as best as we can
      Rank clRank = lowestRank(classification);
      if (genus == null && (clRank == null || clRank.isSuprageneric())) {
        // use epithets if existing - otherwise higher rank if given
        if (any(specificEpithet, infraSpecificEpithet, authorship)) {
          // we dont have any genus or species binomen given, just epithets :(
          StringBuilder sb = new StringBuilder();
          sb.append("?"); // no genus
          appendIfExists(sb, specificEpithet);
          appendIfExists(sb, infraSpecificEpithet);
          appendIfExists(sb, authorship);
          return new NameNRank(sb.toString(), rank);

        } else if (clRank != null){
          return new NameNRank(classification.getHigherRank(clRank), clRank);
        } else {
          return new NameNRank(null, rank);
        }

      } else {
        // try atomized
        ParsedName pn = new ParsedName();
        pn.setGenusOrAbove(genus);
        pn.setInfraGeneric(clean(classification.getSubgenus()));
        pn.setSpecificEpithet(clean(specificEpithet));
        pn.setInfraSpecificEpithet(clean(infraSpecificEpithet));
        pn.setRank(rank);
        pn.setAuthorship(clean(authorship));
        // see if species rank in classificaiton can contribute sth
        if (exists(classification.getSpecies())) {
          Matcher m = BINOMIAL.matcher(clean(classification.getSpecies()));
          if (m.find()) {
            if (pn.getGenusOrAbove() == null) {
              pn.setGenusOrAbove(m.group(1));
            }
            if (pn.getSpecificEpithet() == null) {
              pn.setSpecificEpithet(m.group(2));
            }
          } else if (StringUtils.isAllLowerCase(classification.getSpecies()) && !clean(classification.getSpecies()).contains(" ")) {
            // sometimes the field is wrongly used as the species epithet
            pn.setSpecificEpithet(clean(classification.getSpecies()));
          }
        }
        return new NameNRank(pn.canonicalNameComplete(), rank);
      }
    }
  }

  private static String clean(String x) {
    x = StringUtils.trimToNull(x);
    if (x != null) {
      switch (x) {
        case "\\N":
        case "null":
        case "Null":
        case "NULL":
          return null;
        default:
      }
    }
    return x;
  }

  private static boolean any(String... x) {
    if (x != null) {
      for (String y : x) {
        if (exists(y)) return true;
      }
    }
    return false;
  }

  private static void appendIfExists(StringBuilder sb, @Nullable String x) {
    if (exists(x)) {
      if (sb.length()>0) {
        sb.append(" ");
      }
      sb.append(x.trim());
    }
  }

  private static boolean useScientificName(String scientificName, @Nullable String genericName, @Nullable String specificEpithet, @Nullable String infraSpecificEpithet, LinneanClassification cl) {
    // without genus given we cannot assemble the name, so lets then just use it as it is
    if (exists(cl.getGenus()) || exists(genericName) || isSimpleBinomial(cl.getSpecies())) {
      // scientific name is just one of the epithets
      if (StringUtils.isAllLowerCase(scientificName) &&
          (scientificName.equals(specificEpithet) || scientificName.equals(infraSpecificEpithet) || scientificName.equals(cl.getSpecies()))
      ) {
        return false;
      }
    }
    return true;
  }

  @VisibleForTesting
  static boolean isSimpleBinomial(String name) {
    return exists(name) && BINOMIAL.matcher(name).matches();
  }

  private static void warnIfMissing(String name, @Nullable String epithet, String part) {
    if (exists(epithet) && !name.toLowerCase().contains(epithet.toLowerCase())) {
      LOG.warn("ScientificName >{}< missing {}: {}", name, part, epithet);
    }
  }

  private static boolean exists(@Nullable String x) {
    return !StringUtils.isBlank(x);
  }

  @VisibleForTesting
  static String expandAbbreviatedGenus(String scientificName, String genus) {
    if (exists(scientificName) && exists(genus)) {
      String[] parts = scientificName.split(" +", 2);
      if (parts[0].length() <= 2) {
        String genusCorrect = StringUtils.capitalize(genus.trim().toLowerCase());
        StringBuilder sb = new StringBuilder();
        // is the genus missing alltogether?
        if (parts[0].equals("?")) {
          sb.append(genusCorrect);
        } else if (genusCorrect.length() > 1) {
          // test if name has an abbreviated genus
          if (parts[0].length() == 2 && parts[0].charAt(1) == '.' && parts[0].charAt(0) == genusCorrect.charAt(0)
            || parts[0].length() == 1 && parts[0].charAt(0) == genusCorrect.charAt(0)
          ) {
            sb.append(genusCorrect);
          }
        } else {
          sb.append(parts[0]);
        }
        if (parts.length>1) {
          sb.append(" ");
          sb.append(parts[1]);
        }
        return sb.toString();
      }
    }
    return scientificName;
  }

  @VisibleForTesting
  static String appendAuthorship(String scientificName, String authorship){
    if (!StringUtils.isBlank(scientificName)
        && !StringUtils.isBlank(authorship)
        && !scientificName.toLowerCase().contains(authorship.trim().toLowerCase())) {
      return scientificName.trim() + " " + authorship.trim();
    }
    return StringUtils.trimToNull(scientificName);
  }

  private static Rank lowestRank(LinneanClassification cl){
    for (Rank r : REVERSED_DWC_RANKS) {
      if (exists(cl.getHigherRank(r))) {
        return r;
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NameNRank)) return false;
    NameNRank nameNRank = (NameNRank) o;
    return Objects.equals(name, nameNRank.name) && rank == nameNRank.rank;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, rank);
  }

  @Override
  public String toString() {
    return name + " [" + rank + "]";
  }
}
