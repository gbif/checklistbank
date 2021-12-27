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
package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;

import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

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
    mapper.addMixIn(Date.class, IgnoreMixin.class);
    mapper.addMixIn(Integer.class, IgnoreMixin.class);
    // object readers & writers are slightly more performant than simple object mappers
    // they also are thread safe!
    reader = mapper.readerFor(VerbatimNameUsage.class);
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
