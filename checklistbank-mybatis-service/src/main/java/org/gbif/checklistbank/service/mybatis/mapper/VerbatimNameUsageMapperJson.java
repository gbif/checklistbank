package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;

import java.io.IOException;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnoreType;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializing/Deserializing tool specifically for the term maps of a VerbatimNameUsage to be stored in postgres
 * or neo backends as a single binary column.
 */
public class VerbatimNameUsageMapperJson {

  private static final Logger LOG = LoggerFactory.getLogger(VerbatimNameUsageMapperJson.class);
  private final ObjectReader reader;
  private final ObjectWriter writer;

  @JsonIgnoreType
  public static class IgnoreMixin {}

  public VerbatimNameUsageMapperJson() {
    ObjectMapper mapper = new ObjectMapper();
    // ignore properties of certain types in VerbatimNameUsage that are stored as individual columns in the backends.
    mapper.getSerializationConfig().addMixInAnnotations(Date.class, IgnoreMixin.class);
    mapper.getSerializationConfig().addMixInAnnotations(Integer.class, IgnoreMixin.class);
    // object readers & writers are slightly more performant than simple object mappers
    // they also are thread safe!
    reader = mapper.reader(VerbatimNameUsage.class);
    writer = mapper.writerWithView(VerbatimNameUsage.class);
  }

  public VerbatimNameUsage read(String json) {
    if (json != null) {
      try {
        return reader.readValue(json);
      } catch (IOException e) {
        LOG.error("Cannot deserialize raw json data", e);
      }
    }
    return null;
  }

  public String write(VerbatimNameUsage verbatim) {
    if (verbatim != null) {
      try {
        return writer.writeValueAsString(verbatim);

      } catch (IOException e) {
        LOG.error("Cannot serialize raw json data", e);
      }
    }
    return null;
  }

}
