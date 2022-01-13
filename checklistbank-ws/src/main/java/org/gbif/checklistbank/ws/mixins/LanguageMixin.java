package org.gbif.checklistbank.ws.mixins;

import org.gbif.api.vocabulary.Language;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;

@JsonSerialize(
    using = LanguageMixin.IsoSerializer.class,
    keyUsing = LanguageMixin.IsoKeySerializer.class)
@JsonDeserialize(
    using = LanguageMixin.LenientDeserializer.class,
    keyUsing = LanguageMixin.LenientKeyDeserializer.class)
public interface LanguageMixin {

  class LenientKeyDeserializer extends KeyDeserializer {
    public LenientKeyDeserializer() {}

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

  class LenientDeserializer extends JsonDeserializer<Language> {
    public LenientDeserializer() {}

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

  class IsoSerializer extends JsonSerializer<Language> {
    public IsoSerializer() {}

    public void serialize(Language value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
      jgen.writeString(value.name());
    }
  }

  class IsoKeySerializer extends JsonSerializer<Language> {

    @Override
    public void serialize(
        Language language, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
        throws IOException {
      jsonGenerator.writeFieldName(language.name());
    }
  }
}
