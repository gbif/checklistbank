/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.utils;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.Rank;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * Utilities to format a parsed name
 */
public class NameFormatter {
  private static final Logger LOG = LoggerFactory.getLogger(NameFormatter.class);

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

  public static String canonicalOrScientificName(ParsedName pn) {
    if (pn.isParsed()) {
      String name = pn.canonicalName();
      if (!StringUtils.isBlank(name)) {
        return name;
      }
      LOG.error("Parsed {} name found with an empty canonical name string: {}", pn.getType(), pn.getScientificName());
    }
    return pn.getScientificName();
  }
}
