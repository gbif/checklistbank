package org.gbif.checklistbank.utils;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;

import java.io.IOException;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnoreType;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.smile.SmileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializing/Deserializing tool specifically for the term maps of a VerbatimNameUsage to be stored in postgres or neo backends
 * as a single binary column.
 */
public class VerbatimNameUsageMapper {

  private static final Logger LOG = LoggerFactory.getLogger(VerbatimNameUsageMapper.class);
  private final ObjectReader reader;
  private final ObjectWriter writer;

  @JsonIgnoreType
  public static class IgnoreMixin {}

  public VerbatimNameUsageMapper() {
    SmileFactory f = new SmileFactory();
    ObjectMapper mapper = new ObjectMapper(f);
    // ignore properties of certain types in VerbatimNameUsage that are stored as individual columns in the backends.
    mapper.getSerializationConfig().addMixInAnnotations(Date.class, IgnoreMixin.class);
    mapper.getSerializationConfig().addMixInAnnotations(Integer.class, IgnoreMixin.class);

    reader = mapper.reader(VerbatimNameUsage.class);
    writer = mapper.writerWithView(VerbatimNameUsage.class);
  }

  public VerbatimNameUsage read(byte[] smile) {
    if (smile != null) {
      try {
        return reader.readValue(smile);
      } catch (IOException e) {
        LOG.error("Cannot deserialize raw json data", e);
      }
    }
    return null;
  }

  public byte[] write(VerbatimNameUsage verbatim) {
    if (verbatim != null) {
      try {
        return writer.writeValueAsBytes(verbatim);

      } catch (IOException e) {
        LOG.error("Cannot deserialize raw json data", e);
      }
    }
    return null;
  }

}
