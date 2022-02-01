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
package org.gbif.checklistbank.ws.mixins;

import org.gbif.api.vocabulary.Language;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;

public class LanguageSerializers {

  private LanguageSerializers() {}

  public static class LenientKeyDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
      try {
        return StringUtils.isNotEmpty(key)
            ? LenientDeserializer.lenientParse(key)
            : Language.UNKNOWN;
      } catch (Exception var4) {
        throw new IOException(
            "Unable to deserialize language from provided value (hint: not an ISO 2 or 3 character?): "
                + key);
      }
    }
  }

  public static class LenientDeserializer extends JsonDeserializer<Language> {

    @Override
    public Language deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      try {
        return jp != null && jp.getTextLength() > 0 ? lenientParse(jp.getText()) : Language.UNKNOWN;
      } catch (Exception var4) {
        throw new IOException(
            "Unable to deserialize language from provided value (hint: not an ISO 2 or 3 character?): "
                + jp.getText());
      }
    }

    static Language lenientParse(String value) {
      try {
        return Language.valueOf(value);
      } catch (IllegalArgumentException var3) {
        return Language.UNKNOWN;
      }
    }
  }

  public static class NameSerializer extends JsonSerializer<Language> {

    @Override
    public void serialize(Language value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
      jgen.writeString(value.name());
    }
  }

  public static class NameKeySerializer extends JsonSerializer<Language> {

    @Override
    public void serialize(
        Language language, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
        throws IOException {
      jsonGenerator.writeFieldName(language.name());
    }
  }

  public static class NameTocKeySerializer extends JsonSerializer {

    @Override
    public void serialize(
        Object value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
        throws IOException {

      if (value instanceof Language) {
        jsonGenerator.writeFieldName(((Language) value).name());
      } else if (value instanceof String) {
        jsonGenerator.writeFieldName((String) value);
      }
    }
  }
}
