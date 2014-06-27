package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.Extension;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerbatimNameUsageJsonParser {

  private static final Logger LOG = LoggerFactory.getLogger(VerbatimNameUsageJsonParser.class);
  private TermFactory termFactory = TermFactory.instance();
  private ObjectMapper jsonMapper = new ObjectMapper();
  private final TypeReference<HashMap<String, Object>> jsonMapTypeRef = new TypeReference<HashMap<String, Object>>() {
  };

  /**
   * Parses a json object for the complete, verbatim star record as stored in clb.
   * Example input:
   * {"id":"100",
   * "taxonomicStatus":"valid",
   * "taxonRank":"Species",
   * "scientificNameAuthorship":null,
   * "parentNameUsageID":"86",
   * "acceptedNameUsageID":null,
   * "scientificName":"Spirillum beijerinckii",
   * "extensions": {
   * "VernacularName" : [{"vernacularName":"", "language":"en", ...}, {...}],
   * "Distribution" : [{...}, {...}}],
   * }
   *
   * @param json the raw data serialized in json as given above
   *
   * @return the VerbatimNameUsage or null
   */
  public VerbatimNameUsage jsonToVerbatim(String json) {
    if (Strings.isNullOrEmpty(json)) {
      return null;
    }
    try {
      Map<String, Object> data = jsonMapper.readValue(json, jsonMapTypeRef);
      VerbatimNameUsage verbatim = new VerbatimNameUsage();
      for (String key : data.keySet()) {
        if (!key.equals("extensions")) {
          if (data.get(key) != null) {
            try {
              Term  t = termFactory.findTerm(key);
              verbatim.getFields().put(t, data.get(key).toString());
            } catch (IllegalArgumentException e) {
              LOG.warn("Illegal verbatim term {}", key);
            }
          }
        } else {
          Map<String, List<Map<String, String>>> extData = (Map) data.get(key);
          for (String rowType : extData.keySet()) {
            Extension extension = Extension.fromRowType(rowType);
            if (extension != null) {
              // add extension
              List<Map<Term, String>> records = Lists.newArrayList();
              verbatim.getExtensions().put(extension, records);
              for (Map<String, String> recRaw : extData.get(rowType)) {
                Map<Term, String> rec = Maps.newHashMap();
                records.add(rec);
                for (Map.Entry<String, String> prop : recRaw.entrySet()) {
                  try {
                    Term t = termFactory.findTerm(prop.getKey());
                    rec.put(t, prop.getValue());
                  } catch (IllegalArgumentException e) {
                    LOG.warn("Illegal verbatim term {}", key);
                  }
                }
              }
            }
          }
        }
      }
      return verbatim;

    } catch (IOException e) {
      LOG.error("Cannot deserialize raw json data", e);
    }
    return null;
  }

}
