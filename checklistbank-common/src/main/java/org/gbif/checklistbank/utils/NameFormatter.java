package org.gbif.checklistbank.utils;

import com.google.common.collect.ImmutableMap;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.Rank;

import java.util.Map;

/**
 * Utilities to format a parsed name
 */
public class NameFormatter {
  private NameFormatter() {
  }

  private static NomenclaturalCode DEFAULT = NomenclaturalCode.BOTANICAL;
  private static Map<Kingdom, NomenclaturalCode> codes = ImmutableMap.<Kingdom, NomenclaturalCode>builder()
      .put(Kingdom.ANIMALIA, NomenclaturalCode.ZOOLOGICAL)
      .put(Kingdom.ARCHAEA, NomenclaturalCode.BACTERIAL)
      .put(Kingdom.BACTERIA, NomenclaturalCode.BACTERIAL)
      .put(Kingdom.CHROMISTA, NomenclaturalCode.BOTANICAL)
      .put(Kingdom.FUNGI, NomenclaturalCode.BOTANICAL)
      .put(Kingdom.PLANTAE, NomenclaturalCode.BOTANICAL)
      .put(Kingdom.PROTOZOA, NomenclaturalCode.ZOOLOGICAL)
      .put(Kingdom.VIRUSES, NomenclaturalCode.VIRUS)
      .put(Kingdom.INCERTAE_SEDIS, DEFAULT)
      .build();

  /**
   * @param pn
   * @param kingdom kingdom as a substitute for the nomenclatural code
   * @return
   */
  public static String scientificName(ParsedName pn, Kingdom kingdom) {
    if (kingdom == null) {
      return scientificName(pn, DEFAULT);
    }
    return scientificName(pn, codes.getOrDefault(kingdom, DEFAULT));
  }

  /**
   * @param pn
   * @param code to which rules to adhere to
   * @return
   */
  public static String scientificName(ParsedName pn, NomenclaturalCode code) {
    if (!pn.isParsed() || NomenclaturalCode.VIRUS == code) {
      return pn.getScientificName();
    }

    // remove authorship if indet name
    if (pn.isIndetermined()) {
      return pn.buildName(true, true, false, false, true, false, true, false, true, false, false, false, true, true);
    }

    String sciname;
    if (code == NomenclaturalCode.ZOOLOGICAL && pn.getInfraSpecificEpithet() != null && Rank.SUBSPECIES == pn.getRank()) {
      Rank r = pn.getRank();
      pn.setRank(null);
      sciname = pn.buildName(true, false, true, false, true, false, true, false, true, false, false, false, true, true);
      pn.setRank(r);

    } else {
      sciname = pn.canonicalNameComplete();
    }

    return sciname;
  }

}
